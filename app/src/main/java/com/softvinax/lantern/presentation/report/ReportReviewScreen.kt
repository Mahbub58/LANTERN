package com.softvinax.lantern.presentation.report

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.softvinax.lantern.R
import com.softvinax.lantern.domain.model.AppLanguage
import com.softvinax.lantern.domain.model.EmergencyType
import com.softvinax.lantern.domain.model.Need
import com.softvinax.lantern.domain.model.ReportDraft
import com.softvinax.lantern.domain.model.ReportLocation
import com.softvinax.lantern.presentation.components.EmptyState
import com.softvinax.lantern.presentation.components.LanternAppBar
import com.softvinax.lantern.presentation.components.LanternBottomBar
import com.softvinax.lantern.presentation.components.LanternPreview
import com.softvinax.lantern.presentation.components.LanternTab
import com.softvinax.lantern.presentation.components.LanternTextField
import com.softvinax.lantern.presentation.components.LoadingState
import com.softvinax.lantern.presentation.components.LocationCard
import com.softvinax.lantern.presentation.components.NeedGrid
import com.softvinax.lantern.presentation.components.NeedPill
import com.softvinax.lantern.presentation.components.OutlinePillButton
import com.softvinax.lantern.presentation.components.PrimaryGradientButton
import com.softvinax.lantern.presentation.components.ReportHeader
import com.softvinax.lantern.presentation.components.ReviewDivider
import com.softvinax.lantern.presentation.components.ReviewRow
import com.softvinax.lantern.presentation.components.StructuredPill
import com.softvinax.lantern.presentation.components.UrgencyPill
import com.softvinax.lantern.presentation.components.needLabel
import com.softvinax.lantern.presentation.components.typeLabel
import com.softvinax.lantern.presentation.theme.LanternTheme
import java.util.Locale
import kotlin.math.abs

@Composable
fun ReportReviewScreenRoute(
    onBack: () -> Unit,
    onOpenConfirmation: (String) -> Unit,
    onStartNewReport: () -> Unit,
    onOpenSettings: () -> Unit,
    onTabSelected: (LanternTab) -> Unit,
    viewModel: ReportReviewViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        if (result.values.any { it }) {
            viewModel.onLocationPermissionGranted()
        } else {
            viewModel.onLocationPermissionDenied()
        }
    }

    BackHandler(enabled = state.step == ReportReviewStep.REVIEW) {
        viewModel.editStep()
    }

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
        onToggleShareLocation = { enabled ->
            if (!enabled) {
                viewModel.disableLocation()
            } else {
                val fineGranted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                ) == PackageManager.PERMISSION_GRANTED
                if (fineGranted) {
                    viewModel.onLocationPermissionGranted()
                } else {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                        ),
                    )
                }
            }
        },
        onToggleNeed = viewModel::toggleNeed,
        onPlaceChange = viewModel::setPlace,
        onNext = viewModel::next,
        onEdit = viewModel::editStep,
        onSend = viewModel::send,
        onStartNewReport = viewModel::startNewReport,
        onOpenSettings = onOpenSettings,
        onTabSelected = { tab ->
            viewModel.saveDraftNow()
            onTabSelected(tab)
        },
    )
}

@Composable
fun ReportReviewScreen(
    state: ReportReviewUiState,
    onBack: () -> Unit,
    onToggleShareLocation: (Boolean) -> Unit,
    onToggleNeed: (Need) -> Unit,
    onPlaceChange: (String) -> Unit,
    onNext: () -> Unit,
    onEdit: () -> Unit,
    onSend: () -> Unit,
    onStartNewReport: () -> Unit,
    onOpenSettings: () -> Unit,
    onTabSelected: (LanternTab) -> Unit,
) {
    val colors = LanternTheme.colors
    val draft = state.draft
    val currentStep = if (state.step == ReportReviewStep.REVIEW) 3 else 2

    Scaffold(
        containerColor = colors.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            LanternAppBar(
                isOnline = state.isOnline,
                onOpenSettings = onOpenSettings,
            )
        },
        bottomBar = {
            LanternBottomBar(
                selectedTab = LanternTab.REPORT,
                onSelect = onTabSelected,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .background(colors.background),
        ) {
            ReportHeader(currentStep = currentStep)

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

            else -> Column(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    if (state.step == ReportReviewStep.DETAILS) {
                        DetailsStep(
                            state = state,
                            draft = draft,
                            onToggleNeed = onToggleNeed,
                            onPlaceChange = onPlaceChange,
                            onToggleShareLocation = onToggleShareLocation,
                        )
                    } else {
                        ReviewStep(
                            state = state,
                            draft = draft,
                        )
                    }
                    ReportActions(
                        state = state,
                        step = state.step,
                        onBack = onBack,
                        onNext = onNext,
                        onEdit = onEdit,
                        onSend = onSend,
                    )
                }
            }
        }
    }
    }
}

