package com.softvinax.lantern.domain.usecase

import com.softvinax.lantern.domain.model.AppLanguage
import com.softvinax.lantern.domain.model.EmergencyType
import com.softvinax.lantern.domain.model.Need
import com.softvinax.lantern.domain.model.Report
import com.softvinax.lantern.domain.model.ReportDraft
import com.softvinax.lantern.domain.model.ReportLocation
import com.softvinax.lantern.domain.repository.LocationRepository
import com.softvinax.lantern.domain.repository.ReportRepository
import java.util.UUID
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


/**
 * Builds a guided report draft from the three-step form: type, people, needs
 * and place. The description is a structured English summary so responders can
 * read the report even if the UI language is Urdu or Roman Urdu.
 */
class BuildReportDraftUseCase @Inject constructor() {

    operator fun invoke(
        type: EmergencyType,
        peopleCount: Int,
        needs: Set<Need>,
        place: String,
        location: ReportLocation?,
        language: AppLanguage,
        now: Long = System.currentTimeMillis(),
    ): ReportDraft {
        val safeNeeds = needs.toSet()
        val safePlace = place.trim()
        val description = buildString {
            append(type.label)
            append(". Needs: ")
            append(if (safeNeeds.isEmpty()) "none" else safeNeeds.joinToString { it.label })
            if (safePlace.isNotEmpty()) append(". Place: ").append(safePlace)
        }
        return ReportDraft(
            id = UUID.randomUUID().toString(),
            description = description,
            language = language,
            type = type,
            urgency = urgencyFor(safeNeeds),
            peopleCount = peopleCount.coerceIn(1, 99),
            needs = safeNeeds,
            placeDescription = safePlace,
            location = location,
            createdAt = now,
        ).sanitized()
    }

    fun urgencyFor(needs: Set<Need>): Int = when {
        needs.any { it == Need.RESCUE || it == Need.MEDICAL } -> 5
        else -> 3
    }
}
