package com.softvinax.lantern.data.remote

import android.util.Log
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.QuerySnapshot
import com.softvinax.lantern.domain.model.Report
import com.softvinax.lantern.domain.model.ReportDraft
import com.softvinax.lantern.domain.model.ReportStatus
import com.softvinax.lantern.domain.model.SyncState
import com.softvinax.lantern.domain.repository.AuthRepository
import com.softvinax.lantern.domain.repository.ConnectivityRepository
import com.softvinax.lantern.domain.repository.ReportRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Local-first report repository backed by Firestore.
 *
 * - `submit()` writes immediately; Firestore's offline persistence keeps the
 *   write on disk when there is no signal.
 * - The sync badge is derived from `snapshot.metadata.hasPendingWrites`:
 *   pending writes are [SyncState.QUEUED], server-acknowledged docs are SENT.
 * - Citizens only ever query their own `uid`, which matches the security rules.
 * - Drafts stay in [ReportDraftStore] and are never written to Firestore.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class FirebaseReportRepository @Inject constructor(
    private val authRepository: AuthRepository,
    private val connectivityRepository: ConnectivityRepository,
    private val draftStore: ReportDraftStore,
    private val localQueue: LocalReportQueue,
) : ReportRepository {

    private val db = FirebaseFirestore.getInstance()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val remoteReports: StateFlow<List<Report>> = authRepository.uid
        .distinctUntilChanged()
        .flatMapLatest { uid ->
            if (uid.isNullOrBlank()) {
                flowOf(emptyList())
            } else {
                reportsFlow(uid)
            }
        }
        .catch { throwable ->
            Log.w(TAG, "Report listener failed", throwable)
            emit(emptyList())
        }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    init {
        // Once auth exists, flush reports that were saved before sign-in.
        scope.launch {
            authRepository.uid.filterNotNull().distinctUntilChanged().collect { uid ->
                flushLocalQueue(uid)
            }
        }
        // When the signal returns, retry auth and the local queue.
        scope.launch {
            connectivityRepository.isOnline.distinctUntilChanged().collect { online ->
                if (!online) return@collect
                val uid = runCatching { authRepository.ensureAnonymousUid() }.getOrNull()
                    ?: return@collect
                flushLocalQueue(uid)
            }
        }
        // Keep a local copy until the server acknowledges the write. This means
        // a rejected write is never silently lost.
        scope.launch {
            remoteReports.collect { remote ->
                localQueue.removeAll(
                    remote.filter { it.syncState == SyncState.SENT }.map { it.id }.toSet(),
                )
            }
        }
    }

    override fun observeReports(): Flow<List<Report>> =
        combine(remoteReports, localQueue.reports) { remote, local ->
            val remoteIds = remote.mapTo(HashSet()) { it.id }
            (remote + local.filterNot { it.id in remoteIds })
                .sortedByDescending { it.createdAt }
        }

    override suspend fun getReport(id: String): Report? = withTimeoutOrNull(5_000) {
        observeReports().first { reports -> reports.any { it.id == id } }
            .firstOrNull { it.id == id }
    }

    override suspend fun saveDraft(draft: ReportDraft): ReportDraft {
        val safe = draft.sanitized()
        draftStore.save(safe)
        return safe
    }

    override suspend fun getDraft(id: String): ReportDraft? = draftStore.get(id)

    override suspend fun updateDraft(draft: ReportDraft): ReportDraft {
        val safe = draft.sanitized()
        draftStore.save(safe)
        return safe
    }

    override suspend fun discardDraft(id: String) = draftStore.discard(id)

    override suspend fun submit(draft: ReportDraft): Report {
        val safe = draft.sanitized()
        val now = System.currentTimeMillis()
        val id = db.collection(COLLECTION).document().id
        val uid = runCatching { authRepository.ensureAnonymousUid() }.getOrNull()
        val online = connectivityRepository.isOnline.first()

        val report = Report(
            id = id,
            description = safe.description,
            language = safe.language,
            type = safe.type,
            urgency = safe.urgency,
            peopleCount = safe.peopleCount,
            needs = safe.needs,
            placeDescription = safe.placeDescription,
            location = safe.location,
            syncState = if (online && uid != null) SyncState.SENT else SyncState.QUEUED,
            status = ReportStatus.NEW,
            createdAt = now,
            updatedAt = now,
            // Optimistic; the sync badge is driven by Firestore metadata.
            sentAt = now,
        )

        if (uid != null) {
            writeReport(report, uid)
        } else {
            localQueue.add(report)
        }
        draftStore.discard(safe.id)
        return report
    }

    private fun reportsFlow(uid: String): Flow<List<Report>> = callbackFlow {
        val query = db.collection(COLLECTION).whereEqualTo(FIELD_UID, uid)
        val registration = query.addSnapshotListener(
            MetadataChanges.INCLUDE,
            EventListener<QuerySnapshot> { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Report snapshot error", error)
                    close(error)
                    return@EventListener
                }
                val reports = snapshot?.documents.orEmpty().mapNotNull { document ->
                    val data = document.data ?: return@mapNotNull null
                    data.toReportDto(document.id)?.toDomain(
                        if (document.metadata.hasPendingWrites()) {
                            SyncState.QUEUED
                        } else {
                            SyncState.SENT
                        },
                    )
                }
                trySend(reports)
            },
        )
        awaitClose { registration.remove() }
    }

    private fun writeReport(report: Report, uid: String) {
        runCatching {
            db.collection(COLLECTION)
                .document(report.id)
                .set(ReportDto.fromDomain(report).toFirestore(uid))
                .addOnFailureListener { throwable ->
                    Log.w(TAG, "Report write rejected: ${report.id}", throwable)
                }
        }.onFailure { throwable ->
            Log.w(TAG, "Could not start report write: ${report.id}", throwable)
        }
    }

    private suspend fun flushLocalQueue(uid: String) {
        localQueue.snapshot().forEach { report ->
            writeReport(report, uid)
        }
    }

    private companion object {
        const val TAG = "LanternReports"
        const val COLLECTION = "reports"
    }
}