@Composable
private fun DetailsStep(
    state: ReportReviewUiState,
    draft: ReportDraft,
    onToggleNeed: (Need) -> Unit,
    onPlaceChange: (String) -> Unit,
    onToggleShareLocation: (Boolean) -> Unit,
) {
    val colors = LanternTheme.colors

    Text(
        text = stringResource(R.string.report_what_need),
        style = MaterialTheme.typography.headlineMedium,
        color = colors.textPrimary,
    )

    NeedGrid(selected = draft.needs, onToggle = onToggleNeed)

    Text(
        text = stringResource(R.string.report_where_are_you),
        style = MaterialTheme.typography.headlineSmall,
        color = colors.textPrimary,
    )

    LanternTextField(
        value = draft.placeDescription,
        onValueChange = onPlaceChange,
        label = null,
        placeholder = stringResource(R.string.report_place_placeholder),
        minLines = 3,
        maxLines = 4,
    )

    LocationCard(
        checked = draft.location != null,
        onCheckedChange = onToggleShareLocation,
        subtitle = locationSubtitle(state, draft),
        enabled = !state.isLocating,
    )

    if (state.locationDenied) {
        Text(
            text = stringResource(R.string.report_location_denied),
            style = MaterialTheme.typography.bodySmall,
            color = colors.amber,
        )
    }

    if (!state.isOnline) {
        Text(
            text = stringResource(R.string.report_offline_note),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
        )
    }

    if (state.hasError) {
        Text(
            text = stringResource(R.string.review_error),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.amber,
        )
    }

}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReviewStep(
    state: ReportReviewUiState,
    draft: ReportDraft,
) {
    val colors = LanternTheme.colors
    val shape = RoundedCornerShape(24.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.border, shape)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        UrgencyPill(urgency = draft.urgency)
        StructuredPill()
        Spacer(Modifier.height(4.dp))

        ReviewRow(label = stringResource(R.string.report_type_label)) {
            Text(
                text = typeLabel(draft.type),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.textPrimary,
            )
        }
        ReviewDivider()
        ReviewRow(label = stringResource(R.string.report_people_label)) {
            Text(
                text = "${draft.peopleCount}",
                style = MaterialTheme.typography.bodyLarge,
                color = colors.textPrimary,
            )
        }
        ReviewDivider()
        ReviewRow(label = stringResource(R.string.report_needs_label)) {
            if (draft.needs.isEmpty()) {
                Text(
                    text = stringResource(R.string.report_needs_none),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    draft.needs.forEach { need -> NeedPill(need) }
                }
            }
        }
        ReviewDivider()
        ReviewRow(label = stringResource(R.string.report_place_label)) {
            Text(
                text = placeText(draft),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textPrimary,
                textAlign = TextAlign.End,
            )
        }
    }

    if (!state.isOnline) {
        Text(
            text = stringResource(R.string.report_offline_note),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
        )
    }

    if (state.hasError) {
        Text(
            text = stringResource(R.string.review_error),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.amber,
        )
    }

}

