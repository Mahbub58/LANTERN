package com.mahbub.lantern.domain.usecase

import com.mahbub.lantern.domain.model.Alert
import com.mahbub.lantern.domain.repository.AlertRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveAlertsUseCase @Inject constructor(
    private val alertRepository: AlertRepository,
) {
    operator fun invoke(): Flow<List<Alert>> = alertRepository.observeAlerts()
}

class RefreshAlertsUseCase @Inject constructor(
    private val alertRepository: AlertRepository,
) {
    suspend operator fun invoke() = alertRepository.refresh()
}
