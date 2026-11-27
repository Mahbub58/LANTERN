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
 *  - If `safety_cards` is missing/blank/null, the bundled
 *    `assets/safety_cards.json` pack is activated and any cached remote pack
 *    is cleared. An invalid remote pack never replaces the active pack.
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
            runCatching { mutex.withLock { ensureLoadedLocked() } }
                .onFailure { Log.e(TAG, "Bundled safety cards failed to load", it) }
            // Even when the bundled asset is unreadable, keep going: the Remote
            // Config pack can still supply cards. getAll() returns an empty list
            // until one of them loads.
            loaded.complete(Unit)
            refresh()
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

        val value = runCatching {
            remoteConfig.getValue(RemoteConfigDefaults.SAFETY_CARDS_JSON)
        }.onFailure {
            Log.w(TAG, "Remote Config safety_cards unavailable", it)
        }.getOrNull()

        val source = sourceLabel(value?.source)
        val raw = RemoteCardJson.normalize(runCatching { value?.asString() }.getOrNull())

        if (raw.isBlank()) {
            useBundled("safety_cards ($source): ${RemoteCardJson.describe(raw)}")
            return
        }
        if (raw == activeRaw) {
            Log.i(TAG, "safety_cards ($source): ${raw.length} chars, matches active pack -> ${activeLabel()}")
            return
        }

        Log.i(TAG, "safety_cards ($source): ${RemoteCardJson.describe(raw)} -> validating")
        applyRemote(raw)
    }

    private suspend fun applyRemote(raw: String) {
        val bundle = runCatching { json.decodeFromString<SafetyCardBundleDto>(raw) }
            .onFailure {
                Log.w(TAG, "Remote Config safety_cards is not valid JSON (${raw.length} chars)", it)
            }
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

        val bundledPack = runCatching { readBundled() }
            .onFailure { Log.e(TAG, "Bundled asset $ASSET_NAME is unusable", it) }
            .getOrNull()
        bundled = bundledPack

        val cached = readCached()
        when {
            cached != null -> {
                active = cached
                activeRaw = prefs.getString(KEY_JSON, null)
                versionFlow.value = "${cached.version} (cached remote config)"
                Log.i(TAG, "Using cached Remote Config pack ${cached.version} (${cached.cards.size} cards)")
            }

            bundledPack != null -> {
                active = bundledPack
                activeRaw = null
                versionFlow.value = "${bundledPack.version} (bundled)"
                Log.i(TAG, "Using bundled card pack ${bundledPack.version} (${bundledPack.cards.size} cards)")
            }

            else -> {
                active = null
                activeRaw = null
                versionFlow.value = "unavailable"
                Log.w(TAG, "No bundled or cached card pack; waiting for Remote Config")
            }
        }
    }

    /**
     * `safety_cards` is missing or null: the bundled asset is the source of
     * truth again. The cached remote pack is cleared so a removed parameter
     * does not keep serving stale content.
     */
    private suspend fun useBundled(reason: String) {
        val pack = bundled
        if (pack == null) {
            Log.w(TAG, "$reason; bundled asset unavailable, keeping ${activeLabel()}")
            return
        }
        mutex.withLock {
            active = pack
            activeRaw = null
            prefs.edit().remove(KEY_JSON).remove(KEY_VERSION).apply()
            versionFlow.value = "${pack.version} (bundled)"
        }
        Log.i(TAG, "$reason -> using bundled pack ${pack.version} (${pack.cards.size} cards)")
    }

    private fun readBundled(): CardPack {
        val bytes = context.assets.open(ASSET_NAME).use { it.readBytes() }
        Log.i(TAG, "Bundled asset $ASSET_NAME: ${bytes.size} bytes")
        val bundle = json.decodeFromString<SafetyCardBundleDto>(bytes.toString(Charsets.UTF_8))
        val cards = bundle.cards.toDomainValidated()
        return CardPack(bundle.version, cards)
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

    private fun sourceLabel(source: Int?): String = when (source) {
        FirebaseRemoteConfig.VALUE_SOURCE_REMOTE -> "remote"
        FirebaseRemoteConfig.VALUE_SOURCE_DEFAULT -> "in-app default"
        FirebaseRemoteConfig.VALUE_SOURCE_STATIC -> "static"
        null -> "unavailable"
        else -> "source $source"
    }

    private fun activeLabel(): String = if (activeRaw != null) {
        "cached Remote Config pack ${active?.version}"
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
