package com.mahbub.lantern.presentation.report

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mahbub.lantern.R
import com.mahbub.lantern.domain.model.AppLanguage
import com.mahbub.lantern.domain.model.EmergencyType
import com.mahbub.lantern.presentation.components.LanternAppBar
import com.mahbub.lantern.presentation.components.LanternBottomBar
import com.mahbub.lantern.presentation.components.LanternPreview
import com.mahbub.lantern.presentation.components.LanternTab
import com.mahbub.lantern.presentation.components.LanternTextButton
import com.mahbub.lantern.presentation.components.PeopleCard
import com.mahbub.lantern.presentation.components.PrimaryGradientButton
import com.mahbub.lantern.presentation.components.ReportHeader
import com.mahbub.lantern.presentation.components.TypeGrid
import com.mahbub.lantern.presentation.theme.LanternTheme

@Composable
fun ReportScreenRoute(
    onOpenReview: (String) -> Unit,
    onOpenMyReports: () -> Unit,
    onOpenSettings: () -> Unit,
    onTabSelected: (LanternTab) -> Unit,
    viewModel: ReportViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            if (event is ReportEvent.OpenReview) onOpenReview(event.draftId)
        }
    }
    ReportScreen(
        state = state,
        onTypeChange = viewModel::setType,
        onPeopleChange = viewModel::setPeopleCount,
        onContinue = viewModel::continueToReview,
        onOpenMyReports = onOpenMyReports,
        onOpenSettings = onOpenSettings,
        onTabSelected = onTabSelected,
    )
}

@Composable
fun ReportScreen(
    state: ReportUiState,
    onTypeChange: (EmergencyType) -> Unit,
    onPeopleChange: (Int) -> Unit,
    onContinue: () -> Unit,
    onOpenMyReports: () -> Unit,
    onOpenSettings: () -> Unit,
    onTabSelected: (LanternTab) -> Unit,
) {
    val colors = LanternTheme.colors
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
            LanternBottomBar(selectedTab = LanternTab.REPORT, onSelect = onTabSelected)
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            ReportHeader(currentStep = 1)

            Text(
                text = stringResource(R.string.report_what_happening),
                style = MaterialTheme.typography.headlineMedium,
                color = colors.textPrimary,
            )

            TypeGrid(
                selected = state.type,
                onSelect = onTypeChange,
            )

            PeopleCard(
                count = state.peopleCount,
                onDecrease = { onPeopleChange(state.peopleCount - 1) },
                onIncrease = { onPeopleChange(state.peopleCount + 1) },
            )

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

            PrimaryGradientButton(
                text = stringResource(R.string.report_next_needs),
                onClick = onContinue,
                loading = state.isSaving,
                enabled = !state.isSaving,
                glow = true,
            )

            LanternTextButton(
                text = stringResource(R.string.my_reports),
                onClick = onOpenMyReports,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Preview(name = "Report step 1 — online", widthDp = 390, heightDp = 844)
@Composable
private fun ReportPreview() {
    LanternPreview {
        ReportScreen(
            state = ReportUiState(
                type = EmergencyType.FLOOD,
                peopleCount = 2,
                isOnline = true,
                language = AppLanguage.EN,
            ),
            onTypeChange = {},
            onPeopleChange = {},
            onContinue = {},
            onOpenMyReports = {},
            onOpenSettings = {},
            onTabSelected = {},
        )
    }
}

@Preview(name = "Report step 1 — offline", widthDp = 390, heightDp = 844)
@Composable
private fun ReportOfflinePreview() {
    LanternPreview {
        ReportScreen(
            state = ReportUiState(
                type = EmergencyType.EARTHQUAKE,
                peopleCount = 1,
                isOnline = false,
                language = AppLanguage.EN,
            ),
            onTypeChange = {},
            onPeopleChange = {},
            onContinue = {},
            onOpenMyReports = {},
            onOpenSettings = {},
            onTabSelected = {},
        )
    }
}
