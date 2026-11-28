package com.mahbub.lantern.domain.ai

import com.mahbub.lantern.domain.model.AppLanguage
import com.mahbub.lantern.domain.model.SafetyCard
import kotlinx.coroutines.flow.Flow

enum class NanoStatus {
    UNAVAILABLE,
    NOT_DOWNLOADED,
    DOWNLOADING,
    READY,
}

/**
 * The one result shape returned by both AI paths.
 *
 * [matched] is the model's own "does one approved safety card fit this
 * question?" answer. The router shows the existing safety-card result page when
 * true, and the localized "we did not match your question" message when false.
 */
data class TriageResult(
    val matched: Boolean,
    val cardId: String? = null,
    val answer: String = "",
) {
    companion object {
        val NoMatch = TriageResult(matched = false)
    }
}

/**
 * On-device Gemini Nano adapter.
 *
 * Nano receives the same triage system prompt and the same approved card pack
 * as the cloud model, plus the person's question. It returns the same
 * [TriageResult] as the cloud, so the router and UI do not care which engine
 * produced the answer.
 */
interface OnDeviceInference {
    val status: Flow<NanoStatus>
    suspend fun isAvailable(): Boolean

    /**
     * Matches the question and writes the grounded answer in one Nano call.
     *
     * Returns [TriageResult.NoMatch] when Nano ran and chose no card. Throws
     * only when Nano is unavailable or inference fails, so the router can fall
     * back to cloud AI Logic.
     */
    suspend fun matchAndAnswer(
        question: String,
        cards: List<SafetyCard>,
        language: AppLanguage,
    ): TriageResult
}

interface CloudInference {
    /**
     * Sends only the scrubbed question and the safety-card catalog. The cloud
     * model receives the full card JSON as its system instruction, so it can
     * match in English, Urdu and Roman Urdu and answer from the chosen card.
     *
     * Throws when the request itself fails; a valid "no match" is returned as
     * [TriageResult.NoMatch] instead of an exception.
     */
    suspend fun matchAndAnswer(
        question: String,
        cards: List<SafetyCard>,
        language: AppLanguage,
    ): TriageResult
}
