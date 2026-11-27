package com.mahbub.lantern.data.ai

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

/**
 * Structured-output schema for multilingual card matching. The question may be
 * English, Urdu or Roman Urdu; Nano returns the exact id of the best card, or
 * "none" when no card is a safe match.
 */
@Serializable
@Generable(description = "The safety card that best matches the person's question")
data class GeneratedMatch(
    @Guide(description = "The exact id of the best matching safety card, or none")
    val cardId: String,
) {
    companion object
}
