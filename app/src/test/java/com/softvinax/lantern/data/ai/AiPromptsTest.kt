package com.softvinax.lantern.data.ai

import com.softvinax.lantern.domain.model.AppLanguage
import com.softvinax.lantern.domain.model.LocalizedList
import com.softvinax.lantern.domain.model.LocalizedText
import com.softvinax.lantern.domain.model.SafetyCard
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiPromptsTest {

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
    fun `on-device prompt is short, grounded and uses the structured contract`() {
        val prompt = AiPrompts.onDevice("pani barh raha hai", card, AppLanguage.ROMAN_UR)

        assertTrue(prompt.contains("Return the safety card id and the answer"))
        assertTrue(prompt.contains("SAFETY CARD (${card.id})"))
        assertTrue(prompt.contains("Roman Urdu"))
        assertTrue(prompt.contains("Flood ka pani barh raha hai"))
        assertTrue(prompt.contains("Buland jagah par jayen."))
        assertTrue(prompt.length < 4_000)
    }

    @Test
    fun `prompts never contain coordinates`() {
        val prompt = AiPrompts.cloud("water is rising", card, AppLanguage.EN)
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
