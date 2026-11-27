package com.mahbub.lantern.data.mapper

import com.mahbub.lantern.data.asset.LocalizedListDto
import com.mahbub.lantern.data.asset.LocalizedTextDto
import com.mahbub.lantern.data.asset.SafetyCardDto
import com.mahbub.lantern.domain.model.LocalizedList
import com.mahbub.lantern.domain.model.LocalizedText
import com.mahbub.lantern.domain.model.SafetyCard

fun LocalizedTextDto.toDomain(): LocalizedText = LocalizedText(en = en, ur = ur, roman = roman)

fun LocalizedListDto.toDomain(): LocalizedList = LocalizedList(en = en, ur = ur, roman = roman)

fun SafetyCardDto.toDomain(): SafetyCard = SafetyCard(
    id = id.trim(),
    title = title.toDomain(),
    steps = steps.toDomain(),
    never = never.toDomain(),
    source = source.trim(),
    keywords = keywords.toDomain(),
)

/**
 * Cards are only usable if they can actually be shown and grounded. A malformed
 * asset is a build-time/packaging error, so it fails loudly instead of silently
 * serving empty guidance in a disaster.
 */
fun List<SafetyCardDto>.toDomainValidated(): List<SafetyCard> {
    val mapped = map { it.toDomain() }
    mapped.forEach { card ->
        require(card.id.isNotBlank()) { "Safety card has no id" }
        require(!card.title.isBlank()) { "Safety card ${card.id} has no title" }
        require(card.steps.forLanguage(com.mahbub.lantern.domain.model.AppLanguage.EN).isNotEmpty()) {
            "Safety card ${card.id} has no English steps"
        }
        require(card.source.isNotBlank()) { "Safety card ${card.id} has no source" }
    }
    require(mapped.map { it.id }.toSet().size == mapped.size) { "Duplicate safety card ids" }
    return mapped
}
