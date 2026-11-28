package com.mahbub.lantern.domain.ai

import com.mahbub.lantern.domain.model.SafetyCard
import javax.inject.Inject
import kotlin.math.max

data class ScoredCard(
    val card: SafetyCard,
    val score: Float,
)

/**
 * Last-resort offline matcher across English, Urdu and Roman Urdu keywords.
 *
 * It is only used when Gemini Nano cannot run and the cloud is unavailable
 * (offline or disabled). The router still prefers AI for normal answers; this
 * keeps the app useful on devices where Nano is unsupported and there is no
 * signal, using the same approved safety cards.
 */
class LexicalSafetyCardRetriever @Inject constructor() {

    fun bestCard(question: String, cards: List<SafetyCard>): ScoredCard? {
        if (question.isBlank() || cards.isEmpty()) return null
        return cards
            .map { ScoredCard(it, score(question, it)) }
            .filter { it.score > 0f }
            .maxWithOrNull(
                compareBy<ScoredCard> { it.score }
                    // A longer matching keyword is more specific: prefer "flood water"
                    // over "flood" when both score the same.
                    .thenBy { matchedKeywordTokens(question, it.card) }
                    .thenByDescending { it.card.id },
            )
    }

    /** Token count of the longest keyword that the question actually matches. */
    private fun matchedKeywordTokens(question: String, card: SafetyCard): Int {
        val questionNormalized = TextNormalizer.normalize(question)
        val questionTokens = TextNormalizer.significantTokens(question).toSet()
        return card.allKeywords().maxOfOrNull { keyword ->
            val keywordTokens = TextNormalizer.significantTokens(keyword)
            val matches = keywordTokens.isNotEmpty() &&
                (TextNormalizer.containsPhrase(questionNormalized, TextNormalizer.normalize(keyword)) ||
                    keywordTokens.all { it in questionTokens })
            if (matches) keywordTokens.size else 0
        } ?: 0
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
