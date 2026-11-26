package com.softvinax.lantern.domain.model

enum class InferenceSource {
    ON_DEVICE,
    CLOUD,
    CARD_TEXT,
}

enum class EmergencyType(val label: String) {
    FLOOD("Flood"),
    EARTHQUAKE("Earthquake"),
    LANDSLIDE("Landslide"),
    OTHER("Other"),
}

enum class Need(val label: String) {
    RESCUE("Rescue"),
    SHELTER("Shelter"),
    WATER("Drinking water"),
    FOOD("Food"),
    MEDICAL("Medical help"),
    CLOTHING("Clothing"),
    POWER("Power / charging"),
    OTHER("Other"),
}

enum class SyncState {
    DRAFT,
    QUEUED,
    SENT,
}

enum class ReportStatus {
    NEW,
    ASSIGNED,
    RESOLVED,
}

data class ReportLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float? = null,
    val placeLabel: String? = null,
)

data class ReportDraft(
    val id: String,
    val description: String,
    val language: AppLanguage,
    val type: EmergencyType = EmergencyType.OTHER,
    val urgency: Int = 3,
    val peopleCount: Int = 1,
    val needs: Set<Need> = emptySet(),
    val placeDescription: String = "",
    val location: ReportLocation? = null,
    val createdAt: Long = 0L,
) {
    /** Clamped 1..5 so UI sliders can never produce an invalid report. */
    fun sanitized(): ReportDraft = copy(
        urgency = urgency.coerceIn(1, 5),
        peopleCount = peopleCount.coerceIn(1, 99),
        description = description.trim(),
        placeDescription = placeDescription.trim(),
    )
}

data class Report(
    val id: String,
    val description: String,
    val language: AppLanguage,
    val type: EmergencyType,
    val urgency: Int,
    val peopleCount: Int,
    val needs: Set<Need>,
    val placeDescription: String,
    val location: ReportLocation?,
    val syncState: SyncState,
    val status: ReportStatus,
    val createdAt: Long,
    val updatedAt: Long,
    val sentAt: Long? = null,
) {
    val isPendingSync: Boolean get() = syncState == SyncState.QUEUED
}

data class Answer(
    val card: SafetyCard?,
    val text: String,
    val source: InferenceSource,
    val isNoMatch: Boolean = false,
    val isDeeper: Boolean = false,
    val retrievalScore: Float = 0f,
    val emergencyNumbers: List<EmergencyNumber> = emptyList(),
)

data class RoutingConfig(
    val confidenceThreshold: Float = DEFAULT_THRESHOLD,
    val cloudFallbackEnabled: Boolean = true,
) {
    fun sanitized(): RoutingConfig = copy(
        confidenceThreshold = confidenceThreshold.coerceIn(0f, 1f),
    )

    companion object {
        const val DEFAULT_THRESHOLD = 0.55f

        /** Below this, we treat retrieval as "no vetted card matched". */
        const val MINIMUM_MATCH_SCORE = 0.18f
    }
}

data class EmergencyNumber(
    val label: String,
    val number: String,
    val description: String = "",
)
