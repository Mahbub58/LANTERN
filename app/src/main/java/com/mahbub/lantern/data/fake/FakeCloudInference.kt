package com.mahbub.lantern.data.fake

import com.mahbub.lantern.domain.ai.CloudInference
import com.mahbub.lantern.domain.model.AppLanguage
import com.mahbub.lantern.domain.model.SafetyCard
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay

/**
 * Offline demo stand-in for the cloud model. It receives only scrubbed text (the
 * router applies PiiScrubber before this call) and answers from the same card so
 * the demo never invents advice.
 */
@Singleton
class FakeCloudInference @Inject constructor() : CloudInference {

    override suspend fun answer(
        question: String,
        card: SafetyCard,
        language: AppLanguage,
    ): String {
        delay(1200)
        val title = card.title.forLanguage(language)
        val steps = card.steps.forLanguage(language).take(3).joinToString(" ")
        return listOf(title, steps).filter { it.isNotBlank() }.joinToString(" ")
    }
}
