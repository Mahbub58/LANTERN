package com.softvinax.lantern.domain.ai

import javax.inject.Inject

/**
 * Hard safety rails for generated text. Lantern never produces medical doses or
 * diagnoses, on-device or in the cloud.
 */
class SafetyGuard @Inject constructor() {

    fun isUnsafe(text: String): Boolean = hasDosage(text) || hasDiagnosis(text)

    fun hasDosage(text: String): Boolean =
        DOSAGE_PATTERN.containsMatchIn(text) || WORDED_DOSAGE.containsMatchIn(text)

    fun hasDiagnosis(text: String): Boolean =
        DIAGNOSIS_PATTERN.containsMatchIn(text) || DIAGNOSE_VERB.containsMatchIn(text)

    private companion object {
        val DOSAGE_PATTERN = Regex(
            "(?i)\\b\\d+(?:[.,]\\d+)?\\s*(?:mg|ml|mcg|g|gram|grams|tablet|tablets|pill|pills|capsule|capsules|dose|doses|puff|puffs|drop|drops)\\b",
        )
        val WORDED_DOSAGE = Regex(
            "(?i)\\b(?:take|give|use)\\s+(?:one|two|three|four|five|six|\\d+)\\s+(?:tablet|tablets|pill|pills|capsule|capsules|dose|doses)\\b",
        )
        val DIAGNOSIS_PATTERN = Regex(
            "(?i)\\b(?:you (?:probably |likely |may )?(?:have|are suffering from)|this (?:is|looks like)|it (?:is|looks like))\\s+" +
                "(?:dengue|malaria|cholera|typhoid|covid|infection|fracture|concussion|heart attack|stroke|pneumonia)\\b",
        )
        val DIAGNOSE_VERB = Regex("(?i)\\bdiagnos(?:e|ed|is|ing)\\b")
    }
}
