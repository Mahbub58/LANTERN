package com.mahbub.lantern.domain.ai

import com.mahbub.lantern.domain.model.AppLanguage
import com.mahbub.lantern.domain.model.SafetyCard
import javax.inject.Inject
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class OnDeviceAnswerPayload(
    val cardId: String = "",
    val answer: String = "",
)

/**
 * Validates the strict JSON contract from the on-device model:
 *
 *   {"cardId": "...", "answer": "..."}
 *
 * An answer is rejected when the card id differs, the text is blank/too long,
 * it includes a dosage or diagnosis, or a sentence adds advice that has no
 * support in the retrieved card. Rejection is safe: the router falls back to
 * cloud (if online) or the card text.
 */
class GroundedAnswerValidator @Inject constructor(
    private val safetyGuard: SafetyGuard,
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun validate(raw: String, card: SafetyCard, language: AppLanguage): String? {
        val cleaned = stripCodeFence(raw).trim()
        val payload = runCatching { json.decodeFromString<OnDeviceAnswerPayload>(cleaned) }.getOrNull()
            ?: return null
        if (payload.cardId != card.id) return null

        val answer = payload.answer.trim()
        if (answer.isEmpty() || answer.length > MAX_ANSWER_LENGTH) return null
        if (safetyGuard.isUnsafe(answer)) return null
        if (URL.containsMatchIn(answer)) return null

        val withoutDisclaimer = stripLeadingDisclaimer(answer)
        if (withoutDisclaimer.isBlank()) return null
        if (!isGrounded(withoutDisclaimer, card)) return null

        return withoutDisclaimer
    }

    internal fun isGrounded(answer: String, card: SafetyCard): Boolean {
        val vocabulary = buildSet {
            addAll(TextNormalizer.significantTokens(card.groundingText(AppLanguage.EN)))
            addAll(TextNormalizer.significantTokens(card.groundingText(AppLanguage.UR)))
            addAll(TextNormalizer.significantTokens(card.groundingText(AppLanguage.ROMAN_UR)))
            card.allKeywords().forEach { addAll(TextNormalizer.significantTokens(it)) }
            addAll(DISCLAIMER_TOKENS)
        }
        if (vocabulary.isEmpty()) return false

        val sentences = answer.split(SENTENCE_BREAK).map { it.trim() }.filter { it.isNotBlank() }
        if (sentences.isEmpty()) return false

        return sentences.all { sentence ->
            val tokens = TextNormalizer.significantTokens(sentence)
            if (tokens.size < 2) return@all true
            val supported = tokens.count { it in vocabulary }
            supported.toFloat() / tokens.size >= MIN_GROUNDED_COVERAGE
        }
    }

    private fun stripLeadingDisclaimer(answer: String): String {
        val firstLine = answer.lineSequence().firstOrNull()?.trim().orEmpty()
        return if (DISCLAIMER.containsMatchIn(firstLine)) {
            answer.removePrefix(firstLine).trim()
        } else {
            answer
        }
    }

    private fun stripCodeFence(raw: String): String {
        val trimmed = raw.trim()
        if (!trimmed.startsWith("```")) return trimmed
        return trimmed
            .removePrefix("```json")
            .removePrefix("```JSON")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
    }

    private companion object {
        const val MAX_ANSWER_LENGTH = 700
        const val MIN_GROUNDED_COVERAGE = 0.6f
        val SENTENCE_BREAK = Regex("[.!?\\n۔]+")
        val URL = Regex("https?://|www\\.", RegexOption.IGNORE_CASE)
        val DISCLAIMER = Regex("(?i)call (your )?local emergency number")
        val DISCLAIMER_TOKENS = setOf(
            "call", "local", "emergency", "number", "first", "can", "if",
            "فون", "کریں", "ایمرجنسی", "نمبر",
        )
    }
}
