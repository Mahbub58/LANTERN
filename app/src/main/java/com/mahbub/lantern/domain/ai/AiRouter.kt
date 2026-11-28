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
 * **Both engines use the same triage logic.** Gemini Nano and the cloud model
 * receive the same system instruction, the same approved card pack and the
 * person's question, and both return `{matched, cardId, answer}`. The router
 * only decides which engine runs and how to present a failure.
 *
 * 1. Gemini Nano first (unless the person explicitly asks for a deeper cloud
 *    answer): one call matches the question and writes the grounded answer.
 * 2. If Nano is unavailable, throws, or returns `matched:false`, the same
 *    PII-scrubbed question goes to cloud AI Logic when online and enabled.
 * 3. A cloud `matched:false` becomes a NoMatch; the answer page shows the
 *    localized "we did not match your question" message.
 * 4. A match from either engine renders the existing safety-card result page.
 * 5. Every result carries the card and an [InferenceSource] for the UI badge.
 */
class AiRouter @Inject constructor(
    private val onDeviceInference: OnDeviceInference,
    private val cloudInference: CloudInference,
    private val piiScrubber: PiiScrubber,
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
        if (cards.isEmpty()) {
            throw IllegalStateException("No safety cards are loaded; cannot triage the question")
        }

        val canUseCloud = isOnline && safeConfig.cloudFallbackEnabled
        val onDeviceReady = runCatching { onDeviceInference.isAvailable() }.getOrDefault(false)
        var onDeviceIssue: OnDeviceIssue? = when {
            forceCloud -> null
            onDeviceReady -> null
            else -> OnDeviceIssue.UNAVAILABLE
        }

        // 1. On-device triage: same prompt, card JSON and result schema as cloud.
        if (!forceCloud && onDeviceReady) {
            val result = runCatching {
                onDeviceInference.matchAndAnswer(question, cards, language)
            }.getOrElse {
                onDeviceIssue = OnDeviceIssue.FAILED
                null
            }

            if (result != null) {
                // Trust a resolvable card id even if the model set matched=false
                // by mistake; the id is the stronger signal.
                val card = resolveCard(result.cardId, cards)
                if (card != null) {
                    return Answer(
                        card = card,
                        text = answerText(result.answer, card, language),
                        source = InferenceSource.ON_DEVICE,
                    )
                }
                if (result.matched) {
                    // A matched result with an unknown id is a model error.
                    onDeviceIssue = OnDeviceIssue.FAILED
                }
                // result.matched == false is a valid answer: try the cloud next.
            }
        }

        // 2. Cloud fallback, using exactly the same triage contract.
        var cloudFailure: Throwable? = null
        if (canUseCloud) {
            val scrubbed = piiScrubber.scrub(question)
            val result = try {
                cloudInference.matchAndAnswer(scrubbed, cards, language)
            } catch (t: Throwable) {
                cloudFailure = t
                null
            }

            if (result != null) {
                val card = resolveCard(result.cardId, cards)
                if (card != null) {
                    return Answer(
                        card = card,
                        text = answerText(result.answer, card, language),
                        source = InferenceSource.CLOUD,
                        isDeeper = forceCloud,
                        onDeviceIssue = onDeviceIssue,
                    )
                }
                if (result.matched) {
                    onDeviceIssue = OnDeviceIssue.FAILED
                }
                // The cloud ran and explicitly said no card matched. Show only
                // the localized ask-again message, not a Nano banner.
                return noMatch(0f)
            }
        }

        // 3. A deeper cloud answer was requested but the cloud failed. Nano is
        // still a safe last resort so the person does not lose the answer.
        if (forceCloud && onDeviceReady) {
            val result = runCatching {
                onDeviceInference.matchAndAnswer(question, cards, language)
            }.getOrNull()
            val card = result?.let { resolveCard(it.cardId, cards) }
            if (card != null) {
                return Answer(
                    card = card,
                    text = answerText(result.answer, card, language),
                    source = InferenceSource.ON_DEVICE,
                )
            }
        }

        // A model request failed; do not disguise that as "your question did
        // not match". The answer screen shows the retry state instead.
        if (cloudFailure != null) {
            throw IllegalStateException("AI triage failed on every engine", cloudFailure)
        }

        return noMatch(0f, onDeviceIssue)
    }

    /**
     * Models occasionally return a card title instead of the id, or set
     * `matched:false` while still naming a real card. Normalising hyphens,
     * underscores and case keeps that from becoming a false "no match".
     */
    private fun resolveCard(rawId: String?, cards: List<SafetyCard>): SafetyCard? {
        val value = rawId?.trim().orEmpty()
        if (value.isEmpty() || value.equals("none", ignoreCase = true)) return null

        cards.firstOrNull { it.id.equals(value, ignoreCase = true) }?.let { return it }

        val normalized = normalizeCardKey(value)
        if (normalized.isEmpty()) return null
        return cards.firstOrNull { card ->
            normalizeCardKey(card.id) == normalized ||
                normalizeCardKey(card.title.en) == normalized ||
                normalizeCardKey(card.title.ur) == normalized ||
                normalizeCardKey(card.title.roman) == normalized
        }
    }

    private fun normalizeCardKey(value: String): String = value
        .lowercase()
        .replace('-', ' ')
        .replace('_', ' ')
        .replace(Regex("\\s+"), " ")
        .trim()

    /**
     * A short, safe answer for [Answer.text]. The result page itself is driven
     * by the matched card, so a rejected or blank model answer falls back to the
     * vetted card title.
     */
    private fun answerText(
        raw: String,
        card: SafetyCard,
        language: AppLanguage,
    ): String {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return card.title.forLanguage(language)
        if (trimmed.length > MAX_ANSWER_LENGTH) return card.title.forLanguage(language)
        if (safetyGuard.isUnsafe(trimmed)) return card.title.forLanguage(language)
        if (UNTRUSTED_LINK.containsMatchIn(trimmed)) return card.title.forLanguage(language)
        return trimmed
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
        const val MAX_ANSWER_LENGTH = 1200
        val UNTRUSTED_LINK = Regex("https?://|www\\.", RegexOption.IGNORE_CASE)
    }
}
