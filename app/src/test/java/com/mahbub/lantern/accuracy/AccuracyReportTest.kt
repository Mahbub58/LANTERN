package com.mahbub.lantern.accuracy

import com.mahbub.lantern.data.asset.SafetyCardBundleDto
import com.mahbub.lantern.data.mapper.toDomainValidated
import com.mahbub.lantern.domain.ai.LexicalSafetyCardRetriever
import com.mahbub.lantern.domain.model.AppLanguage
import com.mahbub.lantern.domain.model.EmergencyType
import com.mahbub.lantern.domain.model.Need
import com.mahbub.lantern.domain.model.RoutingConfig
import com.mahbub.lantern.domain.usecase.StructureReportUseCase
import java.io.File
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Debug-only accuracy runner for the deterministic, offline parts of Lantern.
 *
 * Run it with:
 *   ./gradlew :app:testDebugUnitTest --tests "*AccuracyReportTest*" --info
 *
 * The full summary is also written to
 *   app/build/reports/accuracy/summary.txt
 *
 * This test never fails on a low score: it reports the real numbers so they can
 * be published honestly. It only fails if the CSV test set is malformed.
 */
class AccuracyReportTest {

    private val retriever = LexicalSafetyCardRetriever()
    private val structureReport = StructureReportUseCase()

    @Test
    fun printAccuracy() {
        val cards = loadCards()
        val questions = readCsv(assetFile("test/questions.csv"))
        val reports = readCsv(assetFile("test/reports.csv"))

        assertEquals("questions.csv must contain 40 questions", 40, questions.size)
        assertEquals("reports.csv must contain 20 reports", 20, reports.size)
        assertEquals("questions.csv has 4 columns", 4, questions.first().size)
        assertEquals("reports.csv has 7 columns", 7, reports.first().size)

        val summary = buildString {
            appendLine("Lantern offline accuracy report")
            appendLine("cards=${cards.size}")
            appendLine()
            appendCardAccuracy(cards, questions)
            appendLine()
            appendFieldAccuracy(reports)
        }

        println(summary)
        val output = File("build/reports/accuracy/summary.txt")
        output.parentFile?.mkdirs()
        output.writeText(summary)
    }

    private fun StringBuilder.appendCardAccuracy(
        cards: List<com.mahbub.lantern.domain.model.SafetyCard>,
        rows: List<Map<String, String>>,
    ) {
        val byLanguage = linkedMapOf<String, MutableList<Boolean>>()
        val misses = mutableListOf<String>()
        rows.forEach { row ->
            val language = row.getValue("language")
            val question = row.getValue("question")
            val expected = row.getValue("expected_card_id")
            val scored = retriever.bestCard(question, cards)
            val actual = scored
                ?.takeIf { it.score >= RoutingConfig.MINIMUM_MATCH_SCORE }
                ?.card
                ?.id
            val hit = actual == expected
            byLanguage.getOrPut(language) { mutableListOf() }.add(hit)
            if (!hit) {
                misses += "  MISS [$language] \"$question\" expected=$expected actual=${actual ?: "NO_MATCH"}"
            }
        }

        val total = rows.size
        val hits = byLanguage.values.sumOf { values -> values.count { it } }
        appendLine("CARD MATCH (top-1, score >= ${RoutingConfig.MINIMUM_MATCH_SCORE})")
        appendLine("  overall: $hits/$total = ${percent(hits, total)}")
        byLanguage.forEach { (language, values) ->
            appendLine("  $language: ${values.count { it }}/${values.size} = ${percent(values.count { it }, values.size)}")
        }
        if (misses.isNotEmpty()) {
            appendLine("  misses:")
            misses.forEach { appendLine(it) }
        }
    }

