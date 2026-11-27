package com.mahbub.lantern.presentation.report

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mahbub.lantern.R
import com.mahbub.lantern.domain.model.AppLanguage
import com.mahbub.lantern.domain.model.EmergencyType
import com.mahbub.lantern.domain.model.Need
import com.mahbub.lantern.domain.model.Report
import com.mahbub.lantern.domain.model.ReportStatus
import com.mahbub.lantern.domain.model.SyncState
import com.mahbub.lantern.presentation.components.EmptyState
import com.mahbub.lantern.presentation.components.LanternAppBar
import com.mahbub.lantern.presentation.components.LanternCard
import com.mahbub.lantern.presentation.components.LanternPreview
import com.mahbub.lantern.presentation.components.LanternTextButton
import com.mahbub.lantern.presentation.components.LoadingState
import com.mahbub.lantern.presentation.components.PrimaryGradientButton
import com.mahbub.lantern.presentation.theme.LanternTheme

@Composable
fun ReportConfirmationScreenRoute(
    onViewReports: () -> Unit,
    onDone: () -> Unit,
    onNewReport: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: ReportConfirmationViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ReportConfirmationScreen(
        state = state,
        onViewReports = onViewReports,
        onDone = onDone,
        onNewReport = onNewReport,
        onOpenSettings = onOpenSettings,
    )
}

@Composable
fun ReportConfirmationScreen(
    state: ReportConfirmationUiState,
    onViewReports: () -> Unit,
    onDone: () -> Unit,
    onNewReport: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val colors = LanternTheme.colors

    Scaffold(
        containerColor = colors.background,
        topBar = {
            LanternAppBar(
                isOnline = state.isOnline,
                onOpenSettings = onOpenSettings,
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(colors.background),
        ) {
            when {
                state.isLoading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    LoadingState(message = stringResource(R.string.confirm_loading))
                }

                state.notFound -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    EmptyState(
                        title = stringResource(R.string.confirm_not_found),
                        body = stringResource(R.string.reports_empty_body),
                        actionLabel = stringResource(R.string.confirm_new_report),
                        onAction = onNewReport,
                    )
                }

                else -> ConfirmationContent(
                    report = state.report,
                    onViewReports = onViewReports,
                    onDone = onDone,
                    onNewReport = onNewReport,
                )
            }
        }
    }
}

@Composable
private fun ConfirmationContent(
    report: Report?,
    onViewReports: () -> Unit,
    onDone: () -> Unit,
    onNewReport: () -> Unit,
) {
    val colors = LanternTheme.colors
    val sent = report?.syncState == SyncState.SENT
    val accent = if (sent) colors.teal else colors.amber

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(CircleShape)
                .background(if (sent) colors.tealTint else colors.amberTint),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (sent) Icons.Filled.Check else Icons.Filled.Refresh,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(42.dp),
            )
        }

        Text(
            text = stringResource(if (sent) R.string.confirm_sent else R.string.confirm_queued),
            style = MaterialTheme.typography.headlineSmall,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
        )

        Text(
            text = stringResource(
                if (sent) R.string.confirm_sent_body else R.string.confirm_queued_body,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
        )

        LanternCard {
            Text(
                text = stringResource(R.string.confirm_report_id, report?.id?.take(8) ?: "—"),
                style = MaterialTheme.typography.labelMedium,
                color = colors.textSecondary,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = report?.description.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textPrimary,
            )
        }

        Spacer(Modifier.height(4.dp))
        PrimaryGradientButton(
            text = stringResource(R.string.confirm_view_reports),
            onClick = onViewReports,
        )
        LanternTextButton(
            text = stringResource(R.string.confirm_new_report),
            onClick = onNewReport,
            modifier = Modifier.fillMaxWidth(),
        )
        LanternTextButton(
            text = stringResource(R.string.confirm_done),
            onClick = onDone,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(name = "Confirmation — queued", widthDp = 390, heightDp = 844)
@Composable
private fun ConfirmationQueuedPreview() {
    LanternPreview {
        ReportConfirmationScreen(
            state = ReportConfirmationUiState(
                isLoading = false,
                report = previewReport(SyncState.QUEUED),
            ),
            onViewReports = {},
            onDone = {},
            onNewReport = {},
            onOpenSettings = {},
        )
    }
}

@Preview(name = "Confirmation — sent", widthDp = 390, heightDp = 844)
@Composable
private fun ConfirmationSentPreview() {
    LanternPreview {
        ReportConfirmationScreen(
            state = ReportConfirmationUiState(
                isLoading = false,
                report = previewReport(SyncState.SENT),
            ),
            onViewReports = {},
            onDone = {},
            onNewReport = {},
            onOpenSettings = {},
        )
    }
}

@Preview(name = "Confirmation — not found", widthDp = 390, heightDp = 844)
@Composable
private fun ConfirmationMissingPreview() {
    LanternPreview {
        ReportConfirmationScreen(
            state = ReportConfirmationUiState(isLoading = false, report = null),
            onViewReports = {},
            onDone = {},
            onNewReport = {},
            onOpenSettings = {},
        )
    }
}

internal fun previewReport(syncState: SyncState) = Report(
    id = "rep-8f2c19aa",
    description = "Roof cracked, two families trapped on the second floor",
    language = AppLanguage.EN,
    type = EmergencyType.EARTHQUAKE,
    urgency = 5,
    peopleCount = 6,
    needs = setOf(Need.RESCUE, Need.SHELTER),
    placeDescription = "Block B, near the mosque",
    location = null,
    syncState = syncState,
    status = ReportStatus.NEW,
    createdAt = System.currentTimeMillis() - 2 * 60_000L,
    updatedAt = System.currentTimeMillis() - 60_000L,
    sentAt = if (syncState == SyncState.SENT) System.currentTimeMillis() - 60_000L else null,
)
