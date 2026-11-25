package com.softvinax.lantern.domain.repository

import com.softvinax.lantern.domain.model.AlertLevel
import com.softvinax.lantern.domain.model.AppLanguage
import com.softvinax.lantern.domain.model.EmergencyNumber
import com.softvinax.lantern.domain.model.InferenceSource
import com.softvinax.lantern.domain.model.RoutingConfig
import kotlinx.coroutines.flow.Flow

/**
 * App-level configuration. The fake keeps everything in memory; Phase 2 can back
 * this with DataStore + Remote Config.
 */
interface ConfigRepository {
    val language: Flow<AppLanguage>
    val languageChosen: Flow<Boolean>
    val routingConfig: Flow<RoutingConfig>
    val alertLevel: Flow<AlertLevel>
    val emergencyNumbers: Flow<List<EmergencyNumber>>
    val cardsVersion: Flow<String>
    val lastInferenceSource: Flow<InferenceSource?>

    suspend fun setLanguage(language: AppLanguage)
    suspend fun setRoutingConfig(config: RoutingConfig)
    suspend fun setAlertLevel(level: AlertLevel)
    fun recordInference(source: InferenceSource)
}
