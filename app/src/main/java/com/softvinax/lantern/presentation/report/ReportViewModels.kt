package com.softvinax.lantern.presentation.report

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softvinax.lantern.domain.model.AppLanguage
import com.softvinax.lantern.domain.model.EmergencyType
import com.softvinax.lantern.domain.model.Need
import com.softvinax.lantern.domain.model.Report
import com.softvinax.lantern.domain.model.ReportDraft
import com.softvinax.lantern.domain.model.ReportLocation
import com.softvinax.lantern.domain.usecase.GetLastKnownLocationUseCase
import com.softvinax.lantern.domain.usecase.GetReportDraftUseCase
import com.softvinax.lantern.domain.usecase.ObserveConnectivityUseCase
import com.softvinax.lantern.domain.usecase.ObserveLanguageUseCase
import com.softvinax.lantern.domain.usecase.ObserveReportUseCase
import com.softvinax.lantern.domain.usecase.ObserveMyReportsUseCase
import com.softvinax.lantern.domain.usecase.SaveReportDraftUseCase
import com.softvinax.lantern.domain.usecase.StructureReportUseCase
import com.softvinax.lantern.domain.usecase.SubmitReportUseCase
import com.softvinax.lantern.presentation.navigation.routeString
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

// ---------------------------------------------------------------------------
// Step 1 — describe
// ---------------------------------------------------------------------------

data class ReportUiState(
    val description: String = "",
    val isOnline: Boolean = false,
    val language: AppLanguage = AppLanguage.EN,
    val shareLocation: Boolean = false,
    val location: ReportLocation? = null,
    val locationLabel: String? = null,
    val isLocating: Boolean = false,
    val isSaving: Boolean = false,
    val hasError: Boolean = false,
    val blankError: Boolean = false,
    val locationDenied: Boolean = false,
)

sealed interface ReportEvent {
    data class OpenReview(val draftId: String) : ReportEvent
}

@HiltViewModel
class ReportViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val structureReport: StructureReportUseCase,
    private val saveDraft: SaveReportDraftUseCase,
    private val getLastKnownLocation: GetLastKnownLocationUseCase,
    observeConnectivity: ObserveConnectivityUseCase,
    observeLanguage: ObserveLanguageUseCase,
) : ViewModel() {

    private val prefill = savedStateHandle.routeString("prefill")
    private val _state = MutableStateFlow(ReportUiState(description = prefill))
    val state: StateFlow<ReportUiState> = _state.asStateFlow()

    private val _events = Channel<ReportEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            combine(observeConnectivity(), observeLanguage()) { online, language ->
                online to language
            }.collect { (online, language) ->
                _state.update { it.copy(isOnline = online, language = language) }
            }
        }
    }

    fun onDescriptionChange(description: String) {
        _state.update { it.copy(description = description, blankError = false) }
    }

    fun onLocationPermissionGranted() {
        if (_state.value.isLocating) return
        _state.update { it.copy(isLocating = true, locationDenied = false) }
        viewModelScope.launch {
            runCatching { getLastKnownLocation() }
                .onSuccess { location ->
                    _state.update {
                        it.copy(
                            isLocating = false,
                            shareLocation = location != null,
                            location = location,
                            locationLabel = location?.placeLabel,
                        )
                    }
                }
                .onFailure {
                    _state.update {
                        it.copy(isLocating = false, shareLocation = false, locationDenied = true)
                    }
                }
        }
    }

    fun onLocationPermissionDenied() {
        _state.update {
            it.copy(shareLocation = false, location = null, locationLabel = null, locationDenied = true)
        }
    }

    fun disableLocation() {
        _state.update {
            it.copy(shareLocation = false, location = null, locationLabel = null, locationDenied = false)
        }
    }

    fun continueToReview() {
        val current = _state.value
        if (current.description.trim().isEmpty()) {
            _state.update { it.copy(blankError = true) }
            return
        }
        if (current.isSaving) return
        _state.update { it.copy(isSaving = true, hasError = false) }

        viewModelScope.launch {
            runCatching {
                val draft = structureReport(
                    description = current.description,
                    language = current.language,
                    location = if (current.shareLocation) current.location else null,
                )
                saveDraft(draft)
            }.onSuccess { saved ->
                _state.update { it.copy(isSaving = false) }
                _events.trySend(ReportEvent.OpenReview(saved.id))
            }.onFailure {
                _state.update { it.copy(isSaving = false, hasError = true) }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Step 2 — review + send
// ---------------------------------------------------------------------------

data class ReportReviewUiState(
    val isLoading: Boolean = true,
    val draft: ReportDraft? = null,
    val isOnline: Boolean = false,
    val isSending: Boolean = false,
    val hasError: Boolean = false,
    val notFound: Boolean = false,
)

sealed interface ReportReviewEvent {
    data class OpenConfirmation(val reportId: String) : ReportReviewEvent
    data object StartNewReport : ReportReviewEvent
}

@HiltViewModel
class ReportReviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getDraft: GetReportDraftUseCase,
    private val submitReport: SubmitReportUseCase,
    observeConnectivity: ObserveConnectivityUseCase,
) : ViewModel() {

    private val draftId = savedStateHandle.routeString("draftId")
    private val _state = MutableStateFlow(ReportReviewUiState())
    val state: StateFlow<ReportReviewUiState> = _state.asStateFlow()

    private val _events = Channel<ReportReviewEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            val draft = runCatching { getDraft(draftId) }.getOrNull()
            _state.update {
                it.copy(
                    isLoading = false,
                    draft = draft,
                    notFound = draft == null,
                )
            }
        }
        viewModelScope.launch {
            observeConnectivity().collect { online ->
                _state.update { it.copy(isOnline = online) }
            }
        }
    }

    fun setType(type: EmergencyType) = edit { it.copy(type = type) }

    fun setUrgency(urgency: Int) = edit { it.copy(urgency = urgency.coerceIn(1, 5)) }

    fun setPeopleCount(count: Int) = edit { it.copy(peopleCount = count.coerceIn(1, 99)) }

    fun toggleNeed(need: Need) = edit {
        it.copy(needs = if (need in it.needs) it.needs - need else it.needs + need)
    }

    fun setPlace(place: String) = edit { it.copy(placeDescription = place) }

    private fun edit(transform: (ReportDraft) -> ReportDraft) {
        _state.update { current ->
            val draft = current.draft ?: return@update current
            current.copy(draft = transform(draft), hasError = false)
        }
    }

    fun send() {
        val current = _state.value
        val draft = current.draft ?: return
        if (current.isSending) return
        _state.update { it.copy(isSending = true, hasError = false) }
        viewModelScope.launch {
            runCatching { submitReport(draft) }
                .onSuccess { report: Report ->
                    _state.update { it.copy(isSending = false) }
                    _events.trySend(ReportReviewEvent.OpenConfirmation(report.id))
                }
                .onFailure {
                    _state.update { it.copy(isSending = false, hasError = true) }
                }
        }
    }

    fun startNewReport() {
        viewModelScope.launch { _events.send(ReportReviewEvent.StartNewReport) }
    }
}

