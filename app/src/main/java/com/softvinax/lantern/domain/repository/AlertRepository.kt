package com.softvinax.lantern.domain.repository

import com.softvinax.lantern.domain.model.Alert
import kotlinx.coroutines.flow.Flow

interface AlertRepository {
    fun observeAlerts(): Flow<List<Alert>>
    suspend fun refresh()
}
