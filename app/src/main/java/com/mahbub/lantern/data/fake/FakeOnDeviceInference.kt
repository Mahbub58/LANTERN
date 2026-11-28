package com.mahbub.lantern.data.fake

import com.mahbub.lantern.domain.ai.NanoStatus
import com.mahbub.lantern.domain.ai.OnDeviceInference
import com.mahbub.lantern.domain.ai.TextNormalizer
import com.mahbub.lantern.domain.ai.TriageResult
import com.mahbub.lantern.domain.model.AppLanguage
import com.mahbub.lantern.domain.model.SafetyCard
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Stand-in for Gemini Nano. It waits 800 ms, then matches the question with a
 * small token-overlap heuristic and answers from the chosen card in the
 * requested language. The real adapter gives Nano the same prompt and card JSON
 * as the cloud model and uses the same [TriageResult] shape.
 */
@Singleton
class FakeOnDeviceInference @Inject constructor() : OnDeviceInference {

    private val statusFlow = MutableStateFlow(NanoStatus.READY)

    override val status: Flow<NanoStatus> = statusFlow.asStateFlow()

    override suspend fun isAvailable(): Boolean = statusFlow.value == NanoStatus.READY

    override suspend fun matchAndAnswer(
        question: String,
        cards: List<SafetyCard>,
        language: AppLanguage,
    ): TriageResult {
        delay(800)
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
        val steps = card.steps.forLanguage(language).take(2).joinToString(" ")
        val answer = listOf(title, steps).filter { it.isNotBlank() }.joinToString(" ")
        return TriageResult(matched = true, cardId = card.id, answer = answer)
    }
}
