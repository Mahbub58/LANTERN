package com.mahbub.lantern.data.remote

import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Source
import com.mahbub.lantern.domain.model.Alert
import com.mahbub.lantern.domain.model.AlertKind
import com.mahbub.lantern.domain.model.AlertLevel
import com.mahbub.lantern.domain.model.LocalizedText
import com.mahbub.lantern.domain.repository.AlertRepository
import com.mahbub.lantern.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Reads two responder feeds in realtime:
 *
 *  - `alerts`   — general information and all-clear updates
 *  - `warnings` — active hazard warnings
 *
 * Both collections use the same document shape:
 * `{ level, title, message, region, createdAt }`.
 *
 * Documents are merged and sorted newest first. Firestore offline persistence
 * keeps the last snapshot available when there is no signal.
 */
@Singleton
class FirebaseAlertRepository @Inject constructor(
    private val authRepository: AuthRepository,
) : AlertRepository {

    private val db = FirebaseFirestore.getInstance()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val alertFeed = MutableStateFlow<List<Alert>>(emptyList())
    private val warningFeed = MutableStateFlow<List<Alert>>(emptyList())
    private var registrations: List<ListenerRegistration> = emptyList()

    init {
        scope.launch {
            authRepository.uid.filterNotNull().distinctUntilChanged().collect { listen() }
        }
    }

    /** Merged `alerts` + `warnings`, newest first. */
    override fun observeAlerts(): Flow<List<Alert>> =
        combine(alertFeed, warningFeed) { alerts, warnings ->
            (alerts + warnings).sortedByDescending { it.issuedAt }
        }.distinctUntilChanged()

    override suspend fun refresh() {
        // A server read is best-effort: offline, the cached snapshot is used.
        withContext(Dispatchers.IO) {
            COLLECTIONS.forEach { collection ->
                runCatching {
                    Tasks.await(db.collection(collection).limit(1).get(Source.SERVER))
                }.onFailure {
                    Log.i(TAG, "Refresh skipped for $collection: ${it.message}")
                }
            }
        }
    }

    private fun listen() {
        registrations.forEach { it.remove() }
        registrations = listOf(
            listenTo(COLLECTION_ALERTS, AlertKind.ALERT, alertFeed),
            listenTo(COLLECTION_WARNINGS, AlertKind.WARNING, warningFeed),
        )
    }

    private fun listenTo(
        collection: String,
        kind: AlertKind,
        target: MutableStateFlow<List<Alert>>,
    ): ListenerRegistration = db.collection(collection)
        .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Listener failed for $collection", error)
                return@addSnapshotListener
            }
            target.value = snapshot?.documents.orEmpty().mapNotNull { document ->
                val data = document.data ?: return@mapNotNull null
                Alert(
                    id = document.id,
                    level = data["level"].toAlertLevel(),
                    title = data["title"].toLocalizedText(),
                    message = data["message"].toLocalizedText(),
                    region = (data["region"] as? String).orEmpty(),
                    // `createdAt` is the documented field; keep `issuedAt` as a fallback.
                    issuedAt = (data["createdAt"] ?: data["issuedAt"]).toEpochMillis(),
                    kind = kind,
                )
            }
        }

    private fun Any?.toAlertLevel(): AlertLevel {
        val raw = (this as? String)?.trim()?.uppercase() ?: return AlertLevel.INFO
        return AlertLevel.entries.firstOrNull { it.name == raw } ?: when (raw) {
            "CRITICAL", "EMERGENCY", "HIGH", "DANGER" -> AlertLevel.URGENT
            "WATCH", "MODERATE", "CAUTION" -> AlertLevel.WARNING
            else -> AlertLevel.INFO
        }
    }

    /** Accepts a plain string (the documented shape) or a localized map. */
    private fun Any?.toLocalizedText(): LocalizedText = when (this) {
        is String -> LocalizedText(en = this)
        is Map<*, *> -> LocalizedText(
            en = this["en"] as? String ?: "",
            ur = this["ur"] as? String ?: "",
            roman = this["roman"] as? String ?: "",
        )
        else -> LocalizedText()
    }

    /** Accepts a Firestore Timestamp, epoch millis/seconds, or an ISO string. */
    private fun Any?.toEpochMillis(): Long = when (this) {
        is Timestamp -> toDate().time
        is Number -> normalizeEpoch(toLong())
        is String -> runCatching { java.time.Instant.parse(this).toEpochMilli() }
            .getOrElse { normalizeEpoch(toLongOrNull() ?: 0L) }
        else -> 0L
    }

    private fun normalizeEpoch(value: Long): Long =
        if (value in 1..SECONDS_CUTOFF) value * 1_000 else value

    private companion object {
        const val TAG = "LanternAlerts"
        const val COLLECTION_ALERTS = "alerts"
        const val COLLECTION_WARNINGS = "warnings"
        val COLLECTIONS = listOf(COLLECTION_ALERTS, COLLECTION_WARNINGS)

        /** Anything below this is treated as epoch seconds rather than millis. */
        const val SECONDS_CUTOFF = 99_999_999_999L
    }
}
