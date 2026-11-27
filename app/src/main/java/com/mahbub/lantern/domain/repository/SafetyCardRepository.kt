package com.mahbub.lantern.domain.repository

import com.mahbub.lantern.domain.model.SafetyCard
import kotlinx.coroutines.flow.Flow

/**
 * Vetted, bundled safety guidance. Phase 2 swaps the fake asset loader for a
 * versioned download + local cache, without changing callers.
 */
interface SafetyCardRepository {
    suspend fun getAll(): List<SafetyCard>
    suspend fun getById(id: String): SafetyCard?
    val version: Flow<String>
    suspend fun refresh()
}
