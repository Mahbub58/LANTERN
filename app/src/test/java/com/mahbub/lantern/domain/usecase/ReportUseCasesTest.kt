package com.mahbub.lantern.domain.usecase

import com.mahbub.lantern.data.fake.FakeConnectivityRepository
import com.mahbub.lantern.data.fake.FakeReportRepository
import com.mahbub.lantern.domain.model.AppLanguage
import com.mahbub.lantern.domain.model.Need
import com.mahbub.lantern.domain.model.ReportDraft
import com.mahbub.lantern.domain.model.SyncState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportUseCasesTest {

    private val connectivity = FakeConnectivityRepository()
    private val repository = FakeReportRepository(connectivity)

    private val submit = SubmitReportUseCase(repository)
    private val observeMine = ObserveMyReportsUseCase(repository)
    private val observeOne = ObserveReportUseCase(repository)
    private val saveDraft = SaveReportDraftUseCase(repository)
    private val getDraft = GetReportDraftUseCase(repository)
    private val updateDraft = UpdateReportDraftUseCase(repository)
    private val discardDraft = DiscardReportDraftUseCase(repository)

    private fun draft(id: String = "draft-1") = ReportDraft(
        id = id,
        description = "  Roof cracked, two families trapped  ",
        language = AppLanguage.EN,
        urgency = 9,
        peopleCount = 0,
        needs = setOf(Need.RESCUE),
        createdAt = 1L,
    )

    @Test
    fun `submit sanitizes the draft and queues it offline`() = runTest {
        val report = submit(draft())

        assertEquals("Roof cracked, two families trapped", report.description)
        assertEquals(5, report.urgency)
        assertEquals(1, report.peopleCount)
        assertEquals(SyncState.QUEUED, report.syncState)
        assertNull(report.sentAt)
    }

    @Test
    fun `submitted report appears in my reports`() = runTest {
        val report = submit(draft())

        val reports = observeMine().first()

        assertTrue(reports.any { it.id == report.id })
    }

    @Test
    fun `observe report filters by id`() = runTest {
        val report = submit(draft())

        assertEquals(report.id, observeOne(report.id).first()?.id)
        assertNull(observeOne("missing").first())
    }

    @Test
    fun `draft round trip keeps sanitized fields`() = runTest {
        val saved = saveDraft(draft("draft-2"))

        assertEquals(5, saved.urgency)
        val loaded = getDraft("draft-2")
        assertNotNull(loaded)
        assertEquals("Roof cracked, two families trapped", loaded?.description)

        val updated = updateDraft(loaded!!.copy(placeDescription = "  Block B  "))
        assertEquals("Block B", updated.placeDescription)
    }

    @Test
    fun `discard removes the draft`() = runTest {
        saveDraft(draft("draft-3"))

        discardDraft("draft-3")

        assertNull(getDraft("draft-3"))
    }

    @Test
    fun `submitting online marks the report sent`() = runTest {
        connectivity.setSimulatedOnline(true)

        val report = submit(draft())

        assertEquals(SyncState.SENT, report.syncState)
        assertNotNull(report.sentAt)
    }
}
