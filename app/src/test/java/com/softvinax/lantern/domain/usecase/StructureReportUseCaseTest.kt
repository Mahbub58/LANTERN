package com.softvinax.lantern.domain.usecase

import com.softvinax.lantern.domain.model.AppLanguage
import com.softvinax.lantern.domain.model.EmergencyType
import com.softvinax.lantern.domain.model.Need
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StructureReportUseCaseTest {

    private val useCase = StructureReportUseCase()

    @Test
    fun `flood description is detected with urgency, people and needs`() {
        val draft = useCase(
            description = "Flood water is rising, 5 people trapped, we need rescue and drinking water",
            language = AppLanguage.EN,
        )

        assertEquals(EmergencyType.FLOOD, draft.type)
        assertEquals(5, draft.urgency)
        assertEquals(5, draft.peopleCount)
        assertTrue(Need.RESCUE in draft.needs)
        assertTrue(Need.WATER in draft.needs)
    }

    @Test
    fun `roman urdu earthquake is detected`() {
        val draft = useCase(
            description = "Zalzala aaya, family of 4 phansa hue hain",
            language = AppLanguage.ROMAN_UR,
        )

        assertEquals(EmergencyType.EARTHQUAKE, draft.type)
        assertEquals(4, draft.peopleCount)
        assertEquals(5, draft.urgency)
    }

    @Test
    fun `low urgency language is respected`() {
        val draft = useCase(
            description = "Minor leak, we are safe for now",
            language = AppLanguage.EN,
        )

        assertEquals(2, draft.urgency)
    }

    @Test
    fun `urdu flood description is detected`() {
        val draft = useCase(
            description = "سیلاب آ گیا ہے، پانی بڑھ رہا ہے",
            language = AppLanguage.UR,
        )

        assertEquals(EmergencyType.FLOOD, draft.type)
    }

    @Test
    fun `unknown description becomes other with defaults`() {
        val draft = useCase(
            description = "Something happened near the market",
            language = AppLanguage.EN,
        )

        assertEquals(EmergencyType.OTHER, draft.type)
        assertEquals(3, draft.urgency)
        assertEquals(1, draft.peopleCount)
    }

    @Test
    fun `blank description is trimmed and safe`() {
        val draft = useCase(description = "   ", language = AppLanguage.EN)

        assertEquals("", draft.description)
        assertEquals(EmergencyType.OTHER, draft.type)
        assertTrue(draft.urgency in 1..5)
    }

    @Test
    fun `needs are detected from roman urdu`() {
        val draft = useCase(
            description = "Khana aur kapre chahiye, bijli nahi hai",
            language = AppLanguage.ROMAN_UR,
        )

        assertTrue(Need.FOOD in draft.needs)
        assertTrue(Need.CLOTHING in draft.needs)
        assertTrue(Need.POWER in draft.needs)
    }
}
