package com.mahbub.lantern.data.ai

import com.mahbub.lantern.data.asset.SafetyCardBundleDto
import com.mahbub.lantern.data.mapper.toDomainValidated
import com.mahbub.lantern.domain.model.AppLanguage
import com.mahbub.lantern.domain.model.LocalizedList
import com.mahbub.lantern.domain.model.LocalizedText
import com.mahbub.lantern.domain.model.SafetyCard
import java.io.File
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiPromptsTest {

    private val testJson = Json { ignoreUnknownKeys = true }

    private val card = SafetyCard(
        id = "flood-water-rising",
        title = LocalizedText(
            en = "Flood water is rising",
            ur = "سیلاب کا پانی بڑھ رہا ہے",
            roman = "Flood ka pani barh raha hai",
        ),
        steps = LocalizedList(
            en = listOf("Move to higher ground."),
            ur = listOf("بلند جگہ پر جائیں۔"),
            roman = listOf("Buland jagah par jayen."),
        ),
        never = LocalizedList(en = listOf("Never drive through flood water.")),
        source = "TO VERIFY",
        keywords = LocalizedList(en = listOf("flood water rising")),
    )

    @Test
    fun `triage prompt contains the full card json and the shared contract`() {
        val prompt = AiPrompts.triageSystem(listOf(card), AppLanguage.ROMAN_UR)

        assertTrue(prompt.contains("SAFETY_CARDS_JSON:"))
        assertTrue(prompt.contains("\"id\":\"${card.id}\""))
        assertTrue(prompt.contains("\"roman\":\"Flood ka pani barh raha hai\""))
        assertTrue(prompt.contains("Buland jagah par jayen."))
        assertTrue(prompt.contains("\"matched\":true"))
        assertTrue(prompt.contains("\"matched\":false"))
        assertTrue(prompt.contains("Roman Urdu"))
        assertTrue(prompt.contains("Never give medical doses"))
        // Nano's 4,000-token limit: no audit URLs, only the answer-language steps.
        assertFalse(prompt.contains("https://"))
        assertTrue(prompt.contains("\"steps\":[\"Buland jagah par jayen.\"]"))
    }

    @Test
    fun `the same triage prompt is suitable for both engines`() {
        val prompt = AiPrompts.triageSystem(listOf(card), AppLanguage.EN)

        // No cloud-only wording and no on-device-only wording: one prompt.
        assertTrue(prompt.contains("You are Lantern"))
        assertTrue(prompt.contains("STEP 1 - MATCH"))
        assertTrue(prompt.contains("STEP 2 - ANSWER"))
        assertFalse(prompt.contains("cloud", ignoreCase = true))
        assertFalse(prompt.contains("on-device", ignoreCase = true))
    }

    @Test
    fun `triage user prompt contains only the question`() {
        val prompt = AiPrompts.triageQuestion("  pani barh raha hai  ")

        assertTrue(prompt.contains("PERSON'S QUESTION:"))
        assertTrue(prompt.contains("pani barh raha hai"))
        assertFalse(prompt.contains("SAFETY_CARDS_JSON"))
    }

    @Test
    fun `bundled pack stays inside the on-device prompt budget`() {
        val asset = File("src/main/assets/safety_cards.json")
        assertTrue("Bundled card asset missing at ${asset.absolutePath}", asset.exists())

        val cards = testJson
            .decodeFromString<SafetyCardBundleDto>(asset.readText())
            .cards
            .toDomainValidated()
        assertEquals(6, cards.size)

        val question = "flood water rising"
        AppLanguage.entries.forEach { language ->
            val prompt = buildString {
                appendLine(AiPrompts.triageSystem(cards, language))
                appendLine()
                append(AiPrompts.triageQuestion(question))
            }
            assertTrue(
                "$language prompt is ${prompt.length} chars, above the 4,000-token guard",
                prompt.length <= 10_000,
            )
            cards.forEach { card ->
                assertTrue("$language prompt is missing ${card.id}", prompt.contains(card.id))
            }
        }
    }

    @Test
    fun `prompts never contain coordinates`() {
        val prompt = AiPrompts.triageSystem(listOf(card), AppLanguage.EN)
        assertFalse(prompt.contains("latitude", ignoreCase = true))
        assertFalse(prompt.contains("longitude", ignoreCase = true))
        assertFalse(prompt.contains("24.8607"))
    }

    @Test
    fun `urgency prompt asks for a one-field json object`() {
        val prompt = AiPrompts.urgency("wall collapsed, people trapped")
        assertTrue(prompt.contains("\"urgency\""))
        assertTrue(prompt.contains("wall collapsed"))
    }

    @Test
    fun `duplicate prompt lists every id`() {
        val prompt = AiPrompts.duplicateGroups(
            listOf("r1" to "flood at the market", "r2" to "market is flooded"),
        )
        assertTrue(prompt.contains("r1: flood at the market"))
        assertTrue(prompt.contains("r2: market is flooded"))
    }
}
