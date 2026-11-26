package com.softvinax.lantern.data.ai

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.InferenceMode
import com.google.firebase.ai.InferenceSource
import com.google.firebase.ai.OnDeviceConfig
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.PublicPreviewAPI
import com.softvinax.lantern.domain.ai.CloudInference
import com.softvinax.lantern.domain.ai.GroundedAnswerValidator
import com.softvinax.lantern.domain.model.AppLanguage
import com.softvinax.lantern.domain.model.SafetyCard
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cloud half of Firebase AI Logic. Always [InferenceMode.ONLY_IN_CLOUD].
 *
 * Callers must scrub PII first: [com.softvinax.lantern.domain.ai.AiRouter] does
 * that before every call. The prompt contains the retrieved card and the
 * scrubbed question only, never coordinates or device identifiers.
 *
 * The response goes through the same grounded-answer validator as the
 * on-device response. A cloud answer that drifts from the card is rejected so
 * the router can show the vetted card text instead.
 */
@OptIn(PublicPreviewAPI::class)
@Singleton
class FirebaseCloudInference @Inject constructor(
    private val answerValidator: GroundedAnswerValidator,
) : CloudInference {

    private val model: GenerativeModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel(
                modelName = CLOUD_MODEL_NAME,
                onDeviceConfig = OnDeviceConfig(
                    mode = InferenceMode.ONLY_IN_CLOUD,
                    maxOutputTokens = 320,
                    temperature = 0.2f,
                    candidateCount = 1,
                ),
            )
    }

    override suspend fun answer(
        question: String,
        card: SafetyCard,
        language: AppLanguage,
    ): String {
        val prompt = AiPrompts.cloud(question, card, language)
        var lastError: Throwable? = null

        repeat(MAX_ATTEMPTS) { attempt ->
            try {
                val response = model.generateContent(
                    if (attempt == 0) prompt else "$prompt\nReturn ONLY the JSON object.",
                )
                Log.i(TAG, "inferenceSource=${response.inferenceSource} model=$CLOUD_MODEL_NAME")
                if (response.inferenceSource != InferenceSource.IN_CLOUD) {
                    Log.w(TAG, "ONLY_IN_CLOUD returned ${response.inferenceSource}")
                    return@repeat
                }
                val payload = ModelJson.answer(response.text)
                if (payload != null && payload.cardId == card.id) {
                    val validated = answerValidator.validate(
                        ModelJson.encodeAnswer(payload.cardId, payload.answer),
                        card,
                        language,
                    )
                    if (!validated.isNullOrBlank()) return validated
                }
                lastError = IllegalStateException("Cloud answer was not grounded in the card")
            } catch (t: Throwable) {
                lastError = t
                Log.w(TAG, "Cloud attempt ${attempt + 1} failed", t)
            }
        }
        throw CloudInferenceException("Cloud inference failed", lastError)
    }

    private companion object {
        const val TAG = "LanternCloud"
        const val CLOUD_MODEL_NAME = "gemini-3.8-flash"
        const val MAX_ATTEMPTS = 2
    }
}

class CloudInferenceException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
