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

    /** Live key used by the console. Its value is a JSON array. */
    const val EMERGENCY_NUMBERS = "emergency_numbers"

    /** Legacy key kept so older console setups keep working. */
    const val EMERGENCY_NUMBERS_JSON = "emergency_numbers_json"
    const val SAFETY_CARDS_JSON = "safety_cards"

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
        // Empty default so the legacy key (and finally the bundled list) can
        // supply numbers when the console has not set the new key yet.
        EMERGENCY_NUMBERS to "",
        EMERGENCY_NUMBERS_JSON to emergencyNumbersJson,
        // Empty means "no remote pack configured": the bundled asset is used.
        SAFETY_CARDS_JSON to "",
    )
}

/**
 * Flexible DTO for both console shapes:
 *
 * Legacy: {"label":"Rescue 1122","number":"1122","description":"..."}
 * Current: {"number":"1122","name":"Emergency Number","region":"Punjab"}
 */
@Serializable
internal data class EmergencyNumberDto(
    val label: String = "",
    val number: String = "",
    val description: String = "",
    val name: String = "",
    val region: String = "",
    val organization: String = "",
) {
    fun toDomain(): EmergencyNumber {
        val resolvedLabel = label.ifBlank { name }.ifBlank { "Emergency" }
        val resolvedDescription = listOf(description, region, organization)
            .filter { it.isNotBlank() }
            .joinToString(" · ")
        return EmergencyNumber(
            label = resolvedLabel,
            number = number.trim(),
            description = resolvedDescription,
        )
    }

    companion object {
        fun fromDomain(number: EmergencyNumber): EmergencyNumberDto = EmergencyNumberDto(
            label = number.label,
            number = number.number,
            description = number.description,
        )
    }
}
