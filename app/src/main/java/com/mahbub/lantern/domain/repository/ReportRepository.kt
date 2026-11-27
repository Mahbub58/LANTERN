package com.mahbub.lantern.domain.repository

import com.mahbub.lantern.domain.model.Report
import com.mahbub.lantern.domain.model.ReportDraft
import kotlinx.coroutines.flow.Flow

/**
 * Reports stay on the phone as drafts until [submit] is called. The fake
 * implementation flips QUEUED -> SENT when connectivity returns.
 */
interface ReportRepository {
    fun observeReports(): Flow<List<Report>>
    suspend fun getReport(id: String): Report?
    suspend fun saveDraft(draft: ReportDraft): ReportDraft
    suspend fun getDraft(id: String): ReportDraft?
    suspend fun updateDraft(draft: ReportDraft): ReportDraft
    suspend fun discardDraft(id: String)
    suspend fun submit(draft: ReportDraft): Report
}
