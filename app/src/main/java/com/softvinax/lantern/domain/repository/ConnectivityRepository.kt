package com.softvinax.lantern.domain.repository

import kotlinx.coroutines.flow.Flow

interface ConnectivityRepository {
    val isOnline: Flow<Boolean>
}

/**
 * Demo/debug capability only. Kept as a separate contract so the production
 * connectivity implementation never has to expose a "pretend to be online" API.
 */
interface NetworkSimulator {
    fun setSimulatedOnline(online: Boolean)
}
