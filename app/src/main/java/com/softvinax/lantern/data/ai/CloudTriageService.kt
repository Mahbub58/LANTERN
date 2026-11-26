package com.softvinax.lantern.data.ai

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.InferenceMode
import com.google.firebase.ai.OnDeviceConfig
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.PublicPreviewAPI
import com.softvinax.lantern.domain.ai.PiiScrubber
import com.softvinax.lantern.domain.model.AppLanguage
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cloud-only desk tasks that are intentionally not part of the citizen answer
 * path yet. They live behind a small service so the Phase 3 responder "Desk"
 * can call them without changing the citizen UI or the domain layer.
 *
 * Every input is scrubbed with [PiiScrubber] first. Coordinates are never part
 * of these prompts.
 */
@OptIn(PublicPreviewAPI::class)
@Singleton
class CloudTriageService @Inject constructor(
    private val piiScrubber: PiiScrubber,
) {

    private val model: GenerativeModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel(
                modelName = FirebaseAiConfig.CLOUD_MODEL_NAME,
                onDeviceConfig = OnDeviceConfig(
                    mode = InferenceMode.ONLY_IN_CLOUD,
                    maxOutputTokens = 512,
                    temperature = 0.1f,
                    candidateCount = 1,
                ),
            )
    }

    /** Re-checks the deterministic urgency score with a cloud model. */
    suspend fun recheckUrgency(description: String): Int? = runCatching {
        val response = model.generateContent(AiPrompts.urgency(piiScrubber.scrub(description)))
        Log.i(TAG, "urgency inferenceSource=${response.inferenceSource}")
        ModelJson.urgency(response.text)
    }.getOrNull()

    /** Groups reports that describe the same incident. Ids are preserved. */
    suspend fun groupDuplicates(reports: List<Pair<String, String>>): List<List<String>> {
        if (reports.size < 2) return emptyList()
        val scrubbed = reports.map { (id, description) -> id to piiScrubber.scrub(description) }
        return runCatching {
            val response = model.generateContent(AiPrompts.duplicateGroups(scrubbed))
            Log.i(TAG, "duplicates inferenceSource=${response.inferenceSource}")
            ModelJson.duplicateGroups(response.text, reports.map { it.first }.toSet())
        }.getOrDefault(emptyList())
    }

    /** Short situation brief for a relief desk. */
    suspend fun situationBrief(
        alerts: List<String>,
        reports: List<String>,
        language: AppLanguage,
    ): String? = runCatching {
        val response = model.generateContent(
            AiPrompts.situationBrief(
                alerts = alerts.map(piiScrubber::scrub),
                reports = reports.map(piiScrubber::scrub),
                language = language,
            ),
        )
        Log.i(TAG, "brief inferenceSource=${response.inferenceSource}")
        response.text?.trim()?.takeIf { it.isNotEmpty() }
    }.getOrNull()

    private companion object {
        const val TAG = "LanternTriage"
    }
}
