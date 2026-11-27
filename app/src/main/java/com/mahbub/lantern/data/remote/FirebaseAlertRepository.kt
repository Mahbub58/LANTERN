package com.mahbub.lantern.data.remote

import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import com.mahbub.lantern.domain.model.Alert
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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Alerts are readable by any signed-in citizen (rules enforce this) and are
 * cached by Firestore offline persistence, so the last known warnings remain
 * visible with no signal.
 */
@Singleton
class FirebaseAlertRepository @Inject constructor(
    private val authRepository: AuthRepository,
) : AlertRepository {

    private val db = FirebaseFirestore.getInstance()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val alerts = MutableStateFlow<List<Alert>>(emptyList())
    private var registration: ListenerRegistration? = null

    init {
        scope.launch {
            authRepository.uid.filterNotNull().distinctUntilChanged().collect { listen() }
        }
    }

    override fun observeAlerts(): Flow<List<Alert>> = alerts.asStateFlow()

    override suspend fun refresh() {
        // A server read is best-effort: offline, the cached snapshot is used.
        withContext(Dispatchers.IO) {
            runCatching {
                Tasks.await(db.collection(COLLECTION).limit(1).get(Source.SERVER))
            }.onFailure { Log.i(TAG, "Alert refresh skipped: ${it.message}") }
        }
    }

    private fun listen() {
        registration?.remove()
        registration = db.collection(COLLECTION)
            .orderBy("issuedAt", Query.Direction.DESCENDING)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Alert listener failed", error)
                    return@addSnapshotListener
                }
                val next = snapshot?.documents.orEmpty().mapNotNull { document ->
                    val data = document.data ?: return@mapNotNull null
                    Alert(
                        id = document.id,
                        level = runCatching {
                            AlertLevel.valueOf(data["level"] as? String ?: AlertLevel.INFO.name)
                        }.getOrDefault(AlertLevel.INFO),
                        title = data["title"].toLocalizedText(),
                        message = data["message"].toLocalizedText(),
                        region = data["region"] as? String ?: "",
                        issuedAt = (data["issuedAt"] as? Number)?.toLong() ?: 0L,
                    )
                }
                alerts.value = next
            }
    }

    private fun Any?.toLocalizedText(): LocalizedText {
        val map = this as? Map<*, *> ?: return LocalizedText()
        return LocalizedText(
            en = map["en"] as? String ?: "",
            ur = map["ur"] as? String ?: "",
            roman = map["roman"] as? String ?: "",
        )
    }

    private companion object {
        const val TAG = "LanternAlerts"
        const val COLLECTION = "alerts"
    }
}
