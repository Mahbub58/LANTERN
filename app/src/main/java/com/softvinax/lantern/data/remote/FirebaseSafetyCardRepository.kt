package com.softvinax.lantern.data.remote

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import com.softvinax.lantern.data.asset.SafetyCardBundleDto
import com.softvinax.lantern.data.mapper.toDomainValidated
import com.softvinax.lantern.domain.model.SafetyCard
import com.softvinax.lantern.domain.repository.AuthRepository
import com.softvinax.lantern.domain.repository.SafetyCardRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json

/**
 * Bundled cards are the offline fallback. A newer pack can be published as
 * `config/safety_cards` in Firestore (fields `version` and `json`); it is
 * validated, cached locally and used from then on. A bad or partial remote pack
 * is ignored so the vetted bundled guidance is never replaced by junk.
 */
@Singleton
class FirebaseSafetyCardRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository,
) : SafetyCardRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val loaded = CompletableDeferred<Unit>()
    private val versionFlow = MutableStateFlow("1.0.0-demo (bundled)")

    private var bundled: CardPack? = null
    private var active: CardPack? = null

    init {
        scope.launch {
            val result = runCatching { mutex.withLock { ensureLoadedLocked() } }
            result.onFailure { Log.e(TAG, "Bundled safety cards failed to load", it) }
            if (result.isSuccess) {
                loaded.complete(Unit)
                refresh()
            } else {
                loaded.completeExceptionally(
                    result.exceptionOrNull() ?: IllegalStateException("Card load failed"),
                )
            }
        }
    }

    override val version: Flow<String> = versionFlow.asStateFlow()

    override suspend fun getAll(): List<SafetyCard> {
        loaded.await()
        return mutex.withLock { active?.cards.orEmpty() }
    }

    override suspend fun getById(id: String): SafetyCard? =
        getAll().firstOrNull { it.id == id }

    override suspend fun refresh() {
        loaded.await()
        applyCachedPack()

        withTimeoutOrNull(AUTH_WAIT_MS) {
            authRepository.uid.filterNotNull().first()
        } ?: return

        val document = runCatching {
            Tasks.await(
                FirebaseFirestore.getInstance()
                    .collection(CONFIG_COLLECTION)
                    .document(CARDS_DOCUMENT)
                    .get(),
            )
        }.onFailure { Log.i(TAG, "Remote card pack unavailable: ${it.message}") }
            .getOrNull() ?: return

        val remoteVersion = document.getString("version")?.trim().orEmpty()
        val raw = document.getString("json")?.trim().orEmpty()
        if (remoteVersion.isEmpty() || raw.isEmpty()) return

        val bundle = runCatching { json.decodeFromString<SafetyCardBundleDto>(raw) }
            .onFailure { Log.w(TAG, "Remote card pack is not valid JSON", it) }
            .getOrNull() ?: return
        val cards = runCatching { bundle.cards.toDomainValidated() }
            .onFailure { Log.w(TAG, "Remote card pack failed validation", it) }
            .getOrNull() ?: return
        val pack = CardPack(bundle.version.ifBlank { remoteVersion }, cards)

        mutex.withLock {
            val currentVersion = active?.version ?: bundled?.version ?: "0.0.0"
            if (VersionComparator.isNewer(pack.version, currentVersion)) {
                active = pack
                cache(pack, raw)
                versionFlow.value = "${pack.version} (remote)"
                Log.i(TAG, "Loaded remote card pack ${pack.version}")
            }
        }
    }

    private fun ensureLoadedLocked() {
        if (bundled != null) return
        val raw = context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
        val bundle = json.decodeFromString<SafetyCardBundleDto>(raw)
        val cards = bundle.cards.toDomainValidated()
        val bundledPack = CardPack(bundle.version, cards)
        bundled = bundledPack

        val cached = readCached()
        active = if (cached != null && VersionComparator.isNewer(cached.version, bundledPack.version)) {
            versionFlow.value = "${cached.version} (remote)"
            cached
        } else {
            versionFlow.value = "${bundledPack.version} (bundled)"
            bundledPack
        }
    }

    private suspend fun applyCachedPack() {
        val cached = readCached() ?: return
        mutex.withLock {
            val currentVersion = active?.version ?: return@withLock
            if (VersionComparator.isNewer(cached.version, currentVersion)) {
                active = cached
                versionFlow.value = "${cached.version} (remote)"
            }
        }
    }

    private fun readCached(): CardPack? {
        val raw = prefs.getString(KEY_JSON, null) ?: return null
        val version = prefs.getString(KEY_VERSION, null) ?: return null
        return runCatching {
            val bundle = json.decodeFromString<SafetyCardBundleDto>(raw)
            CardPack(version, bundle.cards.toDomainValidated())
        }.getOrNull()
    }

    private fun cache(pack: CardPack, raw: String) {
        prefs.edit()
            .putString(KEY_JSON, raw)
            .putString(KEY_VERSION, pack.version)
            .apply()
    }

    private data class CardPack(
        val version: String,
        val cards: List<SafetyCard>,
    )

    private companion object {
        const val TAG = "LanternCards"
        const val ASSET_NAME = "safety_cards.json"
        const val CONFIG_COLLECTION = "config"
        const val CARDS_DOCUMENT = "safety_cards"
        const val PREFS = "lantern_cards"
        const val KEY_JSON = "remote_json"
        const val KEY_VERSION = "remote_version"
        const val AUTH_WAIT_MS = 8_000L
    }
}
