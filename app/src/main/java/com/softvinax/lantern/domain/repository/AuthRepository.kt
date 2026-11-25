package com.softvinax.lantern.domain.repository

import kotlinx.coroutines.flow.Flow

/** Anonymous, device-scoped identity. No email/phone is ever required. */
interface AuthRepository {
    val uid: Flow<String?>
    suspend fun ensureAnonymousUid(): String
}
