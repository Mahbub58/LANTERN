package com.softvinax.lantern.data.ai

import com.google.firebase.ai.annotations.Generable
import com.google.firebase.ai.annotations.Guide
import kotlinx.serialization.Serializable

/**
 * Structured-output schema used by both AI Logic paths.
 *
 * On-device structured output requires a `@Generable` data class and the
 * `firebase-ai-ksp-processor`. Using the schema instead of asking the model to
 * "please return JSON" makes the cardId/answer contract reliable on Nano 4.
 */
@Serializable
@Generable(description = "A grounded disaster-safety answer based only on one safety card")
data class GeneratedAnswer(
    @Guide(description = "The exact id of the safety card that was used")
    val cardId: String,
    @Guide(description = "A short answer in the requested language, at most 70 words")
    val answer: String,
) {
    companion object
}
