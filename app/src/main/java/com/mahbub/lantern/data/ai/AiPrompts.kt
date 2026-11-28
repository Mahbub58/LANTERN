package com.mahbub.lantern.data.ai

import com.mahbub.lantern.domain.model.AppLanguage
import com.mahbub.lantern.domain.model.SafetyCard
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Prompt builders for Firebase AI Logic.
 *
 * On-device and cloud both use [triageSystem] plus [triageQuestion]. The model
 * matches the question to the approved card pack and returns the same
 * structured `{matched, cardId, answer}` object, so the app has one result path.
 *
 * The cloud prompt receives only text that has already passed through
 * [com.mahbub.lantern.domain.ai.PiiScrubber]. It never receives coordinates.
 */
internal object AiPrompts {

    /**
     * The shared triage system instruction.
     *
     * This is the prompt both Gemini Nano and the cloud model see as
     * `systemInstruction`. It embeds the complete safety-card catalog as JSON,
     * exactly as the app uses it, so the model can match meaning (not keywords)
     * in English, Urdu or Roman Urdu and answer strictly from the chosen card.
     *
     * The response contract matches [GeneratedTriageResult]:
     *   {"matched":true,"cardId":"<id>","answer":"<text>"}
     *   {"matched":false,"cardId":"none","answer":""}
     */
    fun triageSystem(cards: List<SafetyCard>, language: AppLanguage): String = buildString {
        appendLine("You are Lantern, a disaster-safety helper for people in Pakistan.")
        appendLine("You receive exactly one person's question and a catalog of approved safety cards.")
        appendLine("The question can be in English, Urdu (Arabic script) or Roman Urdu (Latin script).")
        appendLine("STEP 1 - MATCH: Choose the single card that safely answers the question.")
        appendLine("Understand the meaning, not only the exact words or keywords.")
        appendLine("An exact title or keyword phrase is a strong match.")
        appendLine("If the question describes a card's situation, choose that card even when the wording is short or informal.")
        appendLine("STEP 2 - ANSWER: If a card matches, answer ONLY from that card's title, steps and never list.")
        appendLine("Do not use outside knowledge and never invent advice or a card id.")
        appendLine("If no card safely matches, do not guess: return matched=false.")
        appendLine("Return exactly one JSON object and no other text:")
        appendLine("{\"matched\":true,\"cardId\":\"<exact card id>\",\"answer\":\"<answer>\"}")
        appendLine("When no card matches, return exactly:")
        appendLine("{\"matched\":false,\"cardId\":\"none\",\"answer\":\"\"}")
        appendLine("RULES:")
        appendLine("- cardId MUST be one of the ids in SAFETY_CARDS_JSON, or \"none\".")
        appendLine("- Reply in ${languageName(language)}.")
        appendLine("- Keep the answer under 80 words.")
        appendLine("- Never give medical doses, medicine names or a diagnosis.")
        appendLine("- Never include links, names, phone numbers or addresses.")
        appendLine("- Never invent a safety card, a step or advice that is not in the matched card.")
        appendLine("- If the person is in immediate danger, tell them to call the local emergency number first.")
        appendLine()
        appendLine("SAFETY_CARDS_JSON:")
        append(cardsJson(cards, language))
    }

    /** The only user content sent with [triageSystem]. */
    fun triageQuestion(question: String): String = buildString {
        appendLine("PERSON'S QUESTION:")
        append(question.trim())
    }

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

    /**
     * A compact projection of the same card pack.
     *
     * Gemini Nano has a 4,000-token request limit, so the full three-language
     * pack would be rejected before inference. The prompt keeps all titles and
     * keywords (for multilingual matching), the steps/never list in the
     * requested answer language, and drops the audit-only `source` URLs. The
     * cloud model receives exactly the same projection.
     */
    private fun cardsJson(cards: List<SafetyCard>, language: AppLanguage): String =
        compactJson.encodeToString(
            cards.map { card ->
                PromptCard(
                    id = card.id,
                    title = PromptLocalizedText(
                        en = card.title.en,
                        ur = card.title.ur,
                        roman = card.title.roman,
                    ),
                    steps = card.steps.forLanguage(language),
                    never = card.never.forLanguage(language),
                    keywords = PromptLocalizedList(
                        en = card.keywords.en,
                        ur = card.keywords.ur,
                        roman = card.keywords.roman,
                    ),
                )
            },
        )

    private val compactJson = Json
}

@Serializable
private data class PromptCard(
    val id: String,
    val title: PromptLocalizedText,
    val steps: List<String>,
    val never: List<String>,
    val keywords: PromptLocalizedList,
)

@Serializable
private data class PromptLocalizedText(
    val en: String,
    val ur: String,
    val roman: String,
)

@Serializable
private data class PromptLocalizedList(
    val en: List<String>,
    val ur: List<String>,
    val roman: List<String>,
)
