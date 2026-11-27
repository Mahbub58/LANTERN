package com.mahbub.lantern.data.remote

import android.content.Context
import com.mahbub.lantern.domain.model.Report
import com.mahbub.lantern.domain.model.SyncState
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json

/**
 * Reports submitted before anonymous auth is possible (for example the very
 * first launch is offline) are kept here. As soon as a uid exists, the report
 * repository writes them to Firestore and removes them from this queue.
 */
@Singleton
class LocalReportQueue @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val _reports = MutableStateFlow(load())
    val reports: StateFlow<List<Report>> = _reports.asStateFlow()

    fun snapshot(): List<Report> = _reports.value

    fun add(report: Report) {
        _reports.update { current -> current.filterNot { it.id == report.id } + report }
        persist()
    }

    fun removeAll(ids: Set<String>) {
        if (ids.isEmpty()) return
        val next = _reports.value.filterNot { it.id in ids }
        if (next.size == _reports.value.size) return
        _reports.value = next
        persist()
    }

    private fun load(): List<Report> = runCatching {
        json.decodeFromString<List<ReportDto>>(prefs.getString(KEY, null) ?: "[]")
            .map { it.toDomain(SyncState.QUEUED) }
    }.getOrDefault(emptyList())

    private fun persist() {
        val dtos = _reports.value.map(ReportDto::fromDomain)
        prefs.edit().putString(KEY, json.encodeToString(dtos)).apply()
    }

    private companion object {
        const val PREFS = "lantern_pending_reports"
        const val KEY = "reports"
    }
}
