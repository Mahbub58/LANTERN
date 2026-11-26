package com.softvinax.lantern.presentation.report

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.softvinax.lantern.R
import com.softvinax.lantern.domain.model.AppLanguage
import com.softvinax.lantern.domain.model.Report
import com.softvinax.lantern.domain.model.SyncState
import com.softvinax.lantern.presentation.components.EmptyState
import com.softvinax.lantern.presentation.components.ErrorState
import com.softvinax.lantern.presentation.components.LanternCard
import com.softvinax.lantern.presentation.components.LanternPreview
import com.softvinax.lantern.presentation.components.LanternTopBar
import com.softvinax.lantern.presentation.components.LoadingState
import com.softvinax.lantern.presentation.components.OfflineNotice
import com.softvinax.lantern.presentation.components.PendingSyncChip
import com.softvinax.lantern.presentation.components.PrimaryGradientButton
import com.softvinax.lantern.presentation.components.ReportStatusChip
import com.softvinax.lantern.presentation.components.SignalChip
import com.softvinax.lantern.presentation.components.relativeTime
import com.softvinax.lantern.presentation.components.typeLabel
import com.softvinax.lantern.presentation.theme.LanternTheme

@Composable
fun MyReportsScreenRoute(
    onBack: () -> Unit,
    onFileReport: () -> Unit,
    viewModel: MyReportsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    MyReportsScreen(
        state = state,
        onBack = onBack,
        onRetry = viewModel::retry,
        onFileReport = onFileReport,
    )
}

@Composable
fun MyReportsScreen(
    state: MyReportsUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onFileReport: () -> Unit,
) {
    val colors = LanternTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        LanternTopBar(
            title = stringResource(R.string.reports_title),
            onBack = onBack,
            actions = { SignalChip(isOnline = state.isOnline) },
        )

        when {
            state.isLoading -> LoadingState(
                message = stringResource(R.string.reports_loading),
                modifier = Modifier.fillMaxWidth(),
            )

            state.hasError -> ErrorState(
                onRetry = onRetry,
                body = stringResource(R.string.reports_error),
            )

            state.reports.isEmpty() -> EmptyState(
                title = stringResource(R.string.reports_empty_title),
                body = stringResource(R.string.reports_empty_body),
                actionLabel = stringResource(R.string.reports_file),
                onAction = onFileReport,
            )

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (!state.isOnline) {
                    item {
                        OfflineNotice(text = stringResource(R.string.reports_offline_note))
                    }
                }
                items(state.reports, key = { it.id }) { report ->
                    ReportRow(report = report)
                }
                item {
                    PrimaryGradientButton(
                        text = stringResource(R.string.reports_file),
                        onClick = onFileReport,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ReportRow(report: Report) {
    val colors = LanternTheme.colors
    LanternCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ReportStatusChip(status = report.status)
            Spacer(Modifier.weight(1f))
            if (report.isPendingSync) {
                PendingSyncChip()
            } else {
                Text(
                    text = relativeTime(report.updatedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = report.description,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.textPrimary,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = typeLabel(report.type),
                style = MaterialTheme.typography.labelMedium,
                color = colors.amber,
            )
            Text(
                text = stringResource(R.string.reports_people, report.peopleCount),
                style = MaterialTheme.typography.labelMedium,
                color = colors.textSecondary,
            )
            Text(
                text = stringResource(R.string.reports_urgency, report.urgency),
                style = MaterialTheme.typography.labelMedium,
                color = if (report.urgency >= 4) colors.coral else colors.textSecondary,
            )
        }
        if (report.placeDescription.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = report.placeDescription,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Preview(name = "My reports — offline queue", widthDp = 390, heightDp = 844)
@Composable
private fun MyReportsPreview() {
    LanternPreview {
        MyReportsScreen(
            state = MyReportsUiState(
                isLoading = false,
                isOnline = false,
                reports = listOf(
                    previewReport(SyncState.QUEUED),
                    previewReport(SyncState.SENT).copy(id = "rep-2", syncState = SyncState.SENT),
                ),
            ),
            onBack = {},
            onRetry = {},
            onFileReport = {},
        )
    }
}

@Preview(name = "My reports — empty", widthDp = 390, heightDp = 844)
@Composable
private fun MyReportsEmptyPreview() {
    LanternPreview {
        MyReportsScreen(
            state = MyReportsUiState(isLoading = false, isOnline = true, reports = emptyList()),
            onBack = {},
            onRetry = {},
            onFileReport = {},
        )
    }
}

@Preview(name = "My reports — loading", widthDp = 390, heightDp = 844)
@Composable
private fun MyReportsLoadingPreview() {
    LanternPreview {
        MyReportsScreen(
            state = MyReportsUiState(isLoading = true),
            onBack = {},
            onRetry = {},
            onFileReport = {},
        )
    }
}
