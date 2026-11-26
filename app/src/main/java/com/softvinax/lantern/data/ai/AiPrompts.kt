package com.softvinax.lantern.data.ai

import com.softvinax.lantern.domain.model.AppLanguage
import com.softvinax.lantern.domain.model.SafetyCard

/**
 * Prompt builders for Firebase AI Logic.
 *
 * On-device models are single-turn and small: keep the prompt well under the
 * 4,000-token on-device limit, put the card first, and force the JSON contract
 * that [com.softvinax.lantern.domain.ai.GroundedAnswerValidator] enforces.
 *
 * The cloud prompt receives only text that has already passed through
 * [com.softvinax.lantern.domain.ai.PiiScrubber]. It never receives coordinates.
 */
internal object AiPrompts {

    fun onDevice(question: String, card: SafetyCard, language: AppLanguage): String = """
        You are Lantern, an offline disaster-safety helper.
        Answer ONLY from the safety card below. Do not use outside knowledge.
        Never give medical doses, medicine names or a diagnosis.
        Never include links, names, phone numbers or addresses.
        Keep the answer under 70 words.
        Reply in ${languageName(language)}.
        Return ONLY one JSON object, no markdown:
        {"cardId":"${card.id}","answer":"..."}

        SAFETY CARD (${card.id}):
        ${card.groundingText(language)}

        PERSON'S QUESTION:
        ${question.trim()}
    """.trimIndent()

    fun cloud(question: String, card: SafetyCard, language: AppLanguage): String = """
        You are Lantern, a disaster-safety helper for people in Pakistan.
        Answer ONLY from the safety card below. If the card does not cover the
        question, say that briefly instead of inventing advice.
        Never give medical doses, medicine names or a diagnosis.
        Never include links, names, phone numbers or addresses.
        Keep the answer under 80 words.
        Reply in ${languageName(language)}.
        Return ONLY one JSON object, no markdown:
        {"cardId":"${card.id}","answer":"..."}

        SAFETY CARD (${card.id}):
        ${card.groundingText(language)}

        PERSON'S QUESTION:
        ${question.trim()}
    """.trimIndent()

    fun urgency(description: String): String = """
        Classify the urgency of this disaster report on a scale of 1 to 5.
        1 = can wait, 5 = life-threatening right now.
        Return ONLY JSON: {"urgency":3}
        REPORT:
        ${description.trim()}
    """.trimIndent()

    fun duplicateGroups(reports: List<Pair<String, String>>): String = buildString {
        appendLine("Group reports that describe the same incident.")
        appendLine("Use only the ids below. Do not invent ids.")
        appendLine("Return ONLY JSON: {\"groups\":[[\"id1\",\"id2\"]]}")
        appendLine("REPORTS:")
        reports.forEach { (id, description) ->
            appendLine("$id: ${description.trim()}")
        }
    }

    fun situationBrief(alerts: List<String>, reports: List<String>, language: AppLanguage): String = buildString {
        appendLine("Write a short situation brief for a relief desk.")
        appendLine("Use only the alerts and reports below. Do not invent facts.")
        appendLine("Maximum 120 words. Reply in ${languageName(language)}.")
        appendLine("ALERTS:")
        alerts.forEach { appendLine("- ${it.trim()}") }
        appendLine("REPORTS:")
        reports.forEach { appendLine("- ${it.trim()}") }
    }

    private fun languageName(language: AppLanguage): String = when (language) {
        AppLanguage.EN -> "English"
        AppLanguage.UR -> "Urdu (Arabic script)"
        AppLanguage.ROMAN_UR -> "Roman Urdu (Latin script)"
    }
}
