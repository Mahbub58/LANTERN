package com.softvinax.lantern.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softvinax.lantern.domain.ai.NanoStatus
import com.softvinax.lantern.domain.model.AppLanguage
import com.softvinax.lantern.domain.model.EmergencyNumber
import com.softvinax.lantern.domain.usecase.GetAnonymousUidUseCase
import com.softvinax.lantern.domain.usecase.GetEmergencyNumbersUseCase
import com.softvinax.lantern.domain.usecase.ObserveCardsVersionUseCase
import com.softvinax.lantern.domain.usecase.ObserveConnectivityUseCase
import com.softvinax.lantern.domain.usecase.ObserveLanguageUseCase
import com.softvinax.lantern.domain.usecase.ObserveNanoStatusUseCase
import com.softvinax.lantern.domain.usecase.SetLanguageUseCase
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

data class SettingsUiState(
    val isLoading: Boolean = true,
    val language: AppLanguage = AppLanguage.EN,
    val nanoStatus: NanoStatus? = null,
    val cardsVersion: String = "",
    val emergencyNumbers: List<EmergencyNumber> = emptyList(),
    val isOnline: Boolean = false,
    val uid: String? = null,
    val isSaving: Boolean = false,
    val hasError: Boolean = false,
)

sealed interface SettingsEvent {
    data class LanguageSaved(val language: AppLanguage) : SettingsEvent
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val observeLanguage: ObserveLanguageUseCase,
    private val observeNanoStatus: ObserveNanoStatusUseCase,
    private val observeCardsVersion: ObserveCardsVersionUseCase,
    private val getEmergencyNumbers: GetEmergencyNumbersUseCase,
    private val observeConnectivity: ObserveConnectivityUseCase,
    private val setLanguage: SetLanguageUseCase,
    private val getAnonymousUid: GetAnonymousUidUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    private val _events = kotlinx.coroutines.channels.Channel<SettingsEvent>(
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
                combine(
                    observeLanguage(),
                    observeNanoStatus(),
                    observeCardsVersion(),
                    getEmergencyNumbers(),
                    observeConnectivity(),
                ) { language, nano, version, numbers, online ->
                    SettingsUiState(
                        isLoading = false,
                        language = language,
                        nanoStatus = nano,
                        cardsVersion = version,
                        emergencyNumbers = numbers,
                        isOnline = online,
                        uid = _state.value.uid,
                        isSaving = _state.value.isSaving,
                    )
                }.collect { next ->
                    _state.update {
                        next.copy(
                            uid = it.uid,
                            isSaving = it.isSaving,
                            hasError = false,
                        )
                    }
                }
            } catch (t: Throwable) {
                _state.update { it.copy(isLoading = false, hasError = true) }
            }
        }
    }

    fun retry() = load()

    fun selectLanguage(language: AppLanguage) {
        if (_state.value.isSaving) return
        _state.update { it.copy(isSaving = true, hasError = false) }
        viewModelScope.launch {
            runCatching { setLanguage(language) }
                .onSuccess {
                    _state.update { it.copy(isSaving = false) }
                    _events.trySend(SettingsEvent.LanguageSaved(language))
                }
                .onFailure {
                    _state.update { it.copy(isSaving = false, hasError = true) }
                }
        }
    }
}
