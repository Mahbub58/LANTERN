package com.softvinax.lantern.presentation.report

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.softvinax.lantern.R
import com.softvinax.lantern.domain.model.AppLanguage
import com.softvinax.lantern.domain.model.ReportLocation
import com.softvinax.lantern.presentation.components.LanternBottomBar
import com.softvinax.lantern.presentation.components.LanternCard
import com.softvinax.lantern.presentation.components.LanternPreview
import com.softvinax.lantern.presentation.components.LanternTab
import com.softvinax.lantern.presentation.components.LanternTextField
import com.softvinax.lantern.presentation.components.LanternTopBar
import com.softvinax.lantern.presentation.components.OfflineNotice
import com.softvinax.lantern.presentation.components.PrimaryGradientButton
import com.softvinax.lantern.presentation.components.ShareLocationRow
import com.softvinax.lantern.presentation.components.SignalChip
import com.softvinax.lantern.presentation.components.StepIndicator
import com.softvinax.lantern.presentation.theme.LanternTheme

@Composable
fun ReportScreenRoute(
    onOpenReview: (String) -> Unit,
    onOpenMyReports: () -> Unit,
    onTabSelected: (LanternTab) -> Unit,
    viewModel: ReportViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        val granted = result.values.any { it }
        if (granted) viewModel.onLocationPermissionGranted() else viewModel.onLocationPermissionDenied()
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            if (event is ReportEvent.OpenReview) onOpenReview(event.draftId)
        }
    }

    ReportScreen(
        state = state,
        onDescriptionChange = viewModel::onDescriptionChange,
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
        onContinue = viewModel::continueToReview,
        onOpenMyReports = onOpenMyReports,
        onTabSelected = onTabSelected,
    )
}

@Composable
fun ReportScreen(
    state: ReportUiState,
    onDescriptionChange: (String) -> Unit,
    onToggleShareLocation: (Boolean) -> Unit,
    onContinue: () -> Unit,
    onOpenMyReports: () -> Unit,
    onTabSelected: (LanternTab) -> Unit,
) {
    val colors = LanternTheme.colors
    Scaffold(
        containerColor = colors.background,
        topBar = {
            LanternTopBar(
                title = stringResource(R.string.report_title),
                actions = { SignalChip(isOnline = state.isOnline) },
            )
        },
        bottomBar = {
            LanternBottomBar(selectedTab = LanternTab.REPORT, onSelect = onTabSelected)
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StepIndicator(currentStep = 1)

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.report_step1_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.textPrimary,
                )
                Text(
                    text = stringResource(R.string.report_step1_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
            }

            LanternTextField(
                value = state.description,
                onValueChange = onDescriptionChange,
                label = stringResource(R.string.report_description_label),
                placeholder = stringResource(R.string.report_description_placeholder),
                minLines = 5,
                maxLines = 8,
                isError = state.blankError,
                supportingText = if (state.blankError) {
                    stringResource(R.string.report_blank_error)
                } else {
                    null
                },
            )

            LanternCard {
                ShareLocationRow(
                    checked = state.shareLocation,
                    onCheckedChange = onToggleShareLocation,
                    label = stringResource(R.string.report_location_toggle),
                    enabled = !state.isLocating,
                )
                when {
                    state.isLocating -> Text(
                        text = stringResource(R.string.report_location_locating),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.teal,
                    )

                    state.locationLabel != null -> Text(
                        text = stringResource(R.string.report_location_ready, state.locationLabel.orEmpty()),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.teal,
                    )

                    state.locationDenied -> Text(
                        text = stringResource(R.string.report_location_denied),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.amber,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.report_location_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
            }

            if (!state.isOnline) {
                OfflineNotice(text = stringResource(R.string.report_offline_note))
            }

            if (state.hasError) {
                Text(
                    text = stringResource(R.string.review_error),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.amber,
                )
            }

            PrimaryGradientButton(
                text = stringResource(R.string.report_continue),
                onClick = onContinue,
                loading = state.isSaving,
                enabled = !state.isSaving,
            )

            com.softvinax.lantern.presentation.components.LanternTextButton(
                text = stringResource(R.string.my_reports),
                onClick = onOpenMyReports,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview(name = "Report step 1 — offline", widthDp = 390, heightDp = 844)
@Composable
private fun ReportPreview() {
    LanternPreview {
        ReportScreen(
            state = ReportUiState(
                description = "",
                isOnline = false,
                language = AppLanguage.EN,
            ),
            onDescriptionChange = {},
            onToggleShareLocation = {},
            onContinue = {},
            onOpenMyReports = {},
            onTabSelected = {},
        )
    }
}

@Preview(name = "Report step 1 — location ready", widthDp = 390, heightDp = 844)
@Composable
private fun ReportLocationPreview() {
    LanternPreview {
        ReportScreen(
            state = ReportUiState(
                description = "Roof cracked, two families trapped on the second floor",
                isOnline = true,
                language = AppLanguage.EN,
                shareLocation = true,
                location = ReportLocation(24.86, 67.01, 30f, "Block B, near the mosque"),
                locationLabel = "Block B, near the mosque",
            ),
            onDescriptionChange = {},
            onToggleShareLocation = {},
            onContinue = {},
            onOpenMyReports = {},
            onTabSelected = {},
        )
    }
}

@Preview(name = "Report step 1 — blank error", widthDp = 390, heightDp = 844)
@Composable
private fun ReportErrorPreview() {
    LanternPreview {
        ReportScreen(
            state = ReportUiState(
                description = "",
                isOnline = false,
                language = AppLanguage.EN,
                blankError = true,
            ),
            onDescriptionChange = {},
            onToggleShareLocation = {},
            onContinue = {},
            onOpenMyReports = {},
            onTabSelected = {},
        )
    }
}
