package com.softvinax.lantern.data.remote

import com.softvinax.lantern.domain.model.AlertLevel
import com.softvinax.lantern.domain.model.RoutingConfig
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteConfigDefaultsTest {

    @Test
    fun `every required key has a safe default`() {
        val values = RemoteConfigDefaults.values

        assertEquals(
            RoutingConfig.DEFAULT_THRESHOLD.toDouble(),
            values[RemoteConfigDefaults.CONFIDENCE_THRESHOLD],
        )
        assertEquals(true, values[RemoteConfigDefaults.CLOUD_FALLBACK_ENABLED])
        assertEquals(AlertLevel.WARNING.name, values[RemoteConfigDefaults.ALERT_LEVEL])
        assertEquals("1.0.0", values[RemoteConfigDefaults.CARDS_VERSION])
        assertTrue(values[RemoteConfigDefaults.EMERGENCY_NUMBERS_JSON] is String)
    }

    @Test
    fun `emergency numbers json parses to the bundled defaults`() {
        val numbers = Json.decodeFromString(
            ListSerializer(EmergencyNumberDto.serializer()),
            RemoteConfigDefaults.emergencyNumbersJson,
        ).map(EmergencyNumberDto::toDomain)

        assertEquals(4, numbers.size)
        assertEquals("Rescue 1122", numbers.first().label)
        assertEquals("1122", numbers.first().number)
        assertTrue(numbers.all { it.label.isNotBlank() && it.number.isNotBlank() })
    }
}
