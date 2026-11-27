package com.mahbub.lantern.data.remote

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonPrimitive

/**
 * Normalises the `safety_cards` Remote Config value.
 *
 * Remote Config parameter data types (String, Boolean, Number, JSON) do not
 * change how the client SDK returns values: `getString()` always returns the
 * stored text. A JSON parameter whose value was stored as an escaped JSON
 * string can therefore arrive wrapped in quotes; unwrap it once so
 * kotlinx.serialization sees the real object.
 */
internal object RemoteCardJson {

    fun normalize(raw: String?): String {
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isEmpty() || trimmed.equals("null", ignoreCase = true)) return ""
        if (trimmed.length < 2 || !trimmed.startsWith('"') || !trimmed.endsWith('"')) {
            return trimmed
        }
        return runCatching { Json.parseToJsonElement(trimmed).jsonPrimitive.content }
            .getOrDefault(trimmed)
            .trim()
    }

    /** Short, log-safe description of a fetched value. */
    fun describe(raw: String): String = if (raw.isEmpty()) {
        "empty"
    } else {
        "${raw.length} chars, starts=${raw.take(24)}"
    }
}
