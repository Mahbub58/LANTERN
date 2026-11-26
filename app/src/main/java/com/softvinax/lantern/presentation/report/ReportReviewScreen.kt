package com.softvinax.lantern.presentation.report

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.softvinax.lantern.R
import com.softvinax.lantern.domain.model.AppLanguage
import com.softvinax.lantern.domain.model.EmergencyType
import com.softvinax.lantern.domain.model.Need
import com.softvinax.lantern.domain.model.ReportDraft
import com.softvinax.lantern.presentation.components.EmptyState
import com.softvinax.lantern.presentation.components.LanternCard
import com.softvinax.lantern.presentation.components.LanternPreview
import com.softvinax.lantern.presentation.components.LanternTextField
import com.softvinax.lantern.presentation.components.LanternTopBar
import com.softvinax.lantern.presentation.components.LoadingState
import com.softvinax.lantern.presentation.components.NeedSelector
import com.softvinax.lantern.presentation.components.OfflineNotice
import com.softvinax.lantern.presentation.components.PeopleStepper
import com.softvinax.lantern.presentation.components.PrimaryGradientButton
import com.softvinax.lantern.presentation.components.SectionLabel
import com.softvinax.lantern.presentation.components.SignalChip
import com.softvinax.lantern.presentation.components.StepIndicator
import com.softvinax.lantern.presentation.components.TypeSelector
import com.softvinax.lantern.presentation.components.UrgencySelector
import com.softvinax.lantern.presentation.theme.LanternTheme

@Composable
fun ReportReviewScreenRoute(
    onBack: () -> Unit,
    onOpenConfirmation: (String) -> Unit,
    onStartNewReport: () -> Unit,
    viewModel: ReportReviewViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ReportReviewEvent.OpenConfirmation -> onOpenConfirmation(event.reportId)
                ReportReviewEvent.StartNewReport -> onStartNewReport()
            }
        }
    }
    ReportReviewScreen(
        state = state,
        onBack = onBack,
        onTypeChange = viewModel::setType,
        onUrgencyChange = viewModel::setUrgency,
        onPeopleChange = viewModel::setPeopleCount,
        onToggleNeed = viewModel::toggleNeed,
        onPlaceChange = viewModel::setPlace,
        onSend = viewModel::send,
        onStartNewReport = viewModel::startNewReport,
    )
}

@Composable
fun ReportReviewScreen(
    state: ReportReviewUiState,
    onBack: () -> Unit,
    onTypeChange: (EmergencyType) -> Unit,
    onUrgencyChange: (Int) -> Unit,
    onPeopleChange: (Int) -> Unit,
    onToggleNeed: (Need) -> Unit,
    onPlaceChange: (String) -> Unit,
    onSend: () -> Unit,
    onStartNewReport: () -> Unit,
) {
    val colors = LanternTheme.colors
    val draft = state.draft
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        LanternTopBar(
            title = stringResource(R.string.review_title),
            onBack = onBack,
            actions = { SignalChip(isOnline = state.isOnline) },
        )

        when {
            state.isLoading -> LoadingState(
                message = stringResource(R.string.loading),
                modifier = Modifier.fillMaxWidth(),
            )

            state.notFound || draft == null -> EmptyState(
                title = stringResource(R.string.review_draft_missing),
                body = stringResource(R.string.review_nothing_sent),
                actionLabel = stringResource(R.string.review_draft_missing_action),
                onAction = onStartNewReport,
            )

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                StepIndicator(currentStep = 2)

                Text(
                    text = stringResource(R.string.review_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )

                SectionLabel(text = stringResource(R.string.review_type))
                TypeSelector(selected = draft.type, onSelect = onTypeChange)

                LanternCard {
                    UrgencySelector(value = draft.urgency, onValueChange = onUrgencyChange)
                    Spacer(Modifier.height(12.dp))
                    PeopleStepper(
                        count = draft.peopleCount,
                        onDecrease = { onPeopleChange(draft.peopleCount - 1) },
                        onIncrease = { onPeopleChange(draft.peopleCount + 1) },
                    )
                }

                SectionLabel(text = stringResource(R.string.review_needs))
                NeedSelector(selected = draft.needs, onToggle = onToggleNeed)

                LanternTextField(
                    value = draft.placeDescription,
                    onValueChange = onPlaceChange,
                    label = stringResource(R.string.review_place),
                    placeholder = stringResource(R.string.review_place_placeholder),
                    minLines = 2,
                    maxLines = 3,
                )

                if (!state.isOnline) {
                    OfflineNotice(text = stringResource(R.string.review_send_offline))
                }

                Text(
                    text = stringResource(R.string.review_nothing_sent),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )

                if (state.hasError) {
                    Text(
                        text = stringResource(R.string.review_error),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.amber,
                    )
                }

                PrimaryGradientButton(
                    text = stringResource(
                        if (state.isSending) R.string.review_sending else R.string.review_send,
                    ),
                    onClick = onSend,
                    loading = state.isSending,
                    enabled = !state.isSending,
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Preview(name = "Review — offline draft", widthDp = 390, heightDp = 844)
@Composable
private fun ReviewPreview() {
    LanternPreview {
        ReportReviewScreen(
            state = ReportReviewUiState(
                isLoading = false,
                isOnline = false,
                draft = previewDraft(),
            ),
            onBack = {},
            onTypeChange = {},
            onUrgencyChange = {},
            onPeopleChange = {},
            onToggleNeed = {},
            onPlaceChange = {},
            onSend = {},
            onStartNewReport = {},
        )
    }
}

@Preview(name = "Review — sending", widthDp = 390, heightDp = 844)
@Composable
private fun ReviewSendingPreview() {
    LanternPreview {
        ReportReviewScreen(
            state = ReportReviewUiState(
                isLoading = false,
                isOnline = true,
                isSending = true,
                draft = previewDraft(),
            ),
            onBack = {},
            onTypeChange = {},
            onUrgencyChange = {},
            onPeopleChange = {},
            onToggleNeed = {},
            onPlaceChange = {},
            onSend = {},
            onStartNewReport = {},
        )
    }
}

@Preview(name = "Review — draft missing", widthDp = 390, heightDp = 844)
@Composable
private fun ReviewMissingPreview() {
    LanternPreview {
        ReportReviewScreen(
            state = ReportReviewUiState(isLoading = false, notFound = true),
            onBack = {},
            onTypeChange = {},
            onUrgencyChange = {},
            onPeopleChange = {},
            onToggleNeed = {},
            onPlaceChange = {},
            onSend = {},
            onStartNewReport = {},
        )
    }
}

internal fun previewDraft() = ReportDraft(
    id = "draft-1",
    description = "Roof cracked, two families trapped on the second floor",
    language = AppLanguage.EN,
    type = EmergencyType.EARTHQUAKE,
    urgency = 5,
    peopleCount = 6,
    needs = setOf(Need.RESCUE, Need.SHELTER, Need.MEDICAL),
    placeDescription = "Block B, near the mosque",
    createdAt = System.currentTimeMillis() - 5 * 60_000L,
)
