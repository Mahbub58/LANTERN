package com.softvinax.lantern.domain.usecase

import com.softvinax.lantern.domain.ai.AiRouter
import com.softvinax.lantern.domain.model.Answer
import com.softvinax.lantern.domain.model.AppLanguage
import com.softvinax.lantern.domain.repository.ConfigRepository
import com.softvinax.lantern.domain.repository.ConnectivityRepository
import com.softvinax.lantern.domain.repository.SafetyCardRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Loads cards + routing config + connectivity, then delegates the decision to
 * [AiRouter]. The router stays pure; this use case is the composition seam.
 */
class AskLanternUseCase @Inject constructor(
    private val safetyCardRepository: SafetyCardRepository,
    private val configRepository: ConfigRepository,
    private val connectivityRepository: ConnectivityRepository,
    private val aiRouter: AiRouter,
) {
    suspend operator fun invoke(
        question: String,
        language: AppLanguage? = null,
        deeper: Boolean = false,
    ): Answer {
        val cards = safetyCardRepository.getAll()
        val config = configRepository.routingConfig.first()
        val isOnline = connectivityRepository.isOnline.first()
        val effectiveLanguage = language ?: configRepository.language.first()

        val answer = aiRouter.route(
            question = question,
            language = effectiveLanguage,
            config = config,
            isOnline = isOnline,
            cards = cards,
            forceCloud = deeper,
        )
        configRepository.recordInference(answer.source)

        return if (answer.isNoMatch) {
            answer.copy(emergencyNumbers = configRepository.emergencyNumbers.first())
        } else {
            answer
        }
    }
}
