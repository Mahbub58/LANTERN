package com.softvinax.lantern.data.remote

import android.content.Context
import com.softvinax.lantern.domain.model.ReportDraft
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

/**
 * Drafts are local-only. They never touch Firestore and never leave the phone
 * until the person presses Send.
 */
@Singleton
class ReportDraftStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()

    suspend fun save(draft: ReportDraft) = mutex.withLock {
        val all = read().associateBy { it.id }.toMutableMap()
        all[draft.id] = ReportDraftDto.fromDomain(draft)
        write(all.values)
    }

    suspend fun get(id: String): ReportDraft? = mutex.withLock {
        read().firstOrNull { it.id == id }?.toDomain()
    }

    suspend fun discard(id: String) = mutex.withLock {
        write(read().filterNot { it.id == id })
    }

    private fun read(): List<ReportDraftDto> = runCatching {
        json.decodeFromString<List<ReportDraftDto>>(prefs.getString(KEY, null) ?: "[]")
    }.getOrDefault(emptyList())

    private fun write(drafts: Collection<ReportDraftDto>) {
        prefs.edit().putString(KEY, json.encodeToString(drafts.toList())).apply()
    }

    private companion object {
        const val PREFS = "lantern_drafts"
        const val KEY = "drafts"
    }
}
