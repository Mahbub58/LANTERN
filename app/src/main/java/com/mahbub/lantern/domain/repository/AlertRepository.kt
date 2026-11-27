package com.mahbub.lantern.domain.repository

import com.mahbub.lantern.domain.model.Alert
import kotlinx.coroutines.flow.Flow

interface AlertRepository {
    fun observeAlerts(): Flow<List<Alert>>
    suspend fun refresh()
}
