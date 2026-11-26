package com.softvinax.lantern.presentation.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softvinax.lantern.domain.model.Alert
import com.softvinax.lantern.domain.model.AppLanguage
import com.softvinax.lantern.domain.usecase.ObserveAlertsUseCase
import com.softvinax.lantern.domain.usecase.ObserveConnectivityUseCase
import com.softvinax.lantern.domain.usecase.ObserveLanguageUseCase
import com.softvinax.lantern.domain.usecase.RefreshAlertsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AlertsUiState(
    val isLoading: Boolean = true,
    val alerts: List<Alert> = emptyList(),
    val isOnline: Boolean = false,
    val hasError: Boolean = false,
    val language: AppLanguage = AppLanguage.EN,
)

sealed interface AlertsEvent {
    data object Retry : AlertsEvent
}

@HiltViewModel
class AlertsViewModel @Inject constructor(
    private val observeAlerts: ObserveAlertsUseCase,
    private val observeConnectivity: ObserveConnectivityUseCase,
    private val observeLanguage: ObserveLanguageUseCase,
    private val refreshAlerts: RefreshAlertsUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(AlertsUiState())
    val state: StateFlow<AlertsUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        refresh()
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
                            alerts = alerts,
                            isOnline = online,
                            language = language,
                            hasError = false,
                        )
                    }
                }
            } catch (t: Throwable) {
                _state.update { it.copy(isLoading = false, hasError = true) }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch { runCatching { refreshAlerts() } }
    }

    fun retry() {
        refresh()
        load()
    }
}
