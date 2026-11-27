package com.mahbub.lantern.data.asset

import kotlinx.serialization.Serializable

@Serializable
data class SafetyCardBundleDto(
    val version: String = "1",
    val cards: List<SafetyCardDto> = emptyList(),
)

@Serializable
data class SafetyCardDto(
    val id: String = "",
    val title: LocalizedTextDto = LocalizedTextDto(),
    val steps: LocalizedListDto = LocalizedListDto(),
    val never: LocalizedListDto = LocalizedListDto(),
    val source: String = "",
    val keywords: LocalizedListDto = LocalizedListDto(),
)

@Serializable
data class LocalizedTextDto(
    val en: String = "",
    val ur: String = "",
    val roman: String = "",
)

@Serializable
data class LocalizedListDto(
    val en: List<String> = emptyList(),
    val ur: List<String> = emptyList(),
    val roman: List<String> = emptyList(),
)
