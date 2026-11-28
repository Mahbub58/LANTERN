package com.mahbub.lantern.data.ai

import com.google.firebase.ai.annotations.Generable
import com.google.firebase.ai.annotations.Guide
import kotlinx.serialization.Serializable

/**
 * Structured-output schema used by both AI Logic paths.
 *
 * Gemini Nano (on-device) and the cloud model both receive the same safety-card
 * JSON and must return this exact object. Using a `@Generable` data class with
 * the `firebase-ai-ksp-processor` makes the contract reliable on Nano 4 and
 * means the app renders the same result page no matter which engine answered.
 */
@Serializable
@Generable(description = "A triage result for one question and the approved safety-card catalog")
data class GeneratedTriageResult(
    @Guide(description = "true only when exactly one safety card safely matches the question")
    val matched: Boolean,
    @Guide(description = "The exact id of the matched safety card, or none")
    val cardId: String,
    @Guide(description = "A short grounded answer in the requested language, at most 80 words; empty when not matched")
    val answer: String,
) {
    companion object
}
