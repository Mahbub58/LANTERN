package com.softvinax.lantern.presentation.language

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softvinax.lantern.domain.model.AppLanguage
import com.softvinax.lantern.domain.usecase.ObserveConnectivityUseCase
import com.softvinax.lantern.domain.usecase.SetLanguageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LanguageUiState(
    val selected: AppLanguage = AppLanguage.EN,
    val isSaving: Boolean = false,
    val hasError: Boolean = false,
    val isOnline: Boolean = false,
)

sealed interface LanguageEvent {
    data class Saved(val language: AppLanguage) : LanguageEvent
}

@HiltViewModel
class LanguageViewModel @Inject constructor(
    observeConnectivity: ObserveConnectivityUseCase,
    private val setLanguage: SetLanguageUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(LanguageUiState())
    val state: StateFlow<LanguageUiState> = _state.asStateFlow()

    private val _events = Channel<LanguageEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            observeConnectivity().collect { online ->
                _state.update { it.copy(isOnline = online) }
            }
        }
    }

    fun select(language: AppLanguage) {
        _state.update { it.copy(selected = language, hasError = false) }
    }

    fun confirm() {
        if (_state.value.isSaving) return
        val language = _state.value.selected
        _state.update { it.copy(isSaving = true, hasError = false) }
        viewModelScope.launch {
            runCatching { setLanguage(language) }
                .onSuccess {
                    _state.update { it.copy(isSaving = false) }
                    _events.trySend(LanguageEvent.Saved(language))
                }
                .onFailure {
                    _state.update { it.copy(isSaving = false, hasError = true) }
                }
        }
    }
}
