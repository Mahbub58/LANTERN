package com.mahbub.lantern.domain.usecase

import com.mahbub.lantern.domain.repository.AppCheckFailure
import com.mahbub.lantern.domain.repository.AppCheckRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

class ObserveAppCheckFailureUseCase @Inject constructor(
    private val repository: AppCheckRepository,
) {
    operator fun invoke(): StateFlow<AppCheckFailure?> = repository.failure
}

class RetryAppCheckUseCase @Inject constructor(
    private val repository: AppCheckRepository,
) {
    operator fun invoke() = repository.retry()
}

class DismissAppCheckFailureUseCase @Inject constructor(
    private val repository: AppCheckRepository,
) {
    operator fun invoke() = repository.dismiss()
}