    private fun StringBuilder.appendFieldAccuracy(rows: List<Map<String, String>>) {
        var typeHits = 0
        var urgencyHits = 0
        var urgencyWithinOne = 0
        var peopleHits = 0
        var needsExact = 0
        var needItemHits = 0
        var needItemTotal = 0
        val misses = mutableListOf<String>()

        rows.forEach { row ->
            val draft = structureReport(
                description = row.getValue("description"),
                language = parseLanguage(row.getValue("language")),
                now = 1_700_000_000_000L,
            )
            val expectedType = EmergencyType.valueOf(row.getValue("expected_type"))
            val expectedUrgency = row.getValue("expected_urgency").toInt()
            val expectedPeople = row.getValue("expected_people").toInt()
            val expectedNeeds = row.getValue("expected_needs")
                .split(';')
                .filter { it.isNotBlank() }
                .map { Need.valueOf(it.trim()) }
                .toSet()

            val typeOk = draft.type == expectedType
            val urgencyOk = draft.urgency == expectedUrgency
            val urgencyNear = kotlin.math.abs(draft.urgency - expectedUrgency) <= 1
            val peopleOk = draft.peopleCount == expectedPeople
            val needsOk = draft.needs == expectedNeeds

            if (typeOk) typeHits++
            if (urgencyOk) urgencyHits++
            if (urgencyNear) urgencyWithinOne++
            if (peopleOk) peopleHits++
            if (needsOk) needsExact++
            needItemTotal += expectedNeeds.size
            needItemHits += expectedNeeds.count { it in draft.needs }

            if (!typeOk || !urgencyOk || !peopleOk || !needsOk) {
                misses += "  MISS [${row.getValue("id")}] " +
                    "type=${draft.type}/${expectedType} " +
                    "urgency=${draft.urgency}/$expectedUrgency " +
                    "people=${draft.peopleCount}/$expectedPeople " +
                    "needs=${draft.needs.map { it.name }}/$expectedNeeds"
            }
        }

        val total = rows.size
        appendLine("REPORT FIELD EXTRACTION")
        appendLine("  type exact: $typeHits/$total = ${percent(typeHits, total)}")
        appendLine("  urgency exact: $urgencyHits/$total = ${percent(urgencyHits, total)}")
        appendLine("  urgency within 1: $urgencyWithinOne/$total = ${percent(urgencyWithinOne, total)}")
        appendLine("  people exact: $peopleHits/$total = ${percent(peopleHits, total)}")
        appendLine("  needs exact set: $needsExact/$total = ${percent(needsExact, total)}")
        appendLine("  need items: $needItemHits/$needItemTotal = ${percent(needItemHits, needItemTotal)}")
        if (misses.isNotEmpty()) {
            appendLine("  misses:")
            misses.forEach { appendLine(it) }
        }
    }

    private fun loadCards(): List<com.mahbub.lantern.domain.model.SafetyCard> {
        val raw = assetFile("safety_cards.json").readText()
        return Json { ignoreUnknownKeys = true }
            .decodeFromString<SafetyCardBundleDto>(raw)
            .cards
            .toDomainValidated()
    }

    private fun parseLanguage(raw: String): AppLanguage = when (raw.lowercase()) {
        "ur" -> AppLanguage.UR
        "roman" -> AppLanguage.ROMAN_UR
        else -> AppLanguage.EN
    }

    private fun assetFile(name: String): File =
        listOf(
            File("src/main/assets/$name"),
            File("app/src/main/assets/$name"),
        ).firstOrNull { it.exists() }
            ?: error("Missing test asset $name (working dir: ${File(".").absolutePath})")

    private fun percent(hits: Int, total: Int): String =
        if (total == 0) "n/a" else "%.1f%%".format(100.0 * hits / total)

    private fun readCsv(file: File): List<Map<String, String>> {
        val rows = parseCsv(file.readText(Charsets.UTF_8))
        if (rows.isEmpty()) return emptyList()
        val header = rows.first()
        return rows.drop(1)
            .filter { row -> row.any { it.isNotBlank() } }
            .map { row ->
                header.indices.associate { index -> header[index] to row.getOrElse(index) { "" } }
            }
    }

    /** Minimal RFC 4180 parser: quoted fields, doubled quotes, CRLF. */
    private fun parseCsv(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val row = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var index = 0
        while (index < text.length) {
            val char = text[index]
            when {
                char == '"' && inQuotes && index + 1 < text.length && text[index + 1] == '"' -> {
                    field.append('"')
                    index++
                }
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    row.add(field.toString())
                    field.clear()
                }
                (char == '\n' || char == '\r') && !inQuotes -> {
                    if (field.isNotEmpty() || row.isNotEmpty()) {
                        row.add(field.toString())
                        rows.add(row.toList())
                        row.clear()
                        field.clear()
                    }
                    if (char == '\r' && index + 1 < text.length && text[index + 1] == '\n') {
                        index++
                    }
                }
                else -> field.append(char)
            }
            index++
        }
        if (field.isNotEmpty() || row.isNotEmpty()) {
            row.add(field.toString())
            rows.add(row.toList())
        }
        return rows
    }
}
