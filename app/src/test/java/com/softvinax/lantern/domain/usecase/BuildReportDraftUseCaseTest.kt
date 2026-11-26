package com.softvinax.lantern.domain.usecase

import com.softvinax.lantern.domain.model.AppLanguage
import com.softvinax.lantern.domain.model.EmergencyType
import com.softvinax.lantern.domain.model.Need
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildReportDraftUseCaseTest {

    private val useCase = BuildReportDraftUseCase()

    @Test
    fun `water and food stay at urgency three`() {
        val draft = useCase(
            type = EmergencyType.FLOOD,
            peopleCount = 2,
            needs = setOf(Need.WATER, Need.FOOD),
            place = "Mingora Bridge",
            location = null,
            language = AppLanguage.EN,
            now = 1L,
        )

        assertEquals(EmergencyType.FLOOD, draft.type)
        assertEquals(3, draft.urgency)
        assertEquals(2, draft.peopleCount)
        assertEquals(setOf(Need.WATER, Need.FOOD), draft.needs)
        assertEquals("Mingora Bridge", draft.placeDescription)
        assertTrue(draft.description.contains("Flood"))
        assertTrue(draft.description.contains("Drinking water"))
        assertEquals(1L, draft.createdAt)
    }

    @Test
    fun `rescue raises urgency to five`() {
        val draft = useCase(
            type = EmergencyType.EARTHQUAKE,
            peopleCount = 4,
            needs = setOf(Need.RESCUE),
            place = "",
            location = null,
            language = AppLanguage.ROMAN_UR,
        )

        assertEquals(5, draft.urgency)
        assertEquals(EmergencyType.EARTHQUAKE, draft.type)
    }

    @Test
    fun `medical help raises urgency to five`() {
        assertEquals(5, useCase.urgencyFor(setOf(Need.MEDICAL)))
    }

    @Test
    fun `people are clamped to a safe range`() {
        val draft = useCase(
            type = EmergencyType.LANDSLIDE,
            peopleCount = 200,
            needs = emptySet(),
            place = "",
            location = null,
            language = AppLanguage.EN,
        )

        assertEquals(99, draft.peopleCount)
        assertEquals(3, draft.urgency)
    }
}
