package com.softvinax.lantern.domain.usecase

import com.softvinax.lantern.domain.model.Report
import com.softvinax.lantern.domain.model.ReportDraft
import com.softvinax.lantern.domain.repository.LocationRepository
import com.softvinax.lantern.domain.repository.ReportRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SubmitReportUseCase @Inject constructor(
    private val reportRepository: ReportRepository,
) {
    suspend operator fun invoke(draft: ReportDraft): Report =
        reportRepository.submit(draft.sanitized())
}

class ObserveMyReportsUseCase @Inject constructor(
    private val reportRepository: ReportRepository,
) {
    operator fun invoke(): Flow<List<Report>> = reportRepository.observeReports()
}

class ObserveReportUseCase @Inject constructor(
    private val reportRepository: ReportRepository,
) {
    operator fun invoke(reportId: String): Flow<Report?> =
        reportRepository.observeReports().map { reports -> reports.firstOrNull { it.id == reportId } }
}

class SaveReportDraftUseCase @Inject constructor(
    private val reportRepository: ReportRepository,
) {
    suspend operator fun invoke(draft: ReportDraft): ReportDraft =
        reportRepository.saveDraft(draft.sanitized())
}

class GetReportDraftUseCase @Inject constructor(
    private val reportRepository: ReportRepository,
) {
    suspend operator fun invoke(draftId: String): ReportDraft? = reportRepository.getDraft(draftId)
}

class UpdateReportDraftUseCase @Inject constructor(
    private val reportRepository: ReportRepository,
) {
    suspend operator fun invoke(draft: ReportDraft): ReportDraft =
        reportRepository.updateDraft(draft.sanitized())
}

class DiscardReportDraftUseCase @Inject constructor(
    private val reportRepository: ReportRepository,
) {
    suspend operator fun invoke(draftId: String) = reportRepository.discardDraft(draftId)
}

class GetLastKnownLocationUseCase @Inject constructor(
    private val locationRepository: LocationRepository,
) {
    suspend operator fun invoke() = locationRepository.lastKnownLocation()
}
