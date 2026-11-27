package com.mahbub.lantern.domain.ai

import com.mahbub.lantern.domain.model.Answer
import com.mahbub.lantern.domain.model.AppLanguage
import com.mahbub.lantern.domain.model.InferenceSource
import com.mahbub.lantern.domain.model.OnDeviceIssue
import com.mahbub.lantern.domain.model.RoutingConfig
import com.mahbub.lantern.domain.model.SafetyCard
import javax.inject.Inject

/**
 * The routing policy. Pure Kotlin and fully unit tested.
 *
 * **Matching is Gemini Nano only.** There is no keyword or lexical fallback:
 * Nano reads the question in English, Urdu or Roman Urdu and returns the id of
 * the best safety card. If Nano is not ready the router returns a NoMatch with
 * [OnDeviceIssue.UNAVAILABLE]; the Ask screen checks Nano before navigating, so
 * the user never reaches this state through the UI.
 *
 * 1. Nano unavailable -> NoMatch + UNAVAILABLE. No generation, no cloud.
 * 2. Nano match fails -> NoMatch + FAILED.
 * 3. Nano finds no card -> NoMatch with no error (the question is genuinely
 *    outside the card pack; the UI shows emergency numbers).
 * 4. Card matched -> grounded on-device answer, strictly from the card JSON.
 * 5. On-device generation fails -> cloud when online and enabled (PII scrubbed),
 *    otherwise the matched card's vetted text. The answer carries
 *    [OnDeviceIssue.FAILED] so the UI can say Nano did not produce it.
 * 6. Every result carries the card and an [InferenceSource] for the UI badge.
 */
class AiRouter @Inject constructor(
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

        // Matching is done by Gemini Nano and nothing else.
        val onDeviceReady = runCatching { onDeviceInference.isAvailable() }.getOrDefault(false)
        if (!onDeviceReady) {
            return noMatch(0f, OnDeviceIssue.UNAVAILABLE)
        }

        val matchedId = runCatching { onDeviceInference.matchCard(question, cards) }
            .getOrElse { return noMatch(0f, OnDeviceIssue.FAILED) }
        val card = matchedId?.let { id -> cards.firstOrNull { it.id == id } }
            ?: return noMatch(0f)

        if (!forceCloud) {
            val raw = runCatching { onDeviceInference.answer(question, card, language) }.getOrNull()
            val validated = raw?.let { answerValidator.validate(it, card, language) }
            if (!validated.isNullOrBlank()) {
                return Answer(
                    card = card,
                    text = validated,
                    source = InferenceSource.ON_DEVICE,
                )
            }
        }

        // Generation failed after a successful match, or a deeper cloud answer
        // was requested. A deliberate deeper request is not a Nano error.
        val issue = if (forceCloud) null else OnDeviceIssue.FAILED

        if (isOnline && safeConfig.cloudFallbackEnabled) {
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
                    onDeviceIssue = issue,
                )
            }
        }

        return Answer(
            card = card,
            text = card.title.forLanguage(language),
            source = InferenceSource.CARD_TEXT,
            onDeviceIssue = issue,
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

    private fun noMatch(score: Float, issue: OnDeviceIssue? = null) = Answer(
        card = null,
        text = "",
        source = InferenceSource.CARD_TEXT,
        isNoMatch = true,
        retrievalScore = score,
        onDeviceIssue = issue,
    )

    private companion object {
        const val MAX_CLOUD_ANSWER_LENGTH = 1200
        val UNTRUSTED_LINK = Regex("https?://|www\\.", RegexOption.IGNORE_CASE)
    }
}
