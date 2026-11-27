package com.mahbub.lantern.domain.usecase

import com.mahbub.lantern.domain.ai.NanoStatus
import com.mahbub.lantern.domain.ai.OnDeviceInference
import com.mahbub.lantern.domain.model.AlertLevel
import com.mahbub.lantern.domain.model.AppLanguage
import com.mahbub.lantern.domain.model.EmergencyNumber
import com.mahbub.lantern.domain.model.InferenceSource
import com.mahbub.lantern.domain.model.RoutingConfig
import com.mahbub.lantern.domain.repository.AuthRepository
import com.mahbub.lantern.domain.repository.ConfigRepository
import com.mahbub.lantern.domain.repository.ConnectivityRepository
import com.mahbub.lantern.domain.repository.NetworkSimulator
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveConnectivityUseCase @Inject constructor(
    private val connectivityRepository: ConnectivityRepository,
) {
    operator fun invoke(): Flow<Boolean> = connectivityRepository.isOnline
}

class ObserveLanguageUseCase @Inject constructor(
    private val configRepository: ConfigRepository,
) {
    operator fun invoke(): Flow<AppLanguage> = configRepository.language
}

class IsLanguageChosenUseCase @Inject constructor(
    private val configRepository: ConfigRepository,
) {
    operator fun invoke(): Flow<Boolean> = configRepository.languageChosen
}

class SetLanguageUseCase @Inject constructor(
    private val configRepository: ConfigRepository,
) {
    suspend operator fun invoke(language: AppLanguage) = configRepository.setLanguage(language)
}

class ObserveRoutingConfigUseCase @Inject constructor(
    private val configRepository: ConfigRepository,
) {
    operator fun invoke(): Flow<RoutingConfig> = configRepository.routingConfig
}

class SetRoutingConfigUseCase @Inject constructor(
    private val configRepository: ConfigRepository,
) {
    suspend operator fun invoke(config: RoutingConfig) =
        configRepository.setRoutingConfig(config.sanitized())
}

class ObserveAlertLevelUseCase @Inject constructor(
    private val configRepository: ConfigRepository,
) {
    operator fun invoke(): Flow<AlertLevel> = configRepository.alertLevel
}

class SetAlertLevelUseCase @Inject constructor(
    private val configRepository: ConfigRepository,
) {
    suspend operator fun invoke(level: AlertLevel) = configRepository.setAlertLevel(level)
}

class GetEmergencyNumbersUseCase @Inject constructor(
    private val configRepository: ConfigRepository,
) {
    operator fun invoke(): Flow<List<EmergencyNumber>> = configRepository.emergencyNumbers
}

class ObserveCardsVersionUseCase @Inject constructor(
    private val configRepository: ConfigRepository,
) {
    operator fun invoke(): Flow<String> = configRepository.cardsVersion
}

class ObserveLastInferenceSourceUseCase @Inject constructor(
    private val configRepository: ConfigRepository,
) {
    operator fun invoke(): Flow<InferenceSource?> = configRepository.lastInferenceSource
}

class ObserveNanoStatusUseCase @Inject constructor(
    private val onDeviceInference: OnDeviceInference,
) {
    operator fun invoke(): Flow<NanoStatus> = onDeviceInference.status
}

/**
 * Matching is Gemini Nano only, so the Ask screen must confirm Nano is ready
 * before opening the answer page.
 */
class IsNanoReadyUseCase @Inject constructor(
    private val onDeviceInference: OnDeviceInference,
) {
    suspend operator fun invoke(): Boolean = onDeviceInference.isAvailable()
}

class SetSimulatedOnlineUseCase @Inject constructor(
    private val networkSimulator: NetworkSimulator,
) {
    operator fun invoke(online: Boolean) = networkSimulator.setSimulatedOnline(online)
}

class GetAnonymousUidUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(): String = authRepository.ensureAnonymousUid()
}
