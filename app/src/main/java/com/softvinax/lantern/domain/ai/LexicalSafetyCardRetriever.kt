package com.softvinax.lantern.domain.ai

import com.softvinax.lantern.domain.model.SafetyCard
import javax.inject.Inject
import kotlin.math.max

data class ScoredCard(
    val card: SafetyCard,
    val score: Float,
)

/**
 * Offline lexical retrieval across English, Urdu and Roman Urdu keywords.
 *
 * The retrieval score IS the routing confidence: the on-device model does not
 * expose a reliable confidence, so we never trust a model-reported number.
 */
class LexicalSafetyCardRetriever @Inject constructor() {

    fun bestCard(question: String, cards: List<SafetyCard>): ScoredCard? {
        if (question.isBlank() || cards.isEmpty()) return null
        return cards
            .map { ScoredCard(it, score(question, it)) }
            .filter { it.score > 0f }
            .maxWithOrNull(compareBy<ScoredCard> { it.score }.thenBy { it.card.id })
    }

    fun score(question: String, card: SafetyCard): Float {
        val questionNormalized = TextNormalizer.normalize(question)
        if (questionNormalized.isBlank()) return 0f
        val questionTokens = TextNormalizer.significantTokens(question).toSet()

        var best = 0f
        for (keyword in card.allKeywords()) {
            val keywordNormalized = TextNormalizer.normalize(keyword)
            if (keywordNormalized.isBlank()) continue

            val score = if (TextNormalizer.containsPhrase(questionNormalized, keywordNormalized)) {
                1f
            } else {
                val keywordTokens = TextNormalizer.significantTokens(keyword)
                if (keywordTokens.isEmpty()) continue
                val matched = keywordTokens.count { it in questionTokens }
                if (matched == 0) continue
                val precision = matched.toFloat() / keywordTokens.size
                val recall = matched.toFloat() / questionTokens.size.coerceAtLeast(1)
                precision * (0.62f + 0.38f * recall)
            }
            best = max(best, score)
        }

        if (best >= 0.35f) {
            val titleTokens = TextNormalizer.significantTokens(
                listOf(
                    card.title.en,
                    card.title.ur,
                    card.title.roman,
                ).joinToString(" "),
            ).toSet()
            if (titleTokens.isNotEmpty()) {
                val titleMatches = titleTokens.count { it in questionTokens }
                best += 0.08f * (titleMatches.toFloat() / titleTokens.size)
            }
        }

        return best.coerceIn(0f, 1f)
    }
}
