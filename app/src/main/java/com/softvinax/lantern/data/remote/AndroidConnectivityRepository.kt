package com.softvinax.lantern.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.softvinax.lantern.domain.repository.ConnectivityRepository
import com.softvinax.lantern.domain.repository.NetworkSimulator
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Real device connectivity, plus the debug panel's simulated override.
 *
 * The override is `null` by default, so the app follows the actual network.
 * Toggling "Simulated signal" in the demo panel forces offline/online without
 * touching airplane mode. A production build can bind only
 * [ConnectivityRepository] and never expose [NetworkSimulator].
 */
@Singleton
class AndroidConnectivityRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : ConnectivityRepository, NetworkSimulator {

    private val simulated = MutableStateFlow<Boolean?>(null)

    private val realOnline: Flow<Boolean> = callbackFlow {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (manager == null) {
            trySend(false)
            awaitClose { }
            return@callbackFlow
        }

        fun isOnline(): Boolean {
            val network = manager.activeNetwork ?: return false
            val capabilities = manager.getNetworkCapabilities(network) ?: return false
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        }

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(isOnline())
            }

            override fun onLost(network: Network) {
                trySend(isOnline())
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
            ) {
                trySend(
                    networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED),
                )
            }
        }

        trySend(isOnline())
        runCatching { manager.registerDefaultNetworkCallback(callback) }
        awaitClose { runCatching { manager.unregisterNetworkCallback(callback) } }
    }.distinctUntilChanged()

    override val isOnline: Flow<Boolean> =
        combine(realOnline, simulated) { real, override -> override ?: real }
            .distinctUntilChanged()

    override fun setSimulatedOnline(online: Boolean) {
        simulated.value = online
    }
}
