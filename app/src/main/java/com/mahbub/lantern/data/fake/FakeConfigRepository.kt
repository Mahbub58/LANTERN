package com.mahbub.lantern.data.fake

import com.mahbub.lantern.domain.model.AlertLevel
import com.mahbub.lantern.domain.model.AppLanguage
import com.mahbub.lantern.domain.model.EmergencyNumber
import com.mahbub.lantern.domain.model.InferenceSource
import com.mahbub.lantern.domain.model.RoutingConfig
import com.mahbub.lantern.domain.repository.ConfigRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class FakeConfigRepository @Inject constructor() : ConfigRepository {

    private val languageFlow = MutableStateFlow(AppLanguage.EN)
    private val languageChosenFlow = MutableStateFlow(false)
    private val routingConfigFlow = MutableStateFlow(RoutingConfig())
    private val alertLevelFlow = MutableStateFlow(AlertLevel.WARNING)
    private val lastInferenceFlow = MutableStateFlow<InferenceSource?>(null)
    private val cardsVersionFlow = MutableStateFlow("1.0.0-demo (bundled)")
    private val emergencyNumbersFlow = MutableStateFlow(
        listOf(
            EmergencyNumber(
                label = "Rescue 1122",
                number = "1122",
                description = "Rescue and ambulance",
            ),
            EmergencyNumber(
                label = "Edhi Ambulance",
                number = "115",
                description = "Nationwide ambulance service",
            ),
            EmergencyNumber(
                label = "Police",
                number = "15",
                description = "Police emergency",
            ),
            EmergencyNumber(
                label = "NDMA Helpline",
                number = "1135",
                description = "National Disaster Management Authority",
            ),
        ),
    )

    override val language: Flow<AppLanguage> = languageFlow.asStateFlow()
    override val languageChosen: Flow<Boolean> = languageChosenFlow.asStateFlow()
    override val routingConfig: Flow<RoutingConfig> = routingConfigFlow.asStateFlow()
    override val alertLevel: Flow<AlertLevel> = alertLevelFlow.asStateFlow()
    override val emergencyNumbers: Flow<List<EmergencyNumber>> = emergencyNumbersFlow.asStateFlow()
    override val cardsVersion: Flow<String> = cardsVersionFlow.asStateFlow()
    override val lastInferenceSource: Flow<InferenceSource?> = lastInferenceFlow.asStateFlow()

    override suspend fun setLanguage(language: AppLanguage) {
        languageFlow.value = language
        languageChosenFlow.value = true
    }

    override suspend fun setRoutingConfig(config: RoutingConfig) {
        routingConfigFlow.value = config.sanitized()
    }

    override suspend fun setAlertLevel(level: AlertLevel) {
        alertLevelFlow.value = level
    }

    override fun recordInference(source: InferenceSource) {
        lastInferenceFlow.value = source
    }
}
