package com.softvinax.lantern.testutil

import com.softvinax.lantern.domain.model.SafetyCard
import com.softvinax.lantern.domain.repository.SafetyCardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class InMemorySafetyCardRepository(
    private val cards: List<SafetyCard>,
) : SafetyCardRepository {

    override suspend fun getAll(): List<SafetyCard> = cards

    override suspend fun getById(id: String): SafetyCard? = cards.firstOrNull { it.id == id }

    override val version: Flow<String> = flowOf("test-1.0")

    override suspend fun refresh() = Unit
}
