package com.mahbub.lantern.data.remote

import android.content.Context
import android.util.Log
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.mahbub.lantern.data.asset.SafetyCardBundleDto
import com.mahbub.lantern.data.mapper.toDomainValidated
import com.mahbub.lantern.domain.model.SafetyCard
import com.mahbub.lantern.domain.repository.SafetyCardRepository
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

/**
 * Safety cards come from the Remote Config key `safety_cards`, which holds the
 * full card-pack JSON as a string.
 *
 * Offline behaviour:
 *  - Remote Config itself persists the last fetched value on disk.
 *  - The validated pack is also cached in SharedPreferences, so the last good
 *    pack is active immediately on the next launch, even with no signal.
 *  - If `safety_cards` is missing/blank/invalid, the bundled
 *    `assets/safety_cards.json` pack is used.
 *
 * A bad remote pack never replaces the last good cached or bundled pack.
 */
@Singleton
class FirebaseSafetyCardRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : SafetyCardRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val remoteConfig = FirebaseRemoteConfig.getInstance()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val loaded = CompletableDeferred<Unit>()
    private val versionFlow = MutableStateFlow("1.0.0-demo (bundled)")

    private var bundled: CardPack? = null
    private var active: CardPack? = null

    /** Raw JSON currently active, used to skip re-parsing identical values. */
    private var activeRaw: String? = null

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

        val raw = runCatching {
            remoteConfig.getString(RemoteConfigDefaults.SAFETY_CARDS_JSON).trim()
        }.onFailure {
            Log.w(TAG, "Remote Config safety_cards unavailable", it)
        }.getOrDefault("")

        if (raw.isBlank() || raw == "null") {
            Log.i(TAG, "No Remote Config safety_cards; using ${activeLabel()}")
            return
        }
        if (raw == activeRaw) return

        applyRemote(raw)
    }

    private suspend fun applyRemote(raw: String) {
        val bundle = runCatching { json.decodeFromString<SafetyCardBundleDto>(raw) }
            .onFailure { Log.w(TAG, "Remote Config safety_cards is not valid JSON", it) }
            .getOrNull() ?: return

        val cards = runCatching { bundle.cards.toDomainValidated() }
            .onFailure { Log.w(TAG, "Remote Config safety_cards failed validation", it) }
            .getOrNull() ?: return
        if (cards.isEmpty()) {
            Log.w(TAG, "Remote Config safety_cards has no valid cards")
            return
        }

        val pack = CardPack(bundle.version.ifBlank { "remote" }, cards)
        mutex.withLock {
            active = pack
            activeRaw = raw
            cache(pack, raw)
            versionFlow.value = "${pack.version} (remote config)"
        }
        Log.i(TAG, "Loaded Remote Config card pack ${pack.version} (${cards.size} cards)")
    }

    private fun ensureLoadedLocked() {
        if (bundled != null) return
        val raw = context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
        val bundle = json.decodeFromString<SafetyCardBundleDto>(raw)
        val cards = bundle.cards.toDomainValidated()
        val bundledPack = CardPack(bundle.version, cards)
        bundled = bundledPack

        val cached = readCached()
        if (cached != null) {
            active = cached
            activeRaw = prefs.getString(KEY_JSON, null)
            versionFlow.value = "${cached.version} (cached)"
            Log.i(TAG, "Using cached card pack ${cached.version} (${cached.cards.size} cards)")
        } else {
            active = bundledPack
            activeRaw = null
            versionFlow.value = "${bundledPack.version} (bundled)"
            Log.i(TAG, "Using bundled card pack ${bundledPack.version} (${bundledPack.cards.size} cards)")
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

    private fun activeLabel(): String = if (activeRaw != null) {
        "cached pack ${active?.version}"
    } else {
        "bundled pack ${active?.version}"
    }

    private data class CardPack(
        val version: String,
        val cards: List<SafetyCard>,
    )

    private companion object {
        const val TAG = "LanternCards"
        const val ASSET_NAME = "safety_cards.json"
        const val PREFS = "lantern_cards"
        const val KEY_JSON = "remote_config_json"
        const val KEY_VERSION = "remote_config_version"
    }
}
