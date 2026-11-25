package com.softvinax.lantern.domain.ai

import javax.inject.Inject

/**
 * Removes direct identifiers before anything is allowed to leave the phone.
 *
 * This is deliberately conservative and regex-based: in Phase 2 the same
 * contract can be backed by an on-device NER model without changing callers.
 * It does not attempt to remove indirect identifiers (e.g. "the only clinic in
 * my village"); the UI copy tells people not to type names or addresses.
 */
class PiiScrubber @Inject constructor() {

    fun scrub(input: String): String {
        if (input.isBlank()) return input
        var text = input

        text = EMAIL.replace(text, PLACEHOLDER_EMAIL)
        text = COORDINATES.replace(text, PLACEHOLDER_LOCATION)
        text = URL.replace(text, PLACEHOLDER_LINK)
        text = CNIC.replace(text, PLACEHOLDER_ID)
        text = HOUSE_OR_PLOT.replace(text, PLACEHOLDER_LOCATION)
        text = STREET_OR_SECTOR.replace(text, PLACEHOLDER_LOCATION)
        text = ADDRESS_AFTER_LABEL.replace(text, "$1$PLACEHOLDER_LOCATION")
        text = NAME_AFTER_LABEL.replace(text, "$1$PLACEHOLDER_NAME")
        text = HONORIFIC_NAME.replace(text, "$1$PLACEHOLDER_NAME")
        text = PHONE.replace(text) { match ->
            val digits = match.value.count { it.isDigit() }
            if (digits >= 8) PLACEHOLDER_PHONE else match.value
        }
        text = LONG_DIGITS.replace(text, PLACEHOLDER_PHONE)

        return text
            .replace(WHITESPACE, " ")
            .trim()
    }

    private companion object {
        const val PLACEHOLDER_PHONE = "[phone]"
        const val PLACEHOLDER_EMAIL = "[email]"
        const val PLACEHOLDER_NAME = "[name]"
        const val PLACEHOLDER_LOCATION = "[location]"
        const val PLACEHOLDER_ID = "[id]"
        const val PLACEHOLDER_LINK = "[link]"

        val EMAIL = Regex("[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}")
        val URL = Regex("https?://\\S+|www\\.\\S+", RegexOption.IGNORE_CASE)
        val COORDINATES = Regex("-?\\d{1,3}\\.\\d{3,}\\s*,\\s*-?\\d{1,3}\\.\\d{3,}")
        val CNIC = Regex("\\b\\d{5}-?\\d{7}-?\\d\\b")
        val LONG_DIGITS = Regex("\\b\\d{10,}\\b")
        val HOUSE_OR_PLOT = Regex(
            "(?i)\\b(?:house|flat|plot|apartment|apt|makaan|ghar)\\s*(?:no\\.?|number)?\\s*[A-Za-z]?-?\\d+[A-Za-z]?\\b",
        )
        val STREET_OR_SECTOR = Regex(
            "(?i)\\b(?:street|st\\.?|road|sector|block|phase|gali|mohalla)\\s*(?:no\\.?\\s*)?[A-Za-z]?\\d+[A-Za-z\\-]*\\b",
        )
        val ADDRESS_AFTER_LABEL = Regex(
            "(?i)\\b(address|pata|live at|rehta hoon|rehti hoon)\\b\\s*(?:is|hai|:)?\\s*[^.!?\\n]{2,80}",
        )
        val NAME_AFTER_LABEL = Regex(
            "(?i)\\b(my name is|mera naam|mera nam|naam)\\b\\s*([\\p{L}][\\p{L}\\s]{1,40})",
        )
        val HONORIFIC_NAME = Regex(
            "(?i)\\b(mr\\.?|mrs\\.?|ms\\.?|dr\\.?|prof\\.?)\\s+[\\p{L}][\\p{L}.]*",
        )
        val PHONE = Regex("\\+?\\d[\\d\\s()\\-]{6,}\\d")
        val WHITESPACE = Regex("\\s+")
    }
}
