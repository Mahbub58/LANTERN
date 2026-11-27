package com.mahbub.lantern.presentation.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahbub.lantern.domain.ai.NanoStatus
import com.mahbub.lantern.domain.model.AlertLevel
import com.mahbub.lantern.domain.model.InferenceSource
import com.mahbub.lantern.domain.model.RoutingConfig
import com.mahbub.lantern.domain.usecase.GetAnonymousUidUseCase
import com.mahbub.lantern.domain.usecase.ObserveAlertLevelUseCase
import com.mahbub.lantern.domain.usecase.ObserveCardsVersionUseCase
import com.mahbub.lantern.domain.usecase.ObserveConnectivityUseCase
import com.mahbub.lantern.domain.usecase.ObserveLastInferenceSourceUseCase
import com.mahbub.lantern.domain.usecase.ObserveNanoStatusUseCase
import com.mahbub.lantern.domain.usecase.ObserveRoutingConfigUseCase
import com.mahbub.lantern.domain.usecase.SetAlertLevelUseCase
import com.mahbub.lantern.domain.usecase.SetRoutingConfigUseCase
import com.mahbub.lantern.domain.usecase.SetSimulatedOnlineUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DebugUiState(
    val isLoading: Boolean = true,
    val isOnline: Boolean = false,
    val lastSource: InferenceSource? = null,
    val config: RoutingConfig = RoutingConfig(),
    val nanoStatus: NanoStatus? = null,
    val cardsVersion: String = "",
    val alertLevel: AlertLevel = AlertLevel.WARNING,
    val uid: String? = null,
    val hasError: Boolean = false,
)

sealed interface DebugEvent {
    data object ResetDone : DebugEvent
}

private data class DebugPartial(
    val isOnline: Boolean,
    val lastSource: InferenceSource?,
    val config: RoutingConfig,
    val nanoStatus: NanoStatus,
    val cardsVersion: String,
)

@HiltViewModel
class DebugViewModel @Inject constructor(
    private val observeConnectivity: ObserveConnectivityUseCase,
    private val observeLastInferenceSource: ObserveLastInferenceSourceUseCase,
    private val observeRoutingConfig: ObserveRoutingConfigUseCase,
    private val observeNanoStatus: ObserveNanoStatusUseCase,
    private val observeCardsVersion: ObserveCardsVersionUseCase,
    private val observeAlertLevel: ObserveAlertLevelUseCase,
    private val setSimulatedOnline: SetSimulatedOnlineUseCase,
    private val setRoutingConfig: SetRoutingConfigUseCase,
    private val setAlertLevel: SetAlertLevelUseCase,
    private val getAnonymousUid: GetAnonymousUidUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(DebugUiState())
    val state: StateFlow<DebugUiState> = _state.asStateFlow()

    private val _events = kotlinx.coroutines.channels.Channel<DebugEvent>(
        kotlinx.coroutines.channels.Channel.BUFFERED,
    )
    val events = _events.receiveAsFlow()

    private var loadJob: Job? = null

    init {
        load()
        viewModelScope.launch {
            runCatching { getAnonymousUid() }
                .onSuccess { uid -> _state.update { it.copy(uid = uid) } }
        }
    }

    private fun load() {
        loadJob?.cancel()
        _state.update { it.copy(isLoading = true, hasError = false) }
        loadJob = viewModelScope.launch {
            try {
                val partial = combine(
                    observeConnectivity(),
                    observeLastInferenceSource(),
                    observeRoutingConfig(),
                    observeNanoStatus(),
                    observeCardsVersion(),
                ) { online, source, config, nano, version ->
                    DebugPartial(online, source, config, nano, version)
                }
                partial.combine(observeAlertLevel()) { values, level ->
                    DebugUiState(
                        isLoading = false,
                        isOnline = values.isOnline,
                        lastSource = values.lastSource,
                        config = values.config,
                        nanoStatus = values.nanoStatus,
                        cardsVersion = values.cardsVersion,
                        alertLevel = level,
                        uid = _state.value.uid,
                    )
                }.collect { next ->
                    _state.update {
                        next.copy(uid = it.uid, hasError = false)
                    }
                }
            } catch (t: Throwable) {
                _state.update { it.copy(isLoading = false, hasError = true) }
            }
        }
    }

    fun retry() = load()

    fun setOnline(online: Boolean) {
        _state.update { it.copy(isOnline = online, hasError = false) }
        viewModelScope.launch {
            runCatching { setSimulatedOnline(online) }
                .onFailure { _state.update { it.copy(hasError = true) } }
        }
    }

    fun setThreshold(threshold: Float) {
        val config = _state.value.config.copy(confidenceThreshold = threshold.coerceIn(0f, 1f))
        _state.update { it.copy(config = config, hasError = false) }
        viewModelScope.launch {
            runCatching { setRoutingConfig(config) }
                .onFailure { _state.update { it.copy(hasError = true) } }
        }
    }

    fun setCloudFallback(enabled: Boolean) {
        val config = _state.value.config.copy(cloudFallbackEnabled = enabled)
        _state.update { it.copy(config = config, hasError = false) }
        viewModelScope.launch {
            runCatching { setRoutingConfig(config) }
                .onFailure { _state.update { it.copy(hasError = true) } }
        }
    }

    fun updateAlertLevel(level: AlertLevel) {
        _state.update { it.copy(alertLevel = level, hasError = false) }
        viewModelScope.launch {
            runCatching { setAlertLevel(level) }
                .onFailure { _state.update { it.copy(hasError = true) } }
        }
    }

    fun reset() {
        _state.update {
            it.copy(
                isOnline = false,
                config = RoutingConfig(),
                alertLevel = AlertLevel.WARNING,
                hasError = false,
            )
        }
        viewModelScope.launch {
            runCatching {
                setSimulatedOnline(false)
                setRoutingConfig(RoutingConfig())
                setAlertLevel(AlertLevel.WARNING)
            }.onSuccess {
                _events.trySend(DebugEvent.ResetDone)
            }.onFailure {
                _state.update { it.copy(hasError = true) }
            }
        }
    }
}
