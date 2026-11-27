package com.mahbub.lantern.testutil

import com.mahbub.lantern.domain.model.LocalizedList
import com.mahbub.lantern.domain.model.LocalizedText
import com.mahbub.lantern.domain.model.SafetyCard

fun floodCard(
    id: String = "flood-1",
    titleEn: String = "Water is entering your home",
    keywordsEn: List<String> = listOf("flood water entering home"),
    stepsEn: List<String> = listOf(
        "Move to the highest floor.",
        "Switch off electricity at the main breaker.",
    ),
): SafetyCard = SafetyCard(
    id = id,
    title = LocalizedText(
        en = titleEn,
        ur = "پانی آپ کے گھر میں داخل ہو رہا ہے",
        roman = "Pani aap ke ghar mein dakhil ho raha hai",
    ),
    steps = LocalizedList(
        en = stepsEn,
        ur = listOf("سب سے اوپر والی منزل پر جائیں۔"),
        roman = listOf("Sab se oopar wali manzil par jayen."),
    ),
    never = LocalizedList(
        en = listOf("Never walk or drive through moving flood water."),
        ur = listOf("بہتے سیلابی پانی میں کبھی نہ چلیں۔"),
        roman = listOf("Behte sailabi pani mein kabhi na chalein."),
    ),
    source = "TO VERIFY",
    keywords = LocalizedList(
        en = keywordsEn,
        ur = listOf("سیلاب", "پانی", "گھر"),
        roman = listOf("pani", "ghar", "selab"),
    ),
)

fun earthquakeCard(id: String = "quake-1"): SafetyCard = SafetyCard(
    id = id,
    title = LocalizedText(
        en = "Earthquake shaking right now",
        ur = "ابھی زلزلہ",
        roman = "Abhi zalzala",
    ),
    steps = LocalizedList(
        en = listOf("Drop, cover, and hold on.", "Stay away from windows."),
        ur = listOf("جھکیں، ڈھانپیں اور پکڑ کر رکھیں۔"),
        roman = listOf("Jhukein, dhanpein aur pakar kar rakhein."),
    ),
    never = LocalizedList(
        en = listOf("Never use a lift during shaking."),
        ur = listOf("زلزلے کے دوران لفٹ کبھی استعمال نہ کریں۔"),
        roman = listOf("Zalzale ke doran lift kabhi istemal na karein."),
    ),
    source = "TO VERIFY",
    keywords = LocalizedList(
        en = listOf("earthquake shaking quake"),
        ur = listOf("زلزلہ"),
        roman = listOf("zalzala", "bhukamp"),
    ),
)
