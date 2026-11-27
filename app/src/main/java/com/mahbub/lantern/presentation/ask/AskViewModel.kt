package com.mahbub.lantern.presentation.ask

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahbub.lantern.domain.ai.NanoStatus
import com.mahbub.lantern.domain.model.Alert
import com.mahbub.lantern.domain.model.AppLanguage
import com.mahbub.lantern.domain.usecase.IsNanoReadyUseCase
import com.mahbub.lantern.domain.usecase.ObserveAlertsUseCase
import com.mahbub.lantern.domain.usecase.ObserveConnectivityUseCase
import com.mahbub.lantern.domain.usecase.ObserveLanguageUseCase
import com.mahbub.lantern.domain.usecase.ObserveNanoStatusUseCase
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
    val nanoStatus: NanoStatus = NanoStatus.UNAVAILABLE,
    val isCheckingNano: Boolean = false,
    val nanoError: Boolean = false,
) {
    /** Nano is the only matcher, so the Ask action must not navigate without it. */
    val canAsk: Boolean get() = nanoStatus == NanoStatus.READY
}

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
    private val isNanoReady: IsNanoReadyUseCase,
    private val observeNanoStatus: ObserveNanoStatusUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(AskUiState())
    val state: StateFlow<AskUiState> = _state.asStateFlow()

    private val _events = Channel<AskEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var loadJob: Job? = null

    init {
        load()
        viewModelScope.launch {
            observeNanoStatus().collect { status ->
                _state.update { it.copy(nanoStatus = status) }
            }
        }
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
        askQuestion(question)
    }

    fun onQuickChip(text: String) {
        _state.update { it.copy(query = text, blankError = false) }
        askQuestion(text)
    }

    /**
     * Gemini Nano performs all matching, so the answer page is only opened
     * when Nano is ready. Otherwise the user stays here and sees an error.
     */
    private fun askQuestion(question: String) {
        if (_state.value.isCheckingNano) return
        viewModelScope.launch {
            _state.update { it.copy(isCheckingNano = true, nanoError = false) }
            val ready = runCatching { isNanoReady() }.getOrDefault(false)
            _state.update { it.copy(isCheckingNano = false) }
            if (!ready) {
                _state.update { it.copy(nanoError = true) }
                return@launch
            }
            _events.send(AskEvent.OpenAnswer(question))
        }
    }

    fun openSettings() {
        viewModelScope.launch { _events.send(AskEvent.OpenSettings) }
    }

    fun openDebug() {
        viewModelScope.launch { _events.send(AskEvent.OpenDebug) }
    }

}
