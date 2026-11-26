package com.softvinax.lantern.data.fake

import com.softvinax.lantern.domain.model.AppLanguage
import com.softvinax.lantern.domain.model.ReportDraft
import com.softvinax.lantern.domain.model.SyncState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * The demo promise: a report filed with no signal stays QUEUED on the phone and
 * flips to SENT by itself once the debug panel goes online.
 */
class FakeReportRepositoryTest {

    @Test
    fun `queued report flips to sent when connectivity returns`() = runBlocking {
        val connectivity = FakeConnectivityRepository()
        val repository = FakeReportRepository(connectivity)

        val draft = ReportDraft(
            id = "draft-1",
            description = "Two families trapped on the roof",
            language = AppLanguage.EN,
            createdAt = 1L,
        )

        val queued = repository.submit(draft)
        assertEquals(SyncState.QUEUED, queued.syncState)

        connectivity.setSimulatedOnline(true)

        val synced = withTimeout(5_000) {
            repository.observeReports().first { reports ->
                reports.any { it.id == queued.id && it.syncState == SyncState.SENT }
            }
        }

        val report = synced.first { it.id == queued.id }
        assertEquals(SyncState.SENT, report.syncState)
        assertNotNull(report.sentAt)
    }
}
