package com.mahbub.lantern.presentation.alerts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mahbub.lantern.R
import com.mahbub.lantern.domain.model.Alert
import com.mahbub.lantern.domain.model.AlertLevel
import com.mahbub.lantern.domain.model.AppLanguage
import com.mahbub.lantern.domain.model.LocalizedText
import com.mahbub.lantern.presentation.components.EmptyState
import com.mahbub.lantern.presentation.components.ErrorState
import com.mahbub.lantern.presentation.components.LanternAppBar
import com.mahbub.lantern.presentation.components.LanternBottomBar
import com.mahbub.lantern.presentation.components.LanternPreview
import com.mahbub.lantern.presentation.components.LanternTab
import com.mahbub.lantern.presentation.components.LanternTextButton
import com.mahbub.lantern.presentation.components.LoadingState
import com.mahbub.lantern.presentation.components.relativeTime
import com.mahbub.lantern.presentation.theme.LanternTheme

@Composable
fun AlertsScreenRoute(
    onOpenSettings: () -> Unit,
    onTabSelected: (LanternTab) -> Unit,
    viewModel: AlertsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AlertsScreen(
        state = state,
        onRefresh = viewModel::retry,
        onOpenSettings = onOpenSettings,
        onTabSelected = onTabSelected,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(
    state: AlertsUiState,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
    onTabSelected: (LanternTab) -> Unit,
) {
    val colors = LanternTheme.colors
    var warningsOnly by rememberSaveable { mutableStateOf(false) }
    var selectedAlert by remember { mutableStateOf<Alert?>(null) }

    val visibleAlerts = if (warningsOnly) {
        state.alerts.filter { it.level != AlertLevel.INFO }
    } else {
        state.alerts
    }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            LanternAppBar(
                isOnline = state.isOnline,
                onOpenSettings = onOpenSettings,
            )
        },
        bottomBar = {
            LanternBottomBar(selectedTab = LanternTab.ALERTS, onSelect = onTabSelected)
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(colors.background),
        ) {
            AlertsHeader(onRefresh = onRefresh)
            AlertFilter(
                warningsOnly = warningsOnly,
                onWarningsOnlyChange = { warningsOnly = it },
            )

            when {
                state.isLoading -> LoadingState(
                    message = stringResource(R.string.alerts_loading),
                    modifier = Modifier.fillMaxWidth(),
                )

                state.hasError -> ErrorState(
                    onRetry = onRefresh,
                    body = stringResource(R.string.alerts_error),
                    modifier = Modifier.fillMaxWidth(),
                )

                visibleAlerts.isEmpty() -> EmptyState(
                    title = stringResource(
                        if (warningsOnly) {
                            R.string.alerts_no_warnings_title
                        } else {
                            R.string.alerts_empty_title
                        },
                    ),
                    body = stringResource(
                        if (warningsOnly) {
                            R.string.alerts_no_warnings_body
                        } else {
                            R.string.alerts_empty_body
                        },
                    ),
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(visibleAlerts, key = { it.id }) { alert ->
                        AlertListCard(
                            alert = alert,
                            language = state.language,
                            onClick = { selectedAlert = alert },
                        )
                    }
                }
            }
        }
    }

    selectedAlert?.let { alert ->
        AlertDetailSheet(
            alert = alert,
            language = state.language,
            onDismiss = { selectedAlert = null },
        )
    }
}

@Composable
private fun AlertsHeader(onRefresh: () -> Unit) {
    val colors = LanternTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.alerts_local_title),
                style = MaterialTheme.typography.headlineMedium,
                color = colors.textPrimary,
            )
            Text(
                text = stringResource(R.string.alerts_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
            )
        }
        RefreshPill(onClick = onRefresh)
    }
}

@Composable
private fun RefreshPill(onClick: () -> Unit) {
    val colors = LanternTheme.colors
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(colors.surface)
            .border(1.dp, colors.border, CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Refresh,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = stringResource(R.string.alerts_refresh),
            style = MaterialTheme.typography.labelLarge,
            color = colors.textPrimary,
        )
    }
}

