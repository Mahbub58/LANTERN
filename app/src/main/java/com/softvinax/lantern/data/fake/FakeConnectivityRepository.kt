package com.softvinax.lantern.data.fake

import com.softvinax.lantern.domain.repository.ConnectivityRepository
import com.softvinax.lantern.domain.repository.NetworkSimulator
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Starts offline on purpose: the first-run demo should prove that Lantern works
 * with no signal. The debug panel flips this flag for the online/cloud demo.
 */
@Singleton
class FakeConnectivityRepository @Inject constructor() :
    ConnectivityRepository,
    NetworkSimulator {

    private val onlineFlow = MutableStateFlow(false)

    override val isOnline: Flow<Boolean> = onlineFlow.asStateFlow()

    override fun setSimulatedOnline(online: Boolean) {
        onlineFlow.value = online
    }
}
