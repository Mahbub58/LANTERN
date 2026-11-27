package com.mahbub.lantern.data.ai

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
internal data class AnswerPayload(
    val cardId: String = "",
    val answer: String = "",
)

@Serializable
internal data class UrgencyPayload(
    val urgency: Int = 3,
)

@Serializable
internal data class DuplicatePayload(
    val groups: List<List<String>> = emptyList(),
)

/**
 * Defensive parsing for model output. Models occasionally wrap JSON in a
 * markdown fence or add a sentence before it, so we extract the first object
 * and validate every field instead of trusting the raw string.
 */
internal object ModelJson {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun answer(raw: String?): AnswerPayload? {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty()) return null
        val objectText = firstJsonObject(text) ?: return null
        return runCatching { json.decodeFromString<AnswerPayload>(objectText) }
            .getOrNull()
            ?.takeIf { it.cardId.isNotBlank() && it.answer.isNotBlank() }
    }

    fun urgency(raw: String?): Int? {
        val text = raw?.trim().orEmpty()
        val objectText = firstJsonObject(text) ?: return null
        return runCatching { json.decodeFromString<UrgencyPayload>(objectText) }
            .getOrNull()
            ?.urgency
            ?.coerceIn(1, 5)
    }

    fun duplicateGroups(raw: String?, knownIds: Set<String>): List<List<String>> {
        val text = raw?.trim().orEmpty()
        val objectText = firstJsonObject(text) ?: return emptyList()
        val payload = runCatching { json.decodeFromString<DuplicatePayload>(objectText) }.getOrNull()
            ?: return emptyList()
        return payload.groups
            .map { group -> group.filter { it in knownIds }.distinct() }
            .filter { it.size >= 2 }
    }

    fun encodeAnswer(cardId: String, answer: String): String =
        json.encodeToString(AnswerPayload.serializer(), AnswerPayload(cardId, answer))

    fun firstJsonObject(text: String): String? {
        val start = text.indexOf('{')
        if (start < 0) return null
        var depth = 0
        var inString = false
        var escaped = false
        for (index in start until text.length) {
            val char = text[index]
            if (inString) {
                when {
                    escaped -> escaped = false
                    char == '\\' -> escaped = true
                    char == '"' -> inString = false
                }
                continue
            }
            when (char) {
                '"' -> inString = true
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return text.substring(start, index + 1)
                }
            }
        }
        return null
    }
}