@Composable
private fun ReportActions(
    state: ReportReviewUiState,
    step: ReportReviewStep,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onEdit: () -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (step == ReportReviewStep.DETAILS) {
            OutlinePillButton(
                text = stringResource(R.string.report_back),
                onClick = onBack,
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                modifier = Modifier.weight(0.38f),
            )
            PrimaryGradientButton(
                text = stringResource(R.string.report_review),
                onClick = onNext,
                loading = state.isSaving,
                enabled = !state.isSaving,
                modifier = Modifier.weight(0.62f),
                glow = true,
            )
        } else {
            OutlinePillButton(
                text = stringResource(R.string.report_edit),
                onClick = onEdit,
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                modifier = Modifier.weight(0.35f),
            )
            PrimaryGradientButton(
                text = stringResource(R.string.report_check_send),
                onClick = onSend,
                icon = Icons.AutoMirrored.Filled.Send,
                loading = state.isSending,
                enabled = !state.isSending,
                modifier = Modifier.weight(0.65f),
                glow = true,
            )
        }
    }
}

@Composable
private fun locationSubtitle(
    state: ReportReviewUiState,
    draft: ReportDraft,
): String {
    val location = draft.location
    return when {
        state.isLocating -> stringResource(R.string.report_location_locating)
        location != null -> "${formatCoordinates(location)} · ${stringResource(R.string.report_location_cached)}"
        state.locationDenied -> stringResource(R.string.report_location_denied)
        else -> stringResource(R.string.report_location_off)
    }
}

@Composable
private fun placeText(draft: ReportDraft): String {
    val location = draft.location
    return when {
        location != null -> stringResource(
            R.string.report_location_coords,
            formatCoordinates(location),
        )
        draft.placeDescription.isNotBlank() -> draft.placeDescription
        else -> "—"
    }
}

private fun formatCoordinates(location: ReportLocation): String {
    val latDirection = if (location.latitude >= 0) "N" else "S"
    val lonDirection = if (location.longitude >= 0) "E" else "W"
    return String.format(
        Locale.US,
        "%.4f° %s, %.4f° %s",
        abs(location.latitude),
        latDirection,
        abs(location.longitude),
        lonDirection,
    )
}

@Preview(name = "Report step 2 — details", widthDp = 390, heightDp = 844)
@Composable
private fun ReviewDetailsPreview() {
    LanternPreview {
        ReportReviewScreen(
            state = ReportReviewUiState(
                isLoading = false,
                isOnline = false,
                draft = previewDraft(),
                step = ReportReviewStep.DETAILS,
            ),
            onBack = {},
            onToggleShareLocation = {},
            onToggleNeed = {},
            onPlaceChange = {},
            onNext = {},
            onEdit = {},
            onSend = {},
            onStartNewReport = {},
            onOpenSettings = {},
            onTabSelected = {},
        )
    }
}

@Preview(name = "Report step 3 — review", widthDp = 390, heightDp = 844)
@Composable
private fun ReviewSummaryPreview() {
    LanternPreview {
        ReportReviewScreen(
            state = ReportReviewUiState(
                isLoading = false,
                isOnline = true,
                draft = previewDraft(),
                step = ReportReviewStep.REVIEW,
            ),
            onBack = {},
            onToggleShareLocation = {},
            onToggleNeed = {},
            onPlaceChange = {},
            onNext = {},
            onEdit = {},
            onSend = {},
            onStartNewReport = {},
            onOpenSettings = {},
            onTabSelected = {},
        )
    }
}

@Preview(name = "Report — draft missing", widthDp = 390, heightDp = 844)
@Composable
private fun ReviewMissingPreview() {
    LanternPreview {
        ReportReviewScreen(
            state = ReportReviewUiState(isLoading = false, notFound = true),
            onBack = {},
            onToggleShareLocation = {},
            onToggleNeed = {},
            onPlaceChange = {},
            onNext = {},
            onEdit = {},
            onSend = {},
            onStartNewReport = {},
            onOpenSettings = {},
            onTabSelected = {},
        )
    }
}

internal fun previewDraft() = ReportDraft(
    id = "draft-1",
    description = "Earthquake. Needs: Rescue, Shelter, Medical help. Place: Block B, near the mosque",
    language = AppLanguage.EN,
    type = EmergencyType.EARTHQUAKE,
    urgency = 5,
    peopleCount = 6,
    needs = setOf(Need.RESCUE, Need.SHELTER, Need.MEDICAL),
    placeDescription = "Block B, near the mosque",
    createdAt = System.currentTimeMillis() - 5 * 60_000L,
)