@Composable
private fun AlertFilter(
    warningsOnly: Boolean,
    onWarningsOnlyChange: (Boolean) -> Unit,
) {
    val colors = LanternTheme.colors
    Row(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .clip(CircleShape)
            .background(colors.surface)
            .border(1.dp, colors.border, CircleShape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        FilterOption(
            text = stringResource(R.string.alerts_filter_all),
            selected = !warningsOnly,
            onClick = { onWarningsOnlyChange(false) },
        )
        FilterOption(
            text = stringResource(R.string.alerts_filter_warnings),
            selected = warningsOnly,
            onClick = { onWarningsOnlyChange(true) },
        )
    }
}

@Composable
private fun FilterOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = LanternTheme.colors
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (selected) colors.surfaceVariant else Color.Transparent)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) colors.textPrimary else colors.textSecondary,
        )
    }
}

@Composable
private fun AlertListCard(
    alert: Alert,
    language: AppLanguage,
    onClick: () -> Unit,
) {
    val colors = LanternTheme.colors
    val accent = alertAccent(alert.level)
    val shape = RoundedCornerShape(22.dp)
    val severity = alertSeverityLabel(alert.level)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.border, shape)
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .width(5.dp)
                .fillMaxHeight()
                .background(accent),
        )
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = severity,
                        style = MaterialTheme.typography.labelMedium,
                        color = accent,
                    )
                    Text(
                        text = " · ",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary,
                    )
                    Text(
                        text = alert.region,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = " · ",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary,
                    )
                    Text(
                        text = relativeTime(alert.issuedAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary,
                        maxLines = 1,
                    )
                }
                Text(
                    text = alert.title.forLanguage(language),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary,
                )
                Text(
                    text = alert.message.forLanguage(language),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = "›",
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textSecondary,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlertDetailSheet(
    alert: Alert,
    language: AppLanguage,
    onDismiss: () -> Unit,
) {
    val colors = LanternTheme.colors
    val accent = alertAccent(alert.level)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = alertSeverityLabel(alert.level),
                    style = MaterialTheme.typography.labelMedium,
                    color = accent,
                )
                Text(
                    text = " · ${alert.region} · ${relativeTime(alert.issuedAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary,
                )
            }
            Text(
                text = alert.title.forLanguage(language),
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary,
            )
            Text(
                text = alert.message.forLanguage(language),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.textSecondary,
            )
            LanternTextButton(
                text = stringResource(R.string.alerts_close),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun alertAccent(level: AlertLevel): Color {
    val colors = LanternTheme.colors
    return when (level) {
        AlertLevel.URGENT -> colors.coral
        AlertLevel.WARNING -> colors.amber
        AlertLevel.INFO -> colors.teal
    }
}

@Composable
private fun alertSeverityLabel(level: AlertLevel): String = stringResource(
    when (level) {
        AlertLevel.URGENT -> R.string.alerts_severity_warning
        AlertLevel.WARNING -> R.string.alerts_severity_watch
        AlertLevel.INFO -> R.string.alerts_severity_clear
    },
)

@Preview(name = "Alerts — all severities", widthDp = 390, heightDp = 844)
@Composable
private fun AlertsPreview() {
    LanternPreview {
        AlertsScreen(
            state = AlertsUiState(
                isLoading = false,
                isOnline = true,
                alerts = listOf(
                    previewAlert(
                        id = "a1",
                        level = AlertLevel.URGENT,
                        region = "Upper Chitral & Mingora",
                        title = "Flash flood warning — river levels rising",
                        message = "Heavy rain upstream is pushing water into low streets. Move to higher floors now.",
                        minutesAgo = 4,
                    ),
                    previewAlert(
                        id = "a2",
                        level = AlertLevel.WARNING,
                        region = "Swat Valley Foothills",
                        title = "Aftershock watch — stay clear of cracked walls",
                        message = "Smaller tremors may follow today's quake. Keep exits clear and shoes by your bed.",
                        minutesAgo = 38,
                    ),
                    previewAlert(
                        id = "a3",
                        level = AlertLevel.INFO,
                        region = "Dir Relief Corridor",
                        title = "All clear — relief shelters open",
                        message = "Main access road is open and two community shelters have drinking water and first aid.",
                        minutesAgo = 120,
                    ),
                ),
            ),
            onRefresh = {},
            onOpenSettings = {},
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
            onRefresh = {},
            onOpenSettings = {},
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
            onRefresh = {},
            onOpenSettings = {},
            onTabSelected = {},
        )
    }
}

private fun previewAlert(
    id: String,
    level: AlertLevel,
    region: String,
    title: String,
    message: String,
    minutesAgo: Long,
) = Alert(
    id = id,
    level = level,
    title = LocalizedText(en = title),
    message = LocalizedText(en = message),
    region = region,
    issuedAt = System.currentTimeMillis() - minutesAgo * 60_000L,
)
