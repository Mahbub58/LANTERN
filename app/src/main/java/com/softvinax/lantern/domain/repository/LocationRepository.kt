package com.softvinax.lantern.domain.repository

import com.softvinax.lantern.domain.model.ReportLocation

/**
 * Offline-friendly location seam. The fake returns a fixed demo location so the
 * permission flow can be exercised without hardware; Phase 2 can use
 * FusedLocationProvider and cache the last fix.
 */
interface LocationRepository {
    suspend fun lastKnownLocation(): ReportLocation?
}
