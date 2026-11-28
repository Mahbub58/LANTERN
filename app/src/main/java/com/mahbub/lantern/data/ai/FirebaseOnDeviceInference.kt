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
import com.mahbub.lantern.domain.ai.TriageResult
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
 * Matching and answering happen in the same call, with the same system prompt
 * and structured result shape as [FirebaseCloudInference].
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
        // Keep Settings/Debug in sync when availability changes outside init.
        statusFlow.value = current
        Log.i(TAG, "isAvailable=$current extension=${runCatching { model.onDeviceExtension != null }.getOrDefault(false)}")
        return current == NanoStatus.READY
    }

    /**
     * Matches the question and writes the grounded answer in one Nano call,
     * using the same prompt, card data and structured
     * `{matched, cardId, answer}` contract as the cloud model.
     *
     * Returns [TriageResult.NoMatch] only when Nano ran and chose no card. Any
     * load or inference problem throws, so the router can fall back to cloud.
     */
    override suspend fun matchAndAnswer(
        question: String,
        cards: List<SafetyCard>,
        language: AppLanguage,
    ): TriageResult {
        if (cards.isEmpty()) return TriageResult.NoMatch
        if (checkStatus() != NanoStatus.READY) {
            throw OnDeviceInferenceException("Gemini Nano is not available on this device")
        }

        // Put the whole prompt in the user content. Some Nano/AICore versions
        // ignore system instructions, and the card catalog must be visible to
        // the model. The compact card projection keeps this under the
        // 4,000-token on-device request limit.
        val prompt = buildString {
            appendLine(AiPrompts.triageSystem(cards, language))
            appendLine()
            append(AiPrompts.triageQuestion(question))
        }
        Log.i(TAG, "on-device triage cards=${cards.size} promptChars=${prompt.length}")
        if (prompt.length > MAX_PROMPT_CHARS) {
            // AICore rejects the whole request above 4,000 tokens. Rather than
            // let that become a mysterious failure, hand the question to the
            // router's cloud fallback with a clear reason.
            throw OnDeviceInferenceException(
                "On-device prompt is too long (${prompt.length} chars); using cloud fallback",
            )
        }
        var lastError: Throwable? = null

        for (attempt in 0 until MAX_ATTEMPTS) {
            try {
                // Reuse the warmed-up singleton model. Creating a new model per
                // request bypasses the extension warm-up done by checkStatus().
                val response = withTimeout(ON_DEVICE_TIMEOUT_MS) {
                    model.generateObject(
                        GeneratedTriageResult.firebaseAISchema(),
                        if (attempt == 0) {
                            prompt
                        } else {
                            "$prompt\nReturn the exact {matched,cardId,answer} JSON object."
                        },
                    )
                }
                val source = response.response.inferenceSource
                Log.i(TAG, "triage inferenceSource=$source")
                if (source != InferenceSource.ON_DEVICE) {
                    Log.w(TAG, "ONLY_ON_DEVICE returned $source; rejecting the response")
                    continue
                }

                val generated = response.getObject()
                if (generated == null) {
                    lastError = IllegalStateException("On-device triage was empty")
                    continue
                }
                Log.i(
                    TAG,
                    "on-device triage matched=${generated.matched} cardId=${generated.cardId} answerChars=${generated.answer.length}",
                )
                val cardId = generated.cardId.trim()
                if (!generated.matched && (cardId.isEmpty() || cardId.equals("none", true))) {
                    return TriageResult.NoMatch
                }

                val card = cards.firstOrNull { it.id.equals(cardId, ignoreCase = true) }
                val answer = generated.answer.trim()
                    .ifBlank { card?.title?.forLanguage(language).orEmpty() }
                Log.i(TAG, "on-device triage resolved cardId=${card?.id ?: cardId}")
                return TriageResult(
                    matched = generated.matched,
                    cardId = cardId.takeUnless { it.isEmpty() || it.equals("none", true) },
                    answer = answer,
                )
            } catch (t: TimeoutCancellationException) {
                lastError = t
                Log.w(TAG, "On-device triage timed out after $ON_DEVICE_TIMEOUT_MS ms")
                break
            } catch (t: Throwable) {
                lastError = t
                Log.w(TAG, "On-device triage attempt ${attempt + 1} failed", t)
            }
        }
        throw OnDeviceInferenceException("On-device triage failed", lastError)
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

        /**
         * Conservative character guard for AICore's 4,000-token request limit.
         * The current six-card pack is about 8,000 characters (roughly
         * 2,100-2,800 tokens depending on the script).
         */
        const val MAX_PROMPT_CHARS = 10_000
    }
}

class OnDeviceInferenceException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
