package com.mahbub.lantern.data.fake

import com.mahbub.lantern.domain.model.Report
import com.mahbub.lantern.domain.model.ReportDraft
import com.mahbub.lantern.domain.model.ReportStatus
import com.mahbub.lantern.domain.model.SyncState
import com.mahbub.lantern.domain.repository.ConnectivityRepository
import com.mahbub.lantern.domain.repository.ReportRepository
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory report store that behaves like the real offline-first repository:
 * a submitted report is QUEUED while offline and flips to SENT shortly after the
 * debug panel switches to online. Drafts never leave the device.
 */
@Singleton
class FakeReportRepository @Inject constructor(
    private val connectivityRepository: ConnectivityRepository,
) : ReportRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()
    private val reports = MutableStateFlow<List<Report>>(emptyList())
    private val drafts = ConcurrentHashMap<String, ReportDraft>()

    init {
        scope.launch {
            connectivityRepository.isOnline
                .distinctUntilChanged()
                .collect { online -> if (online) syncQueued() }
        }
    }

    override fun observeReports(): Flow<List<Report>> = reports.asStateFlow()

    override suspend fun getReport(id: String): Report? = reports.value.firstOrNull { it.id == id }

    override suspend fun saveDraft(draft: ReportDraft): ReportDraft {
        val safe = draft.sanitized()
        drafts[safe.id] = safe
        return safe
    }

    override suspend fun getDraft(id: String): ReportDraft? = drafts[id]

    override suspend fun updateDraft(draft: ReportDraft): ReportDraft {
        val safe = draft.sanitized()
        drafts[safe.id] = safe
        return safe
    }

    override suspend fun discardDraft(id: String) {
        drafts.remove(id)
    }

    override suspend fun submit(draft: ReportDraft): Report {
        val safe = draft.sanitized()
        val now = System.currentTimeMillis()
        val online = connectivityRepository.isOnline.first()
        val report = Report(
            id = "report-${UUID.randomUUID()}",
            description = safe.description,
            language = safe.language,
            type = safe.type,
            urgency = safe.urgency,
            peopleCount = safe.peopleCount,
            needs = safe.needs,
            placeDescription = safe.placeDescription,
            location = safe.location,
            syncState = if (online) SyncState.SENT else SyncState.QUEUED,
            status = ReportStatus.NEW,
            createdAt = now,
            updatedAt = now,
            sentAt = if (online) now else null,
        )

        mutex.withLock {
            drafts.remove(safe.id)
            reports.update { current -> listOf(report) + current }
        }
        return report
    }

    private suspend fun syncQueued() {
        if (reports.value.none { it.syncState == SyncState.QUEUED }) return
        // Small delay so the demo visibly shows "Queued" before "Sent".
        delay(700)
        if (connectivityRepository.isOnline.first().not()) return
        mutex.withLock {
            val now = System.currentTimeMillis()
            reports.update { current ->
                current.map { report ->
                    if (report.syncState == SyncState.QUEUED) {
                        report.copy(
                            syncState = SyncState.SENT,
                            sentAt = now,
                            updatedAt = now,
                        )
                    } else {
                        report
                    }
                }
            }
        }
    }
}
