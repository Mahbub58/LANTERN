package com.mahbub.lantern.data.remote

import com.mahbub.lantern.domain.model.AppLanguage
import com.mahbub.lantern.domain.model.EmergencyType
import com.mahbub.lantern.domain.model.Need
import com.mahbub.lantern.domain.model.Report
import com.mahbub.lantern.domain.model.ReportDraft
import com.mahbub.lantern.domain.model.ReportLocation
import com.mahbub.lantern.domain.model.ReportStatus
import com.mahbub.lantern.domain.model.SyncState
import kotlinx.serialization.Serializable

/**
 * Firestore and local-cache representation of a report.
 *
 * `syncState` is deliberately not stored: it is derived from Firestore snapshot
 * metadata (`hasPendingWrites`), which is the only honest source of truth for
 * whether the write has reached the server.
 */
@Serializable
internal data class ReportDto(
    val id: String,
    val description: String,
    val language: String,
    val type: String,
    val urgency: Int,
    val peopleCount: Int,
    val needs: List<String> = emptyList(),
    val placeDescription: String = "",
    val location: LocationDto? = null,
    val status: String = ReportStatus.NEW.name,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val sentAt: Long? = null,
) {
    fun toDomain(syncState: SyncState): Report = Report(
        id = id,
        description = description,
        language = enumOrDefault(language, AppLanguage.EN),
        type = enumOrDefault(type, EmergencyType.OTHER),
        urgency = urgency.coerceIn(1, 5),
        peopleCount = peopleCount.coerceIn(1, 99),
        needs = needs.mapNotNull { runCatching { Need.valueOf(it) }.getOrNull() }.toSet(),
        placeDescription = placeDescription,
        location = location?.toDomain(),
        syncState = syncState,
        status = enumOrDefault(status, ReportStatus.NEW),
        createdAt = createdAt,
        updatedAt = updatedAt,
        sentAt = sentAt,
    )

    /** Firestore document body. The uid is what the security rules check. */
    fun toFirestore(uid: String): Map<String, Any?> = buildMap {
        put(FIELD_UID, uid)
        put(FIELD_DESCRIPTION, description)
        put(FIELD_LANGUAGE, language)
        put(FIELD_TYPE, type)
        put(FIELD_URGENCY, urgency.coerceIn(1, 5))
        put(FIELD_PEOPLE, peopleCount.coerceIn(1, 99))
        put(FIELD_NEEDS, needs)
        put(FIELD_PLACE, placeDescription)
        put(FIELD_LOCATION, location?.toFirestore())
        put(FIELD_STATUS, status)
        put(FIELD_CREATED_AT, createdAt)
        put(FIELD_UPDATED_AT, updatedAt)
        put(FIELD_SENT_AT, sentAt)
    }

    companion object {
        fun fromDomain(report: Report): ReportDto = ReportDto(
            id = report.id,
            description = report.description,
            language = report.language.name,
            type = report.type.name,
            urgency = report.urgency,
            peopleCount = report.peopleCount,
            needs = report.needs.map { it.name },
            placeDescription = report.placeDescription,
            location = report.location?.let(LocationDto::fromDomain),
            status = report.status.name,
            createdAt = report.createdAt,
            updatedAt = report.updatedAt,
            sentAt = report.sentAt,
        )
    }
}

@Serializable
internal data class LocationDto(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float? = null,
    val placeLabel: String? = null,
) {
    fun toDomain(): ReportLocation = ReportLocation(
        latitude = latitude,
        longitude = longitude,
        accuracyMeters = accuracyMeters,
        placeLabel = placeLabel,
    )

    fun toFirestore(): Map<String, Any?> = buildMap {
        put("latitude", latitude)
        put("longitude", longitude)
        put("accuracyMeters", accuracyMeters)
        put("placeLabel", placeLabel)
    }

    companion object {
        fun fromDomain(location: ReportLocation): LocationDto = LocationDto(
            latitude = location.latitude,
            longitude = location.longitude,
            accuracyMeters = location.accuracyMeters,
            placeLabel = location.placeLabel,
        )
    }
}

