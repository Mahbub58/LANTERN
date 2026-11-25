package com.softvinax.lantern.domain.ai

import com.softvinax.lantern.domain.model.Answer
import com.softvinax.lantern.domain.model.AppLanguage
import com.softvinax.lantern.domain.model.InferenceSource
import com.softvinax.lantern.domain.model.RoutingConfig
import com.softvinax.lantern.domain.model.SafetyCard
import javax.inject.Inject

/**
 * The routing policy. Pure Kotlin and fully unit tested.
 *
 * 1. Retrieve the best card with a lexical scorer; its score is the confidence.
 * 2. Nothing above the minimum -> NoMatch (emergency numbers, no model call).
 * 3. Score >= threshold -> on-device, strictly grounded in the card JSON.
 * 4. Score below threshold, on-device unavailable/invalid -> cloud when online
 *    and enabled (PII scrubbed), otherwise the vetted card text.
 * 5. Every result carries the card and an [InferenceSource] for the UI badge.
 */
class AiRouter @Inject constructor(
    private val retriever: LexicalSafetyCardRetriever,
    private val onDeviceInference: OnDeviceInference,
    private val cloudInference: CloudInference,
    private val piiScrubber: PiiScrubber,
    private val answerValidator: GroundedAnswerValidator,
    private val safetyGuard: SafetyGuard,
) {

    suspend fun route(
        question: String,
        language: AppLanguage,
        config: RoutingConfig,
        isOnline: Boolean,
        cards: List<SafetyCard>,
        forceCloud: Boolean = false,
    ): Answer {
        val safeConfig = config.sanitized()
        if (question.isBlank()) return noMatch(0f)

        val scored = retriever.bestCard(question, cards)
        if (scored == null || scored.score < RoutingConfig.MINIMUM_MATCH_SCORE) {
            return noMatch(scored?.score ?: 0f)
        }

        val card = scored.card
        val canUseCloud = isOnline && safeConfig.cloudFallbackEnabled

        if (!forceCloud && scored.score >= safeConfig.confidenceThreshold) {
            val onDeviceAvailable = runCatching { onDeviceInference.isAvailable() }.getOrDefault(false)
            if (onDeviceAvailable) {
                val raw = runCatching { onDeviceInference.answer(question, card, language) }.getOrNull()
                val validated = raw?.let { answerValidator.validate(it, card, language) }
                if (!validated.isNullOrBlank()) {
                    return Answer(
                        card = card,
                        text = validated,
                        source = InferenceSource.ON_DEVICE,
                        retrievalScore = scored.score,
                    )
                }
            }
        }

        if (canUseCloud) {
            val scrubbed = piiScrubber.scrub(question)
            val cloudAnswer = runCatching {
                cloudInference.answer(scrubbed, card, language)
            }.getOrNull()
            if (isCloudAnswerAcceptable(cloudAnswer)) {
                return Answer(
                    card = card,
                    text = cloudAnswer!!.trim(),
                    source = InferenceSource.CLOUD,
                    isDeeper = forceCloud,
                    retrievalScore = scored.score,
                )
            }
        }

        return Answer(
            card = card,
            text = card.title.forLanguage(language),
            source = InferenceSource.CARD_TEXT,
            retrievalScore = scored.score,
        )
    }

    private fun isCloudAnswerAcceptable(answer: String?): Boolean {
        if (answer.isNullOrBlank()) return false
        val trimmed = answer.trim()
        if (trimmed.length > MAX_CLOUD_ANSWER_LENGTH) return false
        if (safetyGuard.isUnsafe(trimmed)) return false
        if (UNTRUSTED_LINK.containsMatchIn(trimmed)) return false
        return true
    }

    private fun noMatch(score: Float) = Answer(
        card = null,
        text = "",
        source = InferenceSource.CARD_TEXT,
        isNoMatch = true,
        retrievalScore = score,
    )

    private companion object {
        const val MAX_CLOUD_ANSWER_LENGTH = 1200
        val UNTRUSTED_LINK = Regex("https?://|www\\.", RegexOption.IGNORE_CASE)
    }
}
