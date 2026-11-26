package com.softvinax.lantern.presentation.alerts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.softvinax.lantern.R
import com.softvinax.lantern.domain.model.Alert
import com.softvinax.lantern.domain.model.AlertLevel
import com.softvinax.lantern.domain.model.AppLanguage
import com.softvinax.lantern.domain.model.LocalizedText
import com.softvinax.lantern.presentation.components.AlertBanner
import com.softvinax.lantern.presentation.components.EmptyState
import com.softvinax.lantern.presentation.components.ErrorState
import com.softvinax.lantern.presentation.components.LanternBottomBar
import com.softvinax.lantern.presentation.components.LanternPreview
import com.softvinax.lantern.presentation.components.LanternTab
import com.softvinax.lantern.presentation.components.LanternTopBar
import com.softvinax.lantern.presentation.components.LoadingState
import com.softvinax.lantern.presentation.components.OfflineNotice
import com.softvinax.lantern.presentation.components.SignalChip
import com.softvinax.lantern.presentation.components.TopBarAction
import com.softvinax.lantern.presentation.components.relativeTime
import com.softvinax.lantern.presentation.theme.LanternTheme

@Composable
fun AlertsScreenRoute(
    onTabSelected: (LanternTab) -> Unit,
    viewModel: AlertsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AlertsScreen(
        state = state,
        onRetry = viewModel::retry,
        onTabSelected = onTabSelected,
    )
}

@Composable
fun AlertsScreen(
    state: AlertsUiState,
    onRetry: () -> Unit,
    onTabSelected: (LanternTab) -> Unit,
) {
    val colors = LanternTheme.colors

    Scaffold(
        containerColor = colors.background,
        topBar = {
            LanternTopBar(
                title = stringResource(R.string.alerts_title),
                actions = {
                    SignalChip(isOnline = state.isOnline)
                    TopBarAction(
                        onClick = onRetry,
                        contentDescription = stringResource(R.string.state_retry),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = null,
                            tint = colors.textSecondary,
                        )
                    }
                },
            )
        },
        bottomBar = {
            LanternBottomBar(selectedTab = LanternTab.ALERTS, onSelect = onTabSelected)
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .padding(padding),
        ) {
            when {
                state.isLoading -> LoadingState(
                    message = stringResource(R.string.alerts_loading),
                    modifier = Modifier.fillMaxWidth(),
                )

                state.hasError -> ErrorState(
                    onRetry = onRetry,
                    body = stringResource(R.string.alerts_error),
                    modifier = Modifier.fillMaxWidth(),
                )

                state.alerts.isEmpty() -> EmptyState(
                    title = stringResource(R.string.alerts_empty_title),
                    body = stringResource(R.string.alerts_empty_body),
                )

                else -> LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (!state.isOnline) {
                        item {
                            OfflineNotice(text = stringResource(R.string.alerts_offline_note))
                        }
                    }
                    items(state.alerts, key = { it.id }) { alert ->
                        AlertBanner(
                            alert = alert,
                            language = state.language,
                            savedLabel = relativeTime(alert.issuedAt),
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "Alerts — offline", widthDp = 390, heightDp = 844)
@Composable
private fun AlertsPreview() {
    LanternPreview {
        AlertsScreen(
            state = AlertsUiState(
                isLoading = false,
                isOnline = false,
                alerts = listOf(
                    previewAlert(
                        id = "a1",
                        level = AlertLevel.URGENT,
                        title = "Flood water rising in Sector 4",
                        message = "Move to higher ground now. Rescue boats are operating from the school.",
                        minutesAgo = 8,
                    ),
                    previewAlert(
                        id = "a2",
                        level = AlertLevel.INFO,
                        title = "Water distribution point open",
                        message = "Clean drinking water is available at the community centre until 8 pm.",
                        minutesAgo = 95,
                    ),
                ),
            ),
            onRetry = {},
            onTabSelected = {},
        )
    }
}

@Preview(name = "Alerts — empty", widthDp = 390, heightDp = 844)
@Composable
private fun AlertsEmptyPreview() {
    LanternPreview {
        AlertsScreen(
            state = AlertsUiState(isLoading = false, isOnline = false, alerts = emptyList()),
            onRetry = {},
            onTabSelected = {},
        )
    }
}

@Preview(name = "Alerts — error", widthDp = 390, heightDp = 844)
@Composable
private fun AlertsErrorPreview() {
    LanternPreview {
        AlertsScreen(
            state = AlertsUiState(isLoading = false, isOnline = true, hasError = true),
            onRetry = {},
            onTabSelected = {},
        )
    }
}

private fun previewAlert(
    id: String,
    level: AlertLevel,
    title: String,
    message: String,
    minutesAgo: Long,
) = Alert(
    id = id,
    level = level,
    title = LocalizedText(en = title),
    message = LocalizedText(en = message),
    region = "Sector 4",
    issuedAt = System.currentTimeMillis() - minutesAgo * 60_000L,
)
