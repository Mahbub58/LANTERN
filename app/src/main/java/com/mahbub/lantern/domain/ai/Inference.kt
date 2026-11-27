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
 * On-device Gemini Nano adapter. The implementation must return strict JSON:
 * {"cardId":"...","answer":"..."} grounded only in the supplied card.
 */
interface OnDeviceInference {
    val status: Flow<NanoStatus>
    suspend fun isAvailable(): Boolean
    suspend fun answer(question: String, card: SafetyCard, language: AppLanguage): String
}

interface CloudInference {
    suspend fun answer(question: String, card: SafetyCard, language: AppLanguage): String
}
