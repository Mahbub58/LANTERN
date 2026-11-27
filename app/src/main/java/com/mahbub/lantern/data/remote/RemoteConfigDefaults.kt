package com.mahbub.lantern.data.remote

import com.mahbub.lantern.domain.model.AlertLevel
import com.mahbub.lantern.domain.model.EmergencyNumber
import com.mahbub.lantern.domain.model.RoutingConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Safe in-app defaults. They are also pushed with `setDefaultsAsync` so the app
 * behaves correctly before the first Remote Config fetch and while offline.
 */
object RemoteConfigDefaults {

    const val CONFIDENCE_THRESHOLD = "confidence_threshold"
    const val CLOUD_FALLBACK_ENABLED = "cloud_fallback_enabled"
    const val ALERT_LEVEL = "alert_level"
    const val EMERGENCY_NUMBERS_JSON = "emergency_numbers_json"
    const val CARDS_VERSION = "cards_version"

    val emergencyNumbers: List<EmergencyNumber> = listOf(
        EmergencyNumber(
            label = "Rescue 1122",
            number = "1122",
            description = "Rescue and ambulance",
        ),
        EmergencyNumber(
            label = "Edhi Ambulance",
            number = "115",
            description = "Nationwide ambulance service",
        ),
        EmergencyNumber(
            label = "Police",
            number = "15",
            description = "Police emergency",
        ),
        EmergencyNumber(
            label = "NDMA Helpline",
            number = "1135",
            description = "National Disaster Management Authority",
        ),
    )

    private val json = Json

    val emergencyNumbersJson: String = json.encodeToString(
        ListSerializer(EmergencyNumberDto.serializer()),
        emergencyNumbers.map(EmergencyNumberDto::fromDomain),
    )

    val values: Map<String, Any> = mapOf(
        CONFIDENCE_THRESHOLD to RoutingConfig.DEFAULT_THRESHOLD.toDouble(),
        CLOUD_FALLBACK_ENABLED to true,
        ALERT_LEVEL to AlertLevel.WARNING.name,
        EMERGENCY_NUMBERS_JSON to emergencyNumbersJson,
        CARDS_VERSION to "1.0.0",
    )
}

@Serializable
internal data class EmergencyNumberDto(
    val label: String = "",
    val number: String = "",
    val description: String = "",
) {
    fun toDomain(): EmergencyNumber = EmergencyNumber(
        label = label,
        number = number,
        description = description,
    )

    companion object {
        fun fromDomain(number: EmergencyNumber): EmergencyNumberDto = EmergencyNumberDto(
            label = number.label,
            number = number.number,
            description = number.description,
        )
    }
}