// ---------------------------------------------------------------------------
// Step 3 — confirmation + recent reports
// ---------------------------------------------------------------------------

data class ReportConfirmationUiState(
    val isLoading: Boolean = true,
    val report: Report? = null,
    val reports: List<Report> = emptyList(),
    val isOnline: Boolean = false,
    val hasError: Boolean = false,
) {
    val notFound: Boolean get() = !isLoading && report == null
}

@HiltViewModel
class ReportConfirmationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeReport: ObserveReportUseCase,
    observeMyReports: ObserveMyReportsUseCase,
    observeConnectivity: ObserveConnectivityUseCase,
) : ViewModel() {

    private val reportId = savedStateHandle.routeString("reportId")
    private val _state = MutableStateFlow(ReportConfirmationUiState())
    val state: StateFlow<ReportConfirmationUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                combine(
                    observeReport(reportId),
                    observeMyReports(),
                    observeConnectivity(),
                ) { report, reports, online ->
                    Triple(report, reports, online)
                }.collect { (report, reports, online) ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            report = report,
                            reports = reports,
                            isOnline = online,
                            hasError = false,
                        )
                    }
                }
            } catch (t: Throwable) {
                _state.update { it.copy(isLoading = false, hasError = true) }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// My reports
// ---------------------------------------------------------------------------

data class MyReportsUiState(
    val isLoading: Boolean = true,
    val reports: List<Report> = emptyList(),
    val isOnline: Boolean = false,
    val hasError: Boolean = false,
)

sealed interface MyReportsEvent {
    data object FileReport : MyReportsEvent
}

@HiltViewModel
class MyReportsViewModel @Inject constructor(
    private val observeMyReports: ObserveMyReportsUseCase,
    private val observeConnectivity: ObserveConnectivityUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(MyReportsUiState())
    val state: StateFlow<MyReportsUiState> = _state.asStateFlow()

    private val _events = Channel<MyReportsEvent>(Channel.BUFFERED)
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
                combine(observeMyReports(), observeConnectivity()) { reports, online ->
                    reports to online
                }.collect { (reports, online) ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            reports = reports,
                            isOnline = online,
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

    fun fileReport() {
        viewModelScope.launch { _events.send(MyReportsEvent.FileReport) }
    }
}
