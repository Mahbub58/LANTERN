package com.softvinax.lantern.domain.model

data class LocalizedText(
    val en: String = "",
    val ur: String = "",
    val roman: String = "",
) {
    fun forLanguage(language: AppLanguage): String = when (language) {
        AppLanguage.EN -> en.ifBlank { roman.ifBlank { ur } }
        AppLanguage.UR -> ur.ifBlank { roman.ifBlank { en } }
        AppLanguage.ROMAN_UR -> roman.ifBlank { en.ifBlank { ur } }
    }

    fun isBlank(): Boolean = en.isBlank() && ur.isBlank() && roman.isBlank()
}

data class LocalizedList(
    val en: List<String> = emptyList(),
    val ur: List<String> = emptyList(),
    val roman: List<String> = emptyList(),
) {
    fun forLanguage(language: AppLanguage): List<String> = when (language) {
        AppLanguage.EN -> en.ifEmpty { roman.ifEmpty { ur } }
        AppLanguage.UR -> ur.ifEmpty { roman.ifEmpty { en } }
        AppLanguage.ROMAN_UR -> roman.ifEmpty { en.ifEmpty { ur } }
    }

    fun all(): List<String> = (en + ur + roman).filter { it.isNotBlank() }
}

data class SafetyCard(
    val id: String,
    val title: LocalizedText,
    val steps: LocalizedList,
    val never: LocalizedList,
    /** Always "TO VERIFY" until a human checks the card against public guidance. */
    val source: String,
    val keywords: LocalizedList,
) {
    /** Text the on-device model is allowed to draw from. */
    fun groundingText(language: AppLanguage): String = buildString {
        append(title.forLanguage(language))
        steps.forLanguage(language).forEach { append(' ').append(it) }
        never.forLanguage(language).forEach { append(' ').append(it) }
    }

    fun allKeywords(): List<String> = keywords.all()
}
