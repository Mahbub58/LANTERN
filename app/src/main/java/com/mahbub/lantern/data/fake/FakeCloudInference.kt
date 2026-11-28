package com.mahbub.lantern.data.fake

import com.mahbub.lantern.domain.ai.CloudInference
import com.mahbub.lantern.domain.ai.TextNormalizer
import com.mahbub.lantern.domain.ai.TriageResult
import com.mahbub.lantern.domain.model.AppLanguage
import com.mahbub.lantern.domain.model.SafetyCard
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay

/**
 * Offline demo stand-in for the cloud model. It receives only scrubbed text (the
 * router applies PiiScrubber before this call) and answers from the same card so
 * the demo never invents advice.
 */
@Singleton
class FakeCloudInference @Inject constructor() : CloudInference {

    override suspend fun matchAndAnswer(
        question: String,
        cards: List<SafetyCard>,
        language: AppLanguage,
    ): TriageResult {
        delay(1200)
        val questionTokens = TextNormalizer.significantTokens(question).toSet()
        if (questionTokens.isEmpty()) return TriageResult.NoMatch

        val card = cards
            .map { candidate ->
                val cardTokens = TextNormalizer.significantTokens(
                    buildString {
                        append(candidate.title.en).append(' ')
                        append(candidate.title.ur).append(' ')
                        append(candidate.title.roman).append(' ')
                        append(candidate.allKeywords().joinToString(" "))
                    },
                ).toSet()
                candidate to cardTokens.count { it in questionTokens }
            }
            .filter { (_, overlap) -> overlap > 0 }
            .maxByOrNull { (_, overlap) -> overlap }
            ?.first
            ?: return TriageResult.NoMatch

        val title = card.title.forLanguage(language)
        val steps = card.steps.forLanguage(language).take(3).joinToString(" ")
        val answer = listOf(title, steps).filter { it.isNotBlank() }.joinToString(" ")
        return TriageResult(matched = true, cardId = card.id, answer = answer)
    }
}
