package com.mahbub.lantern.presentation.navigation

import com.mahbub.lantern.domain.model.AppLanguage
import com.mahbub.lantern.domain.usecase.IsLanguageChosenUseCase
import com.mahbub.lantern.domain.usecase.ObserveLanguageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

@HiltViewModel
class RootViewModel @Inject constructor(
    observeLanguage: ObserveLanguageUseCase,
    isLanguageChosen: IsLanguageChosenUseCase,
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
}
