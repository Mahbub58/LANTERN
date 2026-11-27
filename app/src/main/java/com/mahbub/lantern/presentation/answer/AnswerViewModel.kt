package com.mahbub.lantern.presentation.answer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahbub.lantern.domain.model.Answer
import com.mahbub.lantern.domain.model.AppLanguage
import com.mahbub.lantern.domain.model.InferenceSource
import com.mahbub.lantern.domain.usecase.AskLanternUseCase
import com.mahbub.lantern.domain.usecase.ObserveConnectivityUseCase
import com.mahbub.lantern.domain.usecase.ObserveLanguageUseCase
import com.mahbub.lantern.domain.usecase.ObserveRoutingConfigUseCase
import com.mahbub.lantern.presentation.navigation.routeString
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AnswerUiState(
    val question: String = "",
    val isLoading: Boolean = true,
    val answer: Answer? = null,
    val isOnline: Boolean = false,
    val cloudFallbackEnabled: Boolean = true,
    val isAskingDeeper: Boolean = false,
    val hasError: Boolean = false,
    val deeperFailed: Boolean = false,
    val language: AppLanguage = AppLanguage.EN,
) {
    val canUseCloud: Boolean get() = isOnline && cloudFallbackEnabled

    val showDeeperButton: Boolean
        get() = answer != null &&
            !answer.isNoMatch &&
            answer.source != InferenceSource.CLOUD
}

sealed interface AnswerEvent {
    data class FileReport(val question: String) : AnswerEvent
    data object AskAnother : AnswerEvent
}

@HiltViewModel
class AnswerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val askLantern: AskLanternUseCase,
    observeConnectivity: ObserveConnectivityUseCase,
    observeRoutingConfig: ObserveRoutingConfigUseCase,
    observeLanguage: ObserveLanguageUseCase,
) : ViewModel() {

    private val question = savedStateHandle.routeString("question")
    private val _state = MutableStateFlow(AnswerUiState(question = question))
    val state: StateFlow<AnswerUiState> = _state.asStateFlow()

    private val _events = Channel<AnswerEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            observeConnectivity().collect { online ->
                _state.update { it.copy(isOnline = online) }
            }
        }
        viewModelScope.launch {
            observeRoutingConfig().collect { config ->
                _state.update { it.copy(cloudFallbackEnabled = config.cloudFallbackEnabled) }
            }
        }
        viewModelScope.launch {
            observeLanguage().collect { language ->
                _state.update { it.copy(language = language) }
            }
        }
        ask()
    }

    fun ask() {
        loadJob?.cancel()
        _state.update {
            it.copy(
                isLoading = true,
                hasError = false,
                deeperFailed = false,
                isAskingDeeper = false,
            )
        }
        loadJob = viewModelScope.launch {
            runCatching { askLantern(_state.value.question) }
                .onSuccess { answer ->
                    _state.update { it.copy(isLoading = false, answer = answer, hasError = false) }
                }
                .onFailure {
                    _state.update { it.copy(isLoading = false, hasError = true) }
                }
        }
    }

    fun askDeeper() {
        val current = _state.value
        if (!current.canUseCloud || current.isAskingDeeper) return
        _state.update { it.copy(isAskingDeeper = true, deeperFailed = false) }
        viewModelScope.launch {
            runCatching { askLantern(current.question, deeper = true) }
                .onSuccess { answer ->
                    _state.update {
                        it.copy(
                            isAskingDeeper = false,
                            answer = answer,
                            deeperFailed = answer.source != InferenceSource.CLOUD,
                        )
                    }
                }
                .onFailure {
                    _state.update { it.copy(isAskingDeeper = false, deeperFailed = true) }
                }
        }
    }

    fun fileReport() {
        viewModelScope.launch { _events.send(AnswerEvent.FileReport(_state.value.question)) }
    }

    fun askAnother() {
        viewModelScope.launch { _events.send(AnswerEvent.AskAnother) }
    }
}
