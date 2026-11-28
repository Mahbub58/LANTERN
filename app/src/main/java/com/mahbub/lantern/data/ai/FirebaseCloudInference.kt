package com.mahbub.lantern.data.ai

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.InferenceMode
import com.google.firebase.ai.InferenceSource
import com.google.firebase.ai.OnDeviceConfig
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.ai.type.QuotaExceededException
import com.google.firebase.ai.type.content
import com.mahbub.lantern.domain.ai.CloudInference
import com.mahbub.lantern.domain.ai.GroundedAnswerValidator
import com.mahbub.lantern.domain.ai.TriageResult
import com.mahbub.lantern.domain.model.AppLanguage
import com.mahbub.lantern.domain.model.SafetyCard
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cloud half of Firebase AI Logic. Always [InferenceMode.ONLY_IN_CLOUD].
 *
 * Callers must scrub PII first: [com.mahbub.lantern.domain.ai.AiRouter] does
 * that before every call. The prompt contains the approved card catalog and the
 * scrubbed question only, never coordinates or device identifiers.
 *
 * The response goes through the same grounded-answer validator as the on-device
 * response. A cloud answer that drifts from the card is rejected so the router
 * can show the vetted card text instead.
 */
@OptIn(PublicPreviewAPI::class)
@Singleton
class FirebaseCloudInference @Inject constructor(
    private val answerValidator: GroundedAnswerValidator,
) : CloudInference {

    /**
     * One cloud call that both matches the question and writes the grounded
     * answer. The full safety-card catalog is the system instruction, so the
     * app only sends the scrubbed question as user content.
     *
     * Returns [TriageResult.NoMatch] when the model says no card fits; throws
     * only when the request or schema itself fails.
     */
    override suspend fun matchAndAnswer(
        question: String,
        cards: List<SafetyCard>,
        language: AppLanguage,
    ): TriageResult {
        if (cards.isEmpty()) return TriageResult.NoMatch
        val systemPrompt = AiPrompts.triageSystem(cards, language)
        val userPrompt = AiPrompts.triageQuestion(question)
        Log.i(TAG, "cloud triage cards=${cards.size} systemPromptChars=${systemPrompt.length}")
        var lastError: Throwable? = null

        for (attempt in 0 until MAX_ATTEMPTS) {
            try {
                val systemModel = Firebase.ai(backend = GenerativeBackend.googleAI())
                    .generativeModel(
                        modelName = FirebaseAiConfig.CLOUD_MODEL_NAME,
                        systemInstruction = content { text(systemPrompt) },
                        onDeviceConfig = OnDeviceConfig(
                            mode = InferenceMode.ONLY_IN_CLOUD,
                            maxOutputTokens = 320,
                            temperature = 0.1f,
                            candidateCount = 1,
                        ),
                    )
                val response = withTimeout(CLOUD_TIMEOUT_MS) {
                    systemModel.generateObject(
                        GeneratedTriageResult.firebaseAISchema(),
                        if (attempt == 0) {
                            userPrompt
                        } else {
                            "$userPrompt\nReturn the exact {matched,cardId,answer} JSON object."
                        },
                    )
                }
                Log.i(
                    TAG,
                    "cloud triage inferenceSource=${response.response.inferenceSource} model=${FirebaseAiConfig.CLOUD_MODEL_NAME}",
                )
                if (response.response.inferenceSource != InferenceSource.IN_CLOUD) {
                    Log.w(TAG, "ONLY_IN_CLOUD returned ${response.response.inferenceSource}")
                    continue
                }

                val generated = response.getObject()
                if (generated == null) {
                    lastError = IllegalStateException("Cloud triage was empty")
                    continue
                }
                Log.i(
                    TAG,
                    "cloud triage matched=${generated.matched} cardId=${generated.cardId} answerChars=${generated.answer.length}",
                )
                val cardId = generated.cardId.trim()
                if (!generated.matched && (cardId.isEmpty() || cardId.equals("none", true))) {
                    return TriageResult.NoMatch
                }

                val card = cards.firstOrNull { it.id.equals(cardId, ignoreCase = true) }
                val answer = if (card != null) {
                    runCatching {
                        answerValidator.validate(
                            ModelJson.encodeAnswer(card.id, generated.answer),
                            card,
                            language,
                        )
                    }.getOrNull()?.takeIf { it.isNotBlank() }
                        ?: card.title.forLanguage(language)
                } else {
                    generated.answer.trim()
                }
                Log.i(TAG, "cloud triage resolved cardId=${card?.id ?: cardId}")
                return TriageResult(
                    matched = generated.matched,
                    cardId = cardId.takeUnless { it.isEmpty() || it.equals("none", true) },
                    answer = answer,
                )
            } catch (t: QuotaExceededException) {
                lastError = t
                Log.w(TAG, "Cloud triage quota exhausted; retrying will not help", t)
                break
            } catch (t: TimeoutCancellationException) {
                lastError = t
                Log.w(TAG, "Cloud triage timed out after $CLOUD_TIMEOUT_MS ms")
                break
            } catch (t: Throwable) {
                lastError = t
                Log.w(TAG, "Cloud triage attempt ${attempt + 1} failed", t)
            }
        }
        throw CloudInferenceException("Cloud triage failed", lastError)
    }

    private companion object {
        const val TAG = "LanternCloud"
        const val MAX_ATTEMPTS = 2
        const val CLOUD_TIMEOUT_MS = 15_000L
    }
}

class CloudInferenceException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
