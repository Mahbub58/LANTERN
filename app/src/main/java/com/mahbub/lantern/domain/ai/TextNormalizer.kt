package com.mahbub.lantern.domain.ai

/**
 * Small, deterministic text helpers shared by retrieval and answer validation.
 * No Android dependencies, no model required: this must behave identically in a
 * unit test and on a low-end phone.
 */
internal object TextNormalizer {

    private val PUNCTUATION = Regex("[^\\p{L}\\p{N}\\s]")
    private val DIACRITICS = Regex("[\\u064B-\\u065F\\u0670]")
    private val WHITESPACE = Regex("\\s+")

    private val STOP_WORDS = setOf(
        // English
        "a", "an", "the", "is", "are", "was", "were", "be", "been", "being",
        "am", "i", "we", "you", "he", "she", "it", "they", "my", "our", "your",
        "his", "her", "its", "their", "this", "that", "these", "those", "to",
        "of", "in", "on", "at", "for", "from", "with", "and", "or", "but", "if",
        "as", "by", "so", "do", "does", "did", "have", "has", "had", "will",
        "would", "can", "could", "should", "may", "might", "must", "not", "no",
        "yes", "please", "help", "now", "then", "there", "here", "when", "what",
        "how", "why", "who", "which", "me", "us", "them", "also", "very", "just",
        // Roman Urdu
        "ka", "ki", "ke", "ko", "se", "mein", "main", "me", "hai", "hain", "ha",
        "aur", "ya", "par", "ek", "yeh", "ye", "woh", "wo", "nahi", "nahin",
        "kya", "to", "bhi", "ho", "hua", "hui", "gaya", "gayi", "gaye", "raha",
        "rahi", "rahe", "abhi", "ab", "mera", "meri", "mere", "hamara", "hamari",
        "aap", "tum", "hum", "koi", "kuch", "bahut", "bohat", "thoda", "please",
        // Urdu
        "کا", "کی", "کے", "کو", "سے", "میں", "ہے", "ہیں", "اور", "یا", "پر",
        "ایک", "یہ", "وہ", "نہیں", "کیا", "تو", "بھی", "ہو", "ہوا", "ہوئی",
        "گیا", "گئی", "رہا", "رہی", "اب", "میرا", "میری", "میرے", "ہمارا",
        "ہماری", "آپ", "تم", "ہم", "کوئی", "کچھ", "بہت", "تھوڑا",
    )

    fun normalize(input: String): String = input
        .lowercase()
        .replace(DIACRITICS, "")
        .replace(PUNCTUATION, " ")
        .replace(WHITESPACE, " ")
        .trim()

    fun tokens(input: String): List<String> =
        normalize(input).split(' ').filter { it.isNotBlank() }

    fun significantTokens(input: String): List<String> =
        tokens(input).filter { it.length > 1 && it !in STOP_WORDS }.distinct()

    fun isStopWord(token: String): Boolean = token in STOP_WORDS

    /** Word-boundary-ish containment that works for Latin and Arabic scripts. */
    fun containsPhrase(haystackNormalized: String, needleNormalized: String): Boolean {
        if (needleNormalized.isBlank()) return false
        return " $haystackNormalized ".contains(" $needleNormalized ")
    }
}
