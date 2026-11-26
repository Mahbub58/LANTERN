package com.softvinax.lantern.domain.ai

import com.softvinax.lantern.domain.model.AppLanguage
import com.softvinax.lantern.testutil.floodCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GroundedAnswerValidatorTest {

    private val validator = GroundedAnswerValidator(SafetyGuard())
    private val card = floodCard()

    @Test
    fun `valid grounded json is accepted`() {
        val raw = """{"cardId":"flood-1","answer":"Move to the highest floor."}"""

        assertEquals("Move to the highest floor.", validator.validate(raw, card, AppLanguage.EN))
    }

    @Test
    fun `code fenced json is accepted`() {
        val raw = """
            ```json
            {"cardId":"flood-1","answer":"Move to the highest floor."}
            ```
        """.trimIndent()

        assertEquals("Move to the highest floor.", validator.validate(raw, card, AppLanguage.EN))
    }

    @Test
    fun `wrong card id is rejected`() {
        val raw = """{"cardId":"quake-1","answer":"Move to the highest floor."}"""

        assertNull(validator.validate(raw, card, AppLanguage.EN))
    }

    @Test
    fun `non json is rejected`() {
        assertNull(validator.validate("Move to the highest floor.", card, AppLanguage.EN))
    }

    @Test
    fun `blank answer is rejected`() {
        val raw = """{"cardId":"flood-1","answer":"   "}"""

        assertNull(validator.validate(raw, card, AppLanguage.EN))
    }

    @Test
    fun `dosage is rejected`() {
        val raw = """{"cardId":"flood-1","answer":"Take 2 tablets and move upstairs."}"""

        assertNull(validator.validate(raw, card, AppLanguage.EN))
    }

    @Test
    fun `diagnosis is rejected`() {
        val raw = """{"cardId":"flood-1","answer":"You probably have dengue, go upstairs."}"""

        assertNull(validator.validate(raw, card, AppLanguage.EN))
    }

    @Test
    fun `ungrounded advice is rejected`() {
        val raw = """{"cardId":"flood-1","answer":"Fly to the moon immediately."}"""

        assertNull(validator.validate(raw, card, AppLanguage.EN))
    }

    @Test
    fun `url is rejected`() {
        val raw = """{"cardId":"flood-1","answer":"Move to the highest floor. See https://evil.example"}"""

        assertNull(validator.validate(raw, card, AppLanguage.EN))
    }

    @Test
    fun `leading emergency disclaimer is stripped`() {
        val raw = """
            {"cardId":"flood-1","answer":"Call your local emergency number first if you can.\nMove to the highest floor."}
        """.trimIndent()

        assertEquals("Move to the highest floor.", validator.validate(raw, card, AppLanguage.EN))
    }

    @Test
    fun `grounding accepts roman urdu from the same card`() {
        val raw = """{"cardId":"flood-1","answer":"Sab se oopar wali manzil par jayen."}"""

        assertEquals(
            "Sab se oopar wali manzil par jayen.",
            validator.validate(raw, card, AppLanguage.ROMAN_UR),
        )
    }
}
