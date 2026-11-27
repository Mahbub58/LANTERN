package com.mahbub.lantern.data.fake

import com.mahbub.lantern.domain.ai.NanoStatus
import com.mahbub.lantern.domain.ai.OnDeviceInference
import com.mahbub.lantern.domain.ai.TextNormalizer
import com.mahbub.lantern.domain.model.AppLanguage
import com.mahbub.lantern.domain.model.SafetyCard
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class DeviceAnswerPayload(
    val cardId: String,
    val answer: String,
)

/**
 * Stand-in for Gemini Nano. It waits 800 ms, then writes a short answer using
 * only the retrieved card, in the requested language, in the strict JSON shape
 * the router validates.
 */
@Singleton
class FakeOnDeviceInference @Inject constructor() : OnDeviceInference {

    private val json = Json
    private val statusFlow = MutableStateFlow(NanoStatus.READY)

    override val status: Flow<NanoStatus> = statusFlow.asStateFlow()

    override suspend fun isAvailable(): Boolean = statusFlow.value == NanoStatus.READY

    /**
     * Simulates Nano's multilingual understanding with a small token-overlap
     * heuristic. The real adapter sends the question and the card catalog to
     * Gemini Nano; the fake only exists for tests and previews.
     */
    override suspend fun matchCard(question: String, cards: List<SafetyCard>): String? {
        delay(150)
        val questionTokens = TextNormalizer.significantTokens(question).toSet()
        if (questionTokens.isEmpty()) return null

        return cards
            .map { card ->
                val cardTokens = TextNormalizer.significantTokens(
                    buildString {
                        append(card.title.en).append(' ')
                        append(card.title.ur).append(' ')
                        append(card.title.roman).append(' ')
                        append(card.allKeywords().joinToString(" "))
                    },
                ).toSet()
                card to cardTokens.count { it in questionTokens }
            }
            .filter { (_, overlap) -> overlap > 0 }
            .maxByOrNull { (_, overlap) -> overlap }
            ?.first
            ?.id
    }

    override suspend fun answer(
        question: String,
        card: SafetyCard,
        language: AppLanguage,
    ): String {
        delay(800)
        val title = card.title.forLanguage(language)
        val steps = card.steps.forLanguage(language).take(2).joinToString(" ")
        val answer = listOf(title, steps).filter { it.isNotBlank() }.joinToString(" ")
        return json.encodeToString(DeviceAnswerPayload(cardId = card.id, answer = answer))
    }
}
