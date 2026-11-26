package com.softvinax.lantern.presentation.ask

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.core.content.ContextCompat
import com.softvinax.lantern.R
import com.softvinax.lantern.domain.model.AppLanguage

data class VoiceInputState(
    val isListening: Boolean,
    @StringRes val messageRes: Int?,
    val onMicClick: () -> Unit,
    val clearMessage: () -> Unit,
)

/**
 * Optional voice input for the Ask card. It uses the platform recognizer with
 * `EXTRA_PREFER_OFFLINE`, so a device with an offline speech model can dictate
 * without a connection. If the device has no recognizer or no offline model,
 * the UI explains the failure and the person can type instead.
 */
@Composable
fun rememberVoiceInput(
    language: AppLanguage,
    onResult: (String) -> Unit,
): VoiceInputState {
    if (LocalInspectionMode.current) {
        return VoiceInputState(
            isListening = false,
            messageRes = null,
            onMicClick = {},
            clearMessage = {},
        )
    }

    val context = LocalContext.current
    val latestOnResult by rememberUpdatedState(onResult)
    var isListening by remember { mutableStateOf(false) }
    var messageRes by remember { mutableStateOf<Int?>(null) }
    val recognitionAvailable = remember { SpeechRecognizer.isRecognitionAvailable(context) }
    val recognizer = remember {
        runCatching { SpeechRecognizer.createSpeechRecognizer(context) }.getOrNull()
    }
    val languageTag = when (language) {
        AppLanguage.EN -> "en-US"
        AppLanguage.UR -> "ur-PK"
        AppLanguage.ROMAN_UR -> "en-PK"
    }
    val intent = remember(languageTag) {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }
    }

    val startListening = {
        if (!recognitionAvailable || recognizer == null) {
            messageRes = R.string.voice_unavailable
        } else {
            isListening = true
            messageRes = null
            runCatching { recognizer.startListening(intent) }
                .onFailure {
                    isListening = false
                    messageRes = R.string.voice_error
                }
            Unit
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            startListening()
        } else {
            messageRes = R.string.voice_permission_denied
        }
    }

    DisposableEffect(recognizer) {
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
            }

            override fun onBeginningOfSpeech() {
                isListening = true
            }

            override fun onRmsChanged(rmsdB: Float) = Unit

            override fun onBufferReceived(buffer: ByteArray?) = Unit

            override fun onEndOfSpeech() {
                isListening = false
            }

            override fun onError(error: Int) {
                isListening = false
                messageRes = when (error) {
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                        R.string.voice_permission_denied
                    SpeechRecognizer.ERROR_NETWORK,
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
                    SpeechRecognizer.ERROR_SERVER,
                    -> R.string.voice_needs_connection
                    else -> R.string.voice_error
                }
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.trim()
                    .orEmpty()
                if (text.isNotEmpty()) {
                    latestOnResult(text)
                } else {
                    messageRes = R.string.voice_error
                }
            }

            override fun onPartialResults(partialResults: Bundle?) = Unit

            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })
        onDispose { runCatching { recognizer?.destroy() } }
    }

    val onMicClick = {
        if (isListening) {
            isListening = false
            runCatching { recognizer?.stopListening() }
            Unit
        } else if (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startListening()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    return VoiceInputState(
        isListening = isListening,
        messageRes = messageRes,
        onMicClick = onMicClick,
        clearMessage = { messageRes = null },
    )
}
