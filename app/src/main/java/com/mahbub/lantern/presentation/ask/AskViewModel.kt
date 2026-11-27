package com.mahbub.lantern.presentation.ask

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahbub.lantern.domain.model.Alert
import com.mahbub.lantern.domain.model.AppLanguage
import com.mahbub.lantern.domain.usecase.ObserveAlertsUseCase
import com.mahbub.lantern.domain.usecase.ObserveConnectivityUseCase
import com.mahbub.lantern.domain.usecase.ObserveLanguageUseCase
import com.mahbub.lantern.domain.usecase.RefreshAlertsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AskUiState(
    val query: String = "",
    val isOnline: Boolean = false,
    val latestAlert: Alert? = null,
    val language: AppLanguage = AppLanguage.EN,
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
    val blankError: Boolean = false,
)

sealed interface AskEvent {
    data class OpenAnswer(val question: String) : AskEvent
    data object OpenSettings : AskEvent
    data object OpenDebug : AskEvent
}

@HiltViewModel
class AskViewModel @Inject constructor(
    private val observeAlerts: ObserveAlertsUseCase,
    private val observeConnectivity: ObserveConnectivityUseCase,
    private val observeLanguage: ObserveLanguageUseCase,
    private val refreshAlerts: RefreshAlertsUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(AskUiState())
    val state: StateFlow<AskUiState> = _state.asStateFlow()

    private val _events = Channel<AskEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    fun load() {
        loadJob?.cancel()
        _state.update { it.copy(isLoading = true, hasError = false) }
        loadJob = viewModelScope.launch {
            try {
                combine(
                    observeAlerts(),
                    observeConnectivity(),
                    observeLanguage(),
                ) { alerts, online, language ->
                    Triple(alerts, online, language)
                }.collect { (alerts, online, language) ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            hasError = false,
                            isOnline = online,
                            language = language,
                            latestAlert = alerts.maxByOrNull { alert -> alert.issuedAt },
                        )
                    }
                }
            } catch (t: Throwable) {
                _state.update { it.copy(isLoading = false, hasError = true) }
            }
        }
    }

    fun retry() {
        viewModelScope.launch { runCatching { refreshAlerts() } }
        load()
    }

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query, blankError = false) }
    }

    fun ask() {
        val question = _state.value.query.trim()
        if (question.isEmpty()) {
            _state.update { it.copy(blankError = true) }
            return
        }
        viewModelScope.launch { _events.send(AskEvent.OpenAnswer(question)) }
    }

    fun onQuickChip(text: String) {
        _state.update { it.copy(query = text, blankError = false) }
        viewModelScope.launch { _events.send(AskEvent.OpenAnswer(text)) }
    }

    fun openSettings() {
        viewModelScope.launch { _events.send(AskEvent.OpenSettings) }
    }

    fun openDebug() {
        viewModelScope.launch { _events.send(AskEvent.OpenDebug) }
    }

}
