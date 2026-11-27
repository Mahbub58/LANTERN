package com.mahbub.lantern.data.ai

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.DownloadStatus
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.InferenceMode
import com.google.firebase.ai.InferenceSource
import com.google.firebase.ai.OnDeviceConfig
import com.google.firebase.ai.OnDeviceExtension
import com.google.firebase.ai.OnDeviceModelOption
import com.google.firebase.ai.OnDeviceModelStatus
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.PublicPreviewAPI
import com.mahbub.lantern.domain.ai.NanoStatus
import com.mahbub.lantern.domain.ai.OnDeviceInference
import com.mahbub.lantern.domain.model.AppLanguage
import com.mahbub.lantern.domain.model.SafetyCard
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * On-device half of Firebase AI Logic.
 *
 * The contest requires Gemini Nano 4, so the model is requested with
 * [OnDeviceModelOption.PREVIEW] (the STABLE category may resolve to Nano 3/2).
 * The inference mode is [InferenceMode.ONLY_ON_DEVICE] on purpose: this class
 * never silently calls the cloud. [com.mahbub.lantern.domain.ai.AiRouter]
 * owns the escalation decision and only calls [com.mahbub.lantern.domain.ai.CloudInference]
 * when the device is online and cloud fallback is enabled.
 *
 * On-device inference is single-turn text only. No chat history is kept.
 */
@OptIn(PublicPreviewAPI::class)
@Singleton
class FirebaseOnDeviceInference @Inject constructor() : OnDeviceInference {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val statusFlow = MutableStateFlow(NanoStatus.UNAVAILABLE)
    private val downloadStarted = AtomicBoolean(false)
    private var downloadJob: Job? = null
    private var warmedUp = false

    private val model: GenerativeModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel(
                // A cloud model name is required by the builder even in
                // ONLY_ON_DEVICE mode; it is never used by this instance.
                modelName = FirebaseAiConfig.CLOUD_MODEL_NAME,
                onDeviceConfig = OnDeviceConfig(
                    mode = InferenceMode.ONLY_ON_DEVICE,
                    maxOutputTokens = 320,
                    temperature = 0.1f,
                    candidateCount = 1,
                    modelOption = OnDeviceModelOption.PREVIEW,
                ),
            )
    }

    override val status: Flow<NanoStatus> = statusFlow.asStateFlow()

    init {
        scope.launch { refreshStatus() }
    }

    override suspend fun isAvailable(): Boolean {
        val current = checkStatus()
        Log.i(TAG, "isAvailable=$current extension=${runCatching { model.onDeviceExtension != null }.getOrDefault(false)}")
        return current == NanoStatus.READY
    }

    override suspend fun answer(
        question: String,
        card: SafetyCard,
        language: AppLanguage,
    ): String {
        if (checkStatus() != NanoStatus.READY) {
            throw OnDeviceInferenceException("Gemini Nano is not available on this device")
        }
        val prompt = AiPrompts.onDevice(question, card, language)
        var lastError: Throwable? = null

        // Structured output keeps the cardId/answer contract reliable. One
        // retry, then the router falls back to cloud or the vetted card text.
        for (attempt in 0 until MAX_ATTEMPTS) {
            try {
                val response = withTimeout(ON_DEVICE_TIMEOUT_MS) {
                    model.generateObject(
                        GeneratedAnswer.firebaseAISchema(),
                        if (attempt == 0) prompt else "$prompt\nReturn the safety card id and the answer.",
                    )
                }
                val source = response.response.inferenceSource
                Log.i(TAG, "inferenceSource=$source model=${FirebaseAiConfig.CLOUD_MODEL_NAME}")
                if (source != InferenceSource.ON_DEVICE) {
                    Log.w(TAG, "ONLY_ON_DEVICE returned $source; rejecting the response")
                    continue
                }
                val generated = response.getObject()
                if (generated != null && generated.cardId == card.id) {
                    return ModelJson.encodeAnswer(generated.cardId, generated.answer)
                }
                lastError = IllegalStateException("On-device structured answer was empty or used the wrong card")
                Log.w(TAG, "On-device answer rejected: ${generated?.cardId ?: "null"} vs ${card.id}")
            } catch (t: TimeoutCancellationException) {
                lastError = t
                Log.w(TAG, "On-device request timed out after ${ON_DEVICE_TIMEOUT_MS} ms")
                break
            } catch (t: Throwable) {
                lastError = t
                Log.w(TAG, "On-device attempt ${attempt + 1} failed", t)
            }
        }
        throw OnDeviceInferenceException("On-device inference failed", lastError)
    }

    private suspend fun refreshStatus() {
        statusFlow.value = checkStatus()
    }

    private suspend fun checkStatus(): NanoStatus {
        val extension = runCatching { model.onDeviceExtension }.getOrNull()
        if (extension == null) {
            Log.w(TAG, "onDeviceExtension is null; is firebase-ai-ondevice on the classpath?")
            return NanoStatus.UNAVAILABLE
        }
        return try {
            val raw = extension.checkStatus()
            Log.i(TAG, "OnDeviceModelStatus=$raw")
            when (raw) {
                OnDeviceModelStatus.AVAILABLE -> {
                    warmUp(extension)
                    NanoStatus.READY
                }
                OnDeviceModelStatus.DOWNLOADABLE -> {
                    startDownload(extension)
                    NanoStatus.NOT_DOWNLOADED
                }
                OnDeviceModelStatus.DOWNLOADING -> NanoStatus.DOWNLOADING
                else -> NanoStatus.UNAVAILABLE
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Could not read Gemini Nano status", t)
            NanoStatus.UNAVAILABLE
        }
    }

    private fun warmUp(extension: OnDeviceExtension) {
        if (warmedUp) return
        warmedUp = true
        scope.launch { runCatching { extension.warmUp() } }
    }

    private fun startDownload(extension: OnDeviceExtension) {
        if (!downloadStarted.compareAndSet(false, true)) return
        downloadJob = scope.launch {
            statusFlow.value = NanoStatus.DOWNLOADING
            try {
                extension.download().collect { update ->
                    when (update) {
                        is DownloadStatus.DownloadStarted -> {
                            statusFlow.value = NanoStatus.DOWNLOADING
                            Log.i(TAG, "Gemini Nano download started: ${update.bytesToDownload} bytes")
                        }
                        is DownloadStatus.DownloadInProgress -> {
                            statusFlow.value = NanoStatus.DOWNLOADING
                            Log.i(TAG, "Gemini Nano download: ${update.totalBytesDownloaded} bytes")
                        }
                        is DownloadStatus.DownloadCompleted -> {
                            statusFlow.value = NanoStatus.READY
                            Log.i(TAG, "Gemini Nano download complete")
                        }
                        is DownloadStatus.DownloadFailed -> {
                            statusFlow.value = NanoStatus.NOT_DOWNLOADED
                            downloadStarted.set(false)
                            Log.w(TAG, "Gemini Nano download failed: $update")
                        }
                    }
                }
            } catch (t: Throwable) {
                downloadStarted.set(false)
                statusFlow.value = NanoStatus.NOT_DOWNLOADED
                Log.w(TAG, "Gemini Nano download failed", t)
            }
        }
    }

    private companion object {
        const val TAG = "LanternOnDevice"
        const val MAX_ATTEMPTS = 2
        const val ON_DEVICE_TIMEOUT_MS = 20_000L
    }
}

class OnDeviceInferenceException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
