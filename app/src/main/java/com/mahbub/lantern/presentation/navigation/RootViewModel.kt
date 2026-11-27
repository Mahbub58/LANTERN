package com.mahbub.lantern.presentation.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahbub.lantern.domain.model.AppLanguage
import com.mahbub.lantern.domain.repository.AppCheckFailure
import com.mahbub.lantern.domain.usecase.DismissAppCheckFailureUseCase
import com.mahbub.lantern.domain.usecase.IsLanguageChosenUseCase
import com.mahbub.lantern.domain.usecase.ObserveAppCheckFailureUseCase
import com.mahbub.lantern.domain.usecase.ObserveLanguageUseCase
import com.mahbub.lantern.domain.usecase.RetryAppCheckUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class RootViewModel @Inject constructor(
    observeLanguage: ObserveLanguageUseCase,
    isLanguageChosen: IsLanguageChosenUseCase,
    observeAppCheckFailure: ObserveAppCheckFailureUseCase,
    private val retryAppCheckUseCase: RetryAppCheckUseCase,
    private val dismissAppCheckFailureUseCase: DismissAppCheckFailureUseCase,
) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = true,
        val language: AppLanguage = AppLanguage.EN,
        val languageChosen: Boolean = false,
    )

    val state: StateFlow<UiState> = combine(
        observeLanguage(),
        isLanguageChosen(),
    ) { language, chosen ->
        UiState(isLoading = false, language = language, languageChosen = chosen)
    }
        .catch { emit(UiState(isLoading = false, language = AppLanguage.EN, languageChosen = false)) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UiState(),
        )

    /** Non-null while App Check attestation is failing. */
    val appCheckFailure: StateFlow<AppCheckFailure?> = observeAppCheckFailure()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    fun retryAppCheck() = retryAppCheckUseCase()

    fun dismissAppCheckFailure() = dismissAppCheckFailureUseCase()
}
