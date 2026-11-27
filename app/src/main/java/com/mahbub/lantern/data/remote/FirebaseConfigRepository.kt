package com.mahbub.lantern.data.remote

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.mahbub.lantern.BuildConfig
import com.mahbub.lantern.domain.model.AlertLevel
import com.mahbub.lantern.domain.model.AppLanguage
import com.mahbub.lantern.domain.model.EmergencyNumber
import com.mahbub.lantern.domain.model.InferenceSource
import com.mahbub.lantern.domain.model.RoutingConfig
import com.mahbub.lantern.domain.repository.ConfigRepository
import com.mahbub.lantern.domain.repository.ConnectivityRepository
import com.mahbub.lantern.domain.repository.SafetyCardRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Remote Config is the live control plane for routing, alert level, emergency
 * numbers and the card-pack version. Local prefs keep the language choice.
 *
 * The debug panel's threshold slider is a local override for the session; when
 * it has not been touched, the Remote Config value is used. Remote Config
 * real-time updates are activated as soon as the console publishes them.
 */
@Singleton
class FirebaseConfigRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val safetyCardRepository: SafetyCardRepository,
    private val connectivityRepository: ConnectivityRepository,
) : ConfigRepository {

    private val remoteConfig = FirebaseRemoteConfig.getInstance()
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true }

    private val languageFlow = MutableStateFlow(readLanguage())
    private val languageChosenFlow = MutableStateFlow(prefs.getBoolean(KEY_CHOSEN, false))
    private val remote = MutableStateFlow(RemoteSnapshot())
    private val localRouting = MutableStateFlow<RoutingConfig?>(null)
    private val localAlertLevel = MutableStateFlow<AlertLevel?>(null)
    private val lastInference = MutableStateFlow<InferenceSource?>(null)

    override val language: Flow<AppLanguage> = languageFlow.asStateFlow()
    override val languageChosen: Flow<Boolean> = languageChosenFlow.asStateFlow()
    override val routingConfig: Flow<RoutingConfig> =
        combine(remote, localRouting) { snapshot, override -> override ?: snapshot.routing }
    override val alertLevel: Flow<AlertLevel> =
        combine(remote, localAlertLevel) { snapshot, override -> override ?: snapshot.alertLevel }
    override val emergencyNumbers: Flow<List<EmergencyNumber>> =
        remote.map { it.emergencyNumbers }
    override val cardsVersion: Flow<String> = safetyCardRepository.version
    override val lastInferenceSource: Flow<InferenceSource?> = lastInference.asStateFlow()

    init {
        scope.launch {
            runCatching {
                val settings = FirebaseRemoteConfigSettings.Builder()
                    // Always fetch in debug so a newly published pack is picked up
                    // on the next launch; 15 minutes in release to stay well inside
                    // the Remote Config fetch quota.
                    .setMinimumFetchIntervalInSeconds(if (BuildConfig.DEBUG) 0L else 900L)
                    .build()
                Tasks.await(remoteConfig.setConfigSettingsAsync(settings))
                Tasks.await(remoteConfig.setDefaultsAsync(RemoteConfigDefaults.values))
                val updated = Tasks.await(remoteConfig.fetchAndActivate())
                val info = remoteConfig.info
                Log.i(
                    TAG,
                    "Remote Config fetch: updated=$updated status=${info.lastFetchStatus} " +
                        "keys=${remoteConfig.all.keys}",
                )
            }.onFailure { Log.w(TAG, "Remote Config initial fetch failed", it) }
            readRemote()
            remoteConfig.addOnConfigUpdateListener(object : ConfigUpdateListener {
                override fun onUpdate(configUpdate: ConfigUpdate) {
                    Log.i(TAG, "Remote Config update: ${configUpdate.updatedKeys}")
                    scope.launch {
                        runCatching { Tasks.await(remoteConfig.activate()) }
                        readRemote()
                    }
                }

                override fun onError(error: FirebaseRemoteConfigException) {
                    Log.w(TAG, "Remote Config realtime error ${error.code}", error)
                }
            })
        }

        // Fetch again whenever the device regains a connection.
        scope.launch {
            connectivityRepository.isOnline.distinctUntilChanged().collect { online ->
                if (!online) return@collect
                runCatching {
                    val updated = Tasks.await(remoteConfig.fetchAndActivate())
                    Log.i(TAG, "Remote Config refetch on reconnect: updated=$updated")
                }.onFailure { Log.w(TAG, "Remote Config refetch failed", it) }
                readRemote()
            }
        }
    }

    override suspend fun setLanguage(language: AppLanguage) {
        languageFlow.value = language
        languageChosenFlow.value = true
        prefs.edit()
            .putString(KEY_LANGUAGE, language.name)
            .putBoolean(KEY_CHOSEN, true)
            .apply()
    }

    override suspend fun setRoutingConfig(config: RoutingConfig) {
        localRouting.value = config.sanitized()
    }

    override suspend fun setAlertLevel(level: AlertLevel) {
        localAlertLevel.value = level
    }

    override fun recordInference(source: InferenceSource) {
        lastInference.value = source
    }

    private fun readRemote() {
        val threshold = runCatching {
            remoteConfig.getDouble(RemoteConfigDefaults.CONFIDENCE_THRESHOLD)
        }.getOrDefault(RoutingConfig.DEFAULT_THRESHOLD.toDouble()).toFloat().coerceIn(0f, 1f)

        val cloudFallback = runCatching {
            remoteConfig.getBoolean(RemoteConfigDefaults.CLOUD_FALLBACK_ENABLED)
        }.getOrDefault(true)

        val level = runCatching {
            AlertLevel.valueOf(remoteConfig.getString(RemoteConfigDefaults.ALERT_LEVEL))
        }.getOrDefault(AlertLevel.WARNING)

        val numbers = runCatching {
            json.decodeFromString(
                ListSerializer(EmergencyNumberDto.serializer()),
                remoteConfig.getString(RemoteConfigDefaults.EMERGENCY_NUMBERS_JSON),
            ).map(EmergencyNumberDto::toDomain).ifEmpty { RemoteConfigDefaults.emergencyNumbers }
        }.getOrDefault(RemoteConfigDefaults.emergencyNumbers)

        remote.value = RemoteSnapshot(
            routing = RoutingConfig(
                confidenceThreshold = threshold,
                cloudFallbackEnabled = cloudFallback,
            ),
            alertLevel = level,
            emergencyNumbers = numbers,
        )

        // The `safety_cards` Remote Config value is the primary card pack.
        // refresh() keeps the cached/bundled pack when the value is missing.
        scope.launch {
            runCatching { safetyCardRepository.refresh() }
                .onFailure { Log.i(TAG, "Card pack refresh deferred: ${it.message}") }
        }
    }

    private fun readLanguage(): AppLanguage {
        val stored = prefs.getString(KEY_LANGUAGE, null) ?: return AppLanguage.EN
        return runCatching { AppLanguage.valueOf(stored) }.getOrDefault(AppLanguage.EN)
    }

    private data class RemoteSnapshot(
        val routing: RoutingConfig = RoutingConfig(),
        val alertLevel: AlertLevel = AlertLevel.WARNING,
        val emergencyNumbers: List<EmergencyNumber> = RemoteConfigDefaults.emergencyNumbers,
    )

    private companion object {
        const val TAG = "LanternConfig"
        const val PREFS = "lantern_config"
        const val KEY_LANGUAGE = "language"
        const val KEY_CHOSEN = "language_chosen"
    }
}
