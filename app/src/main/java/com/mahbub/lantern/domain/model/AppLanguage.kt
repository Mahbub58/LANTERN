package com.mahbub.lantern.domain.model

/**
 * Languages the app ships with. [UR] is right-to-left; [ROMAN_UR] is Latin-script
 * Urdu written the way most people type on WhatsApp.
 */
enum class AppLanguage(val code: String, val isRtl: Boolean) {
    EN("en", false),
    UR("ur", true),
    ROMAN_UR("ur-Latn", false);

    companion object {
        fun fromCode(code: String?): AppLanguage =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: EN
    }
}
