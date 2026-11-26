package com.softvinax.lantern.domain.usecase

import com.softvinax.lantern.domain.model.AppLanguage
import com.softvinax.lantern.domain.model.EmergencyType
import com.softvinax.lantern.domain.model.Need
import com.softvinax.lantern.domain.model.ReportDraft
import com.softvinax.lantern.domain.model.ReportLocation
import java.util.UUID
import javax.inject.Inject

/**
 * Turns free text into editable structured fields using deterministic local
 * rules. No model call, no network, works on the cheapest phone. The result is
 * always a *draft* the person can correct on the review screen.
 */
class StructureReportUseCase @Inject constructor() {

    operator fun invoke(
        description: String,
        language: AppLanguage,
        location: ReportLocation? = null,
        now: Long = System.currentTimeMillis(),
    ): ReportDraft {
        val text = description.trim()
        val normalized = text.lowercase()

        return ReportDraft(
            id = UUID.randomUUID().toString(),
            description = text,
            language = language,
            type = detectType(normalized),
            urgency = detectUrgency(normalized),
            peopleCount = detectPeople(normalized),
            needs = detectNeeds(normalized),
            placeDescription = location?.placeLabel.orEmpty(),
            location = location,
            createdAt = now,
        ).sanitized()
    }

    private fun detectType(normalized: String): EmergencyType {
        val floodHits = FLOOD_TERMS.count { normalized.contains(it) }
        val quakeHits = EARTHQUAKE_TERMS.count { normalized.contains(it) }
        val landslideHits = LANDSLIDE_TERMS.count { normalized.contains(it) }
        return when {
            floodHits == 0 && quakeHits == 0 && landslideHits == 0 -> EmergencyType.OTHER
            floodHits >= quakeHits && floodHits >= landslideHits -> EmergencyType.FLOOD
            quakeHits >= landslideHits -> EmergencyType.EARTHQUAKE
            else -> EmergencyType.LANDSLIDE
        }
    }

    private fun detectUrgency(normalized: String): Int = when {
        CRITICAL_TERMS.any { normalized.contains(it) } -> 5
        HIGH_TERMS.any { normalized.contains(it) } -> 4
        LOW_TERMS.any { normalized.contains(it) } -> 2
        else -> 3
    }

    private fun detectPeople(normalized: String): Int {
        DIGIT_PEOPLE.find(normalized)?.let { return it.groupValues[1].toIntOrNull() ?: 1 }
        WORD_PEOPLE.find(normalized)?.let { match ->
            return WORD_NUMBERS[match.groupValues[1]] ?: 1
        }
        FAMILY_OF.find(normalized)?.let { return it.groupValues[1].toIntOrNull() ?: 1 }
        if (FAMILY_TERMS.any { normalized.contains(it) }) return 4
        return 1
    }

    private fun detectNeeds(normalized: String): Set<Need> = buildSet {
        if (containsAny(normalized, "water", "pani", "peene", "پانی", "پینے")) add(Need.WATER)
        if (containsAny(normalized, "food", "khana", "کھانا", "bhook", "bhookh")) add(Need.FOOD)
        if (containsAny(normalized, "shelter", "tent", "panaah", "پناہ", "chhat", "roof")) add(Need.SHELTER)
        if (containsAny(normalized, "medical", "doctor", "zakhmi", "injured", "dawai", "clinic", "زخمی", "ڈاکٹر")) {
            add(Need.MEDICAL)
        }
        if (containsAny(normalized, "rescue", "trapped", "phansa", "phansi", "bachao", "بچاؤ", "پھنس")) {
            add(Need.RESCUE)
        }
        if (containsAny(normalized, "clothes", "kapre", "کپڑے", "blanket", "kambal", "کمبل")) {
            add(Need.CLOTHING)
        }
        if (containsAny(normalized, "power", "electricity", "bijli", "charge", "بجلی", "چارج")) {
            add(Need.POWER)
        }
    }

    private fun containsAny(text: String, vararg terms: String): Boolean =
        terms.any { text.contains(it) }

    private companion object {
        val FLOOD_TERMS = listOf(
            "flood", "pani", "paani", "water", "selab", "sailab", "barish", "baarish",
            "سیلاب", "پانی", "بارش", "طغیانی",
        )
        val EARTHQUAKE_TERMS = listOf(
            "earthquake", "quake", "bhukamp", "bhukam", "zalzala", "zameen hili", "jhatka",
            "زلزلہ", "بھوکمپ", "زمین ہلی",
        )
        val LANDSLIDE_TERMS = listOf(
            "landslide", "land slide", "zameen khisak", "zameen khisak gayi", "pahar gir",
            "pahad gir", "zameen dhans", "لینڈ سلائیڈ", "زمین کھسک", "پہاڑ گر",
        )
        val CRITICAL_TERMS = listOf(
            "trapped", "phansa", "phansi", "drowning", "doob", "bleeding", "dying",
            "can't breathe", "cant breathe", "unconscious", "behosh", "بے ہوش", "پھنس",
            "بچاؤ", "زخمی", "مدد", "ڈوب",
        )
        val HIGH_TERMS = listOf(
            "rising", "barh", "barh raha", "stuck", "children", "bachay", "bachche",
            "pregnant", "hamal", "danger", "khatra", "خطرہ", "بچے", "حاملہ", "بڑھ",
        )
        val LOW_TERMS = listOf(
            "safe", "minor", "thoda", "theek", "محفوظ", "ٹھیک",
        )
        val FAMILY_TERMS = listOf("family", "ghar wale", "gharwalay", "khandan", "خاندان", "گھر والے")
        val DIGIT_PEOPLE = Regex("\\b(\\d{1,3})\\s*(?:people|persons?|log|afrad|bande|لوگ|افراد|بندے)\\b")
        val WORD_PEOPLE = Regex("\\b(do|teen|char|chaar|paanch|panch|chhe|chhah|saat|aath|nau|das)\\s+(?:log|afrad|bande|people)\\b")
        val FAMILY_OF = Regex("\\bfamily of\\s+(\\d{1,3})\\b")
        val WORD_NUMBERS = mapOf(
            "do" to 2, "teen" to 3, "char" to 4, "chaar" to 4, "paanch" to 5, "panch" to 5,
            "chhe" to 6, "chhah" to 6, "saat" to 7, "aath" to 8, "nau" to 9, "das" to 10,
        )
    }
}
