package com.softvinax.lantern.presentation.ask

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.softvinax.lantern.presentation.components.LanternBottomBar
import com.softvinax.lantern.presentation.components.LanternPreview
import com.softvinax.lantern.presentation.components.LanternTab
import com.softvinax.lantern.presentation.components.LanternTextButton
import com.softvinax.lantern.presentation.components.LanternTextField
import com.softvinax.lantern.presentation.components.LanternTopBar
import com.softvinax.lantern.presentation.components.LoadingState
import com.softvinax.lantern.presentation.components.OfflineNotice
import com.softvinax.lantern.presentation.components.PrimaryGradientButton
import com.softvinax.lantern.presentation.components.QuickChip
import com.softvinax.lantern.presentation.components.SignalChip
import com.softvinax.lantern.presentation.components.TopBarAction
import com.softvinax.lantern.presentation.components.relativeTime
import com.softvinax.lantern.presentation.theme.LanternTheme
import java.util.concurrent.atomic.AtomicInteger

@Composable
fun AskScreenRoute(
    onOpenAnswer: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDebug: () -> Unit,
    onOpenMyReports: () -> Unit,
    onTabSelected: (LanternTab) -> Unit,
    viewModel: AskViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is AskEvent.OpenAnswer -> onOpenAnswer(event.question)
                AskEvent.OpenSettings -> onOpenSettings()
                AskEvent.OpenDebug -> onOpenDebug()
                AskEvent.OpenMyReports -> onOpenMyReports()
            }
        }
    }
    AskScreen(
        state = state,
        onQueryChange = viewModel::onQueryChange,
        onAsk = viewModel::ask,
        onQuickChip = viewModel::onQuickChip,
        onOpenSettings = viewModel::openSettings,
        onOpenDebug = viewModel::openDebug,
        onOpenMyReports = viewModel::openMyReports,
        onRetry = viewModel::retry,
        onTabSelected = onTabSelected,
    )
}

private val debugTapCount = AtomicInteger(0)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AskScreen(
    state: AskUiState,
    onQueryChange: (String) -> Unit,
    onAsk: () -> Unit,
    onQuickChip: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDebug: () -> Unit,
    onOpenMyReports: () -> Unit,
    onRetry: () -> Unit,
    onTabSelected: (LanternTab) -> Unit,
) {
    val colors = LanternTheme.colors
    val debugHint = stringResource(R.string.cd_debug_title)
    val floodRising = stringResource(R.string.quick_flood_rising)
    val earthquakeNow = stringResource(R.string.quick_earthquake_now)
    val safeShelter = stringResource(R.string.quick_safe_shelter)
    val missingFamily = stringResource(R.string.quick_missing_family)

    Scaffold(
        containerColor = colors.background,
        topBar = {
            LanternTopBar(
                title = stringResource(R.string.ask_title),
                onTitleClick = {
                    if (debugTapCount.incrementAndGet() >= 7) {
                        debugTapCount.set(0)
                        onOpenDebug()
                    }
                },
                titleContentDescription = debugHint,
                actions = {
                    SignalChip(isOnline = state.isOnline)
                    TopBarAction(onClick = onOpenSettings, contentDescription = stringResource(R.string.cd_settings)) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                },
            )
        },
        bottomBar = {
            LanternBottomBar(selectedTab = LanternTab.ASK, onSelect = onTabSelected)
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
            if (state.isLoading) {
                LoadingState(message = stringResource(R.string.answer_loading))
                return@Column
            }

            if (state.hasError) {
                com.softvinax.lantern.presentation.components.ErrorState(onRetry = onRetry)
                return@Column
            }

            state.latestAlert?.let { alert ->
                AlertBanner(
                    alert = alert,
                    language = state.language,
                    savedLabel = relativeTime(alert.issuedAt),
                )
            } ?: Text(
                text = stringResource(R.string.ask_no_alerts),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.ask_question_label),
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.textPrimary,
                )
                LanternTextField(
                    value = state.query,
                    onValueChange = onQueryChange,
                    label = stringResource(R.string.ask_question_label),
                    placeholder = stringResource(R.string.ask_placeholder),
                    isError = state.blankError,
                    supportingText = if (state.blankError) {
                        stringResource(R.string.ask_blank_error)
                    } else {
                        stringResource(R.string.ask_privacy_note)
                    },
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                QuickChip(text = floodRising, onClick = { onQuickChip(floodRising) })
                QuickChip(text = earthquakeNow, onClick = { onQuickChip(earthquakeNow) })
                QuickChip(text = safeShelter, onClick = { onQuickChip(safeShelter) })
                QuickChip(text = missingFamily, onClick = { onQuickChip(missingFamily) })
            }

            PrimaryGradientButton(
                text = stringResource(R.string.ask_button),
                onClick = onAsk,
            )

            if (!state.isOnline) {
                OfflineNotice(text = stringResource(R.string.offline_mode_footer))
            }

            LanternTextButton(
                text = stringResource(R.string.my_reports),
                onClick = onOpenMyReports,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Preview(name = "Ask — offline with alert", widthDp = 390, heightDp = 844)
@Composable
private fun AskPreview() {
    LanternPreview {
        AskScreen(
            state = AskUiState(
                query = "",
                isOnline = false,
                language = AppLanguage.EN,
                isLoading = false,
                latestAlert = Alert(
                    id = "a1",
                    level = AlertLevel.WARNING,
                    title = LocalizedText(en = "Flood water rising in Sector 4"),
                    message = LocalizedText(en = "Move to higher ground now. Rescue boats are operating from the school."),
                    region = "Sector 4",
                    issuedAt = System.currentTimeMillis() - 8 * 60_000L,
                ),
            ),
            onQueryChange = {},
            onAsk = {},
            onQuickChip = {},
            onOpenSettings = {},
            onOpenDebug = {},
            onOpenMyReports = {},
            onRetry = {},
            onTabSelected = {},
        )
    }
}

@Preview(name = "Ask — loading", widthDp = 390, heightDp = 844)
@Composable
private fun AskLoadingPreview() {
    LanternPreview {
        AskScreen(
            state = AskUiState(isLoading = true, language = AppLanguage.EN),
            onQueryChange = {},
            onAsk = {},
            onQuickChip = {},
            onOpenSettings = {},
            onOpenDebug = {},
            onOpenMyReports = {},
            onRetry = {},
            onTabSelected = {},
        )
    }
}

@Preview(name = "Ask — blank error, online", widthDp = 390, heightDp = 844)
@Composable
private fun AskErrorPreview() {
    LanternPreview {
        AskScreen(
            state = AskUiState(
                query = "   ",
                isOnline = true,
                language = AppLanguage.EN,
                isLoading = false,
                blankError = true,
            ),
            onQueryChange = {},
            onAsk = {},
            onQuickChip = {},
            onOpenSettings = {},
            onOpenDebug = {},
            onOpenMyReports = {},
            onRetry = {},
            onTabSelected = {},
        )
    }
}
