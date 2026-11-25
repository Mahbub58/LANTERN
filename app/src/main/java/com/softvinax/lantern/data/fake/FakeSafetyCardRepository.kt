package com.softvinax.lantern.data.fake

import android.content.Context
import com.softvinax.lantern.data.asset.SafetyCardBundleDto
import com.softvinax.lantern.data.mapper.toDomainValidated
import com.softvinax.lantern.domain.model.SafetyCard
import com.softvinax.lantern.domain.repository.SafetyCardRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

@Singleton
class FakeSafetyCardRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : SafetyCardRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()
    private val versionFlow = MutableStateFlow("bundled")
    private var cache: List<SafetyCard>? = null

    override val version: Flow<String> = versionFlow

    override suspend fun getAll(): List<SafetyCard> = mutex.withLock {
        cache ?: load().also { cache = it }
    }

    override suspend fun getById(id: String): SafetyCard? = getAll().firstOrNull { it.id == id }

    override suspend fun refresh() {
        mutex.withLock {
            cache = null
            cache = load()
        }
    }

    private fun load(): List<SafetyCard> {
        val raw = context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
        val bundle = json.decodeFromString<SafetyCardBundleDto>(raw)
        versionFlow.value = bundle.version
        return bundle.cards.toDomainValidated()
    }

    private companion object {
        const val ASSET_NAME = "safety_cards.json"
    }
}