@Serializable
internal data class ReportDraftDto(
    val id: String,
    val description: String,
    val language: String,
    val type: String,
    val urgency: Int,
    val peopleCount: Int,
    val needs: List<String> = emptyList(),
    val placeDescription: String = "",
    val location: LocationDto? = null,
    val createdAt: Long = 0L,
) {
    fun toDomain(): ReportDraft = ReportDraft(
        id = id,
        description = description,
        language = enumOrDefault(language, AppLanguage.EN),
        type = enumOrDefault(type, EmergencyType.OTHER),
        urgency = urgency.coerceIn(1, 5),
        peopleCount = peopleCount.coerceIn(1, 99),
        needs = needs.mapNotNull { runCatching { Need.valueOf(it) }.getOrNull() }.toSet(),
        placeDescription = placeDescription,
        location = location?.toDomain(),
        createdAt = createdAt,
    )

    companion object {
        fun fromDomain(draft: ReportDraft): ReportDraftDto = ReportDraftDto(
            id = draft.id,
            description = draft.description,
            language = draft.language.name,
            type = draft.type.name,
            urgency = draft.urgency,
            peopleCount = draft.peopleCount,
            needs = draft.needs.map { it.name },
            placeDescription = draft.placeDescription,
            location = draft.location?.let(LocationDto::fromDomain),
            createdAt = draft.createdAt,
        )
    }
}

private inline fun <reified T : Enum<T>> enumOrDefault(value: String, fallback: T): T =
    runCatching { enumValueOf<T>(value) }.getOrDefault(fallback)

internal const val FIELD_UID = "uid"
internal const val FIELD_DESCRIPTION = "description"
internal const val FIELD_LANGUAGE = "language"
internal const val FIELD_TYPE = "type"
internal const val FIELD_URGENCY = "urgency"
internal const val FIELD_PEOPLE = "peopleCount"
internal const val FIELD_NEEDS = "needs"
internal const val FIELD_PLACE = "placeDescription"
internal const val FIELD_LOCATION = "location"
internal const val FIELD_STATUS = "status"
internal const val FIELD_CREATED_AT = "createdAt"
internal const val FIELD_UPDATED_AT = "updatedAt"
internal const val FIELD_SENT_AT = "sentAt"

internal fun Map<String, Any?>.toReportDto(id: String): ReportDto? {
    val description = this[FIELD_DESCRIPTION] as? String ?: return null
    return ReportDto(
        id = id,
        description = description,
        language = this[FIELD_LANGUAGE] as? String ?: AppLanguage.EN.name,
        type = this[FIELD_TYPE] as? String ?: EmergencyType.OTHER.name,
        urgency = (this[FIELD_URGENCY] as? Number)?.toInt() ?: 3,
        peopleCount = (this[FIELD_PEOPLE] as? Number)?.toInt() ?: 1,
        needs = (this[FIELD_NEEDS] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
        placeDescription = this[FIELD_PLACE] as? String ?: "",
        location = (this[FIELD_LOCATION] as? Map<*, *>)?.let { map ->
            val latitude = (map["latitude"] as? Number)?.toDouble() ?: return@let null
            val longitude = (map["longitude"] as? Number)?.toDouble() ?: return@let null
            LocationDto(
                latitude = latitude,
                longitude = longitude,
                accuracyMeters = (map["accuracyMeters"] as? Number)?.toFloat(),
                placeLabel = map["placeLabel"] as? String,
            )
        },
        status = this[FIELD_STATUS] as? String ?: ReportStatus.NEW.name,
        createdAt = (this[FIELD_CREATED_AT] as? Number)?.toLong() ?: 0L,
        updatedAt = (this[FIELD_UPDATED_AT] as? Number)?.toLong() ?: 0L,
        sentAt = (this[FIELD_SENT_AT] as? Number)?.toLong(),
    )
}
