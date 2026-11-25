package com.softvinax.lantern.data.fake

import com.softvinax.lantern.domain.model.ReportLocation
import com.softvinax.lantern.domain.repository.LocationRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay

@Singleton
class FakeLocationRepository @Inject constructor() : LocationRepository {

    override suspend fun lastKnownLocation(): ReportLocation {
        delay(300)
        return ReportLocation(
            latitude = 24.8607,
            longitude = 67.0011,
            accuracyMeters = 250f,
            placeLabel = "Demo location, Karachi",
        )
    }
}
