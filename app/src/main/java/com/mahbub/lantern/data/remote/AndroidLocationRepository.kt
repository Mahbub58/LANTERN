package com.mahbub.lantern.data.remote

import android.content.Context
import android.location.Location
import android.location.LocationManager
import com.mahbub.lantern.domain.model.ReportLocation
import com.mahbub.lantern.domain.repository.LocationRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Last known fix from the platform location service. No fused provider or
 * extra dependency is needed, and a missing permission simply returns null so
 * the report can still be sent without a location.
 */
@Singleton
class AndroidLocationRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : LocationRepository {

    override suspend fun lastKnownLocation(): ReportLocation? {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null
        val location = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER,
        ).mapNotNull { provider ->
            runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
        }.maxByOrNull(Location::getTime) ?: return null

        return ReportLocation(
            latitude = location.latitude,
            longitude = location.longitude,
            accuracyMeters = location.accuracy.takeIf { it > 0f },
            placeLabel = null,
        )
    }
}
