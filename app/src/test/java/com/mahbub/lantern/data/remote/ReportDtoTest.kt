package com.mahbub.lantern.data.remote

import com.mahbub.lantern.domain.model.AppLanguage
import com.mahbub.lantern.domain.model.EmergencyType
import com.mahbub.lantern.domain.model.Need
import com.mahbub.lantern.domain.model.Report
import com.mahbub.lantern.domain.model.ReportDraft
import com.mahbub.lantern.domain.model.ReportLocation
import com.mahbub.lantern.domain.model.ReportStatus
import com.mahbub.lantern.domain.model.SyncState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportDtoTest {

    private val report = Report(
        id = "report-1",
        description = "Flood water entered the house, two people trapped.",
        language = AppLanguage.ROMAN_UR,
        type = EmergencyType.FLOOD,
        urgency = 5,
        peopleCount = 2,
        needs = setOf(Need.RESCUE, Need.WATER),
        placeDescription = "Near the market",
        location = ReportLocation(24.8607, 67.0011, 12f, "Malir"),
        syncState = SyncState.QUEUED,
        status = ReportStatus.NEW,
        createdAt = 1_700_000_000_000,
        updatedAt = 1_700_000_000_100,
        sentAt = 1_700_000_000_100,
    )

    @Test
    fun `firestore body stores the uid and every field`() {
        val map = ReportDto.fromDomain(report).toFirestore("uid-1")

        assertEquals("uid-1", map[FIELD_UID])
        assertEquals("report-1", ReportDto.fromDomain(report).id)
        assertEquals("ROMAN_UR", map[FIELD_LANGUAGE])
        assertEquals("FLOOD", map[FIELD_TYPE])
        assertEquals(5, map[FIELD_URGENCY])
        assertEquals(2, map[FIELD_PEOPLE])
        assertEquals(listOf("RESCUE", "WATER"), map[FIELD_NEEDS])
        assertEquals("Near the market", map[FIELD_PLACE])
        assertEquals("NEW", map[FIELD_STATUS])
        assertTrue(map[FIELD_LOCATION] is Map<*, *>)
    }

    @Test
    fun `firestore body omits location when the person did not share it`() {
        val withoutLocation = report.copy(location = null)
        val map = ReportDto.fromDomain(withoutLocation).toFirestore("uid-1")
        assertNull(map[FIELD_LOCATION])
    }

    @Test
    fun `snapshot mapping keeps the domain fields and applies the sync state`() {
        val map = ReportDto.fromDomain(report).toFirestore("uid-1")
        val dto = map.toReportDto("report-1")
        val domain = dto?.toDomain(SyncState.SENT)

        assertEquals("report-1", domain?.id)
        assertEquals(AppLanguage.ROMAN_UR, domain?.language)
        assertEquals(EmergencyType.FLOOD, domain?.type)
        assertEquals(5, domain?.urgency)
        assertEquals(2, domain?.peopleCount)
        assertEquals(setOf(Need.RESCUE, Need.WATER), domain?.needs)
        assertEquals(SyncState.SENT, domain?.syncState)
        assertEquals(24.8607, domain?.location?.latitude ?: 0.0, 0.0001)
    }

    @Test
    fun `malformed snapshots are ignored instead of crashing the list`() {
        assertNull(mapOf(FIELD_URGENCY to 3).toReportDto("broken"))
    }

    @Test
    fun `draft round trip keeps local-only fields`() {
        val draft = ReportDraft(
            id = "draft-1",
            description = "Wall cracked",
            language = AppLanguage.UR,
            type = EmergencyType.EARTHQUAKE,
            urgency = 4,
            peopleCount = 3,
            needs = setOf(Need.SHELTER),
            placeDescription = "Gulshan",
            location = ReportLocation(24.9, 67.1),
            createdAt = 1_700_000_000_000,
        )
        val roundTrip = ReportDraftDto.fromDomain(draft).toDomain()

        assertEquals("draft-1", roundTrip.id)
        assertEquals(AppLanguage.UR, roundTrip.language)
        assertEquals(EmergencyType.EARTHQUAKE, roundTrip.type)
        assertEquals(setOf(Need.SHELTER), roundTrip.needs)
        assertEquals(24.9, roundTrip.location?.latitude ?: 0.0, 0.0001)
    }
}
