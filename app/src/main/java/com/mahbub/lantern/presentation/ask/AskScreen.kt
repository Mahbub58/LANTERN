package com.mahbub.lantern.presentation.ask

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import com.mahbub.lantern.presentation.components.ErrorState
import com.mahbub.lantern.presentation.components.LanternAppBar
import com.mahbub.lantern.presentation.components.LanternBottomBar
import com.mahbub.lantern.presentation.components.LanternIcons
import com.mahbub.lantern.presentation.components.LanternPreview
import com.mahbub.lantern.presentation.components.LanternTab
import com.mahbub.lantern.presentation.components.LoadingState
import com.mahbub.lantern.presentation.components.PrimaryGradientButton
import com.mahbub.lantern.presentation.components.QuickChip
import com.mahbub.lantern.presentation.components.alertVisuals
import com.mahbub.lantern.presentation.components.relativeTime
import com.mahbub.lantern.presentation.theme.LanternTheme
import java.util.concurrent.atomic.AtomicInteger

@Composable
fun AskScreenRoute(
    onOpenAnswer: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDebug: () -> Unit,
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
        onRetry = viewModel::retry,
        onTabSelected = onTabSelected,
    )
}

private val debugTapCount = AtomicInteger(0)

@Composable
fun AskScreen(
    state: AskUiState,
    onQueryChange: (String) -> Unit,
    onAsk: () -> Unit,
    onQuickChip: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDebug: () -> Unit,
    onRetry: () -> Unit,
    onTabSelected: (LanternTab) -> Unit,
) {
    val colors = LanternTheme.colors
    val snackbarHostState = remember { SnackbarHostState() }
    val voiceInput = rememberVoiceInput(
        language = state.language,
        onResult = onQueryChange,
    )
    val voiceMessage = voiceInput.messageRes?.let { stringResource(it) }

    LaunchedEffect(voiceMessage) {
        if (voiceMessage != null) {
            snackbarHostState.showSnackbar(voiceMessage)
            voiceInput.clearMessage()
        }
    }

    Scaffold(
        containerColor = colors.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            LanternAppBar(
                isOnline = state.isOnline,
                onOpenSettings = onOpenSettings,
                onTitleClick = {
                    if (debugTapCount.incrementAndGet() >= 7) {
                        debugTapCount.set(0)
                        onOpenDebug()
                    }
                },
                titleContentDescription = stringResource(R.string.cd_debug_title),
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.background),
            ) {
                EmergencyFooter(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
                LanternBottomBar(selectedTab = LanternTab.ASK, onSelect = onTabSelected)
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (state.isLoading) {
                LoadingState(message = stringResource(R.string.answer_loading))
                return@Column
            }

            if (state.hasError) {
                ErrorState(onRetry = onRetry)
                return@Column
            }

            state.latestAlert?.let { alert ->
                AskAlertCard(alert = alert, language = state.language)
            } ?: NoAlertCard()

            AskInputCard(
                query = state.query,
                onQueryChange = onQueryChange,
                placeholder = stringResource(R.string.ask_question_label),
                isError = state.blankError,
                isListening = voiceInput.isListening,
                onMicClick = voiceInput.onMicClick,
            )

            if (state.blankError) {
                Text(
                    text = stringResource(R.string.ask_blank_error),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.amber,
                )
            }

            Text(
                text = stringResource(R.string.ask_privacy_note),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )

            QuickChipGrid(
                onQuickChip = onQuickChip,
                floodRising = stringResource(R.string.quick_flood_rising),
                earthquakeNow = stringResource(R.string.quick_earthquake_now),
                safeShelter = stringResource(R.string.quick_safe_shelter),
                missingFamily = stringResource(R.string.quick_missing_family),
            )

            PrimaryGradientButton(
                text = stringResource(R.string.ask_button),
                onClick = onAsk,
                glow = true,
            )

            Spacer(Modifier.heightIn(min = 4.dp))
        }
    }
}

@Composable
private fun AskAlertCard(
    alert: Alert,
    language: AppLanguage,
) {
    val colors = LanternTheme.colors
    val (_, accent) = alertVisuals(alert.level)
    val shape = RoundedCornerShape(24.dp)
    val savedLabel = stringResource(R.string.ask_alert_saved, relativeTime(alert.issuedAt))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .background(accent.copy(alpha = 0.06f))
            .border(1.dp, accent.copy(alpha = 0.45f), shape)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            AlertWatchPill(level = alert.level, accent = accent)
            Spacer(Modifier.width(8.dp))
            Text(
                text = alert.region,
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = savedLabel,
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary,
                maxLines = 1,
            )
        }
        Text(
            text = alert.title.forLanguage(language),
            style = MaterialTheme.typography.headlineSmall,
            color = colors.textPrimary,
        )
        Text(
            text = alert.message.forLanguage(language),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
        )
    }
}

@Composable
private fun AlertWatchPill(
    level: AlertLevel,
    accent: androidx.compose.ui.graphics.Color,
) {
    val label = when (level) {
        AlertLevel.WARNING -> stringResource(R.string.alert_watch)
        AlertLevel.URGENT -> stringResource(R.string.alerts_level_urgent)
        AlertLevel.INFO -> stringResource(R.string.alerts_level_info)
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = accent,
        modifier = Modifier
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.14f))
            .border(1.dp, accent.copy(alpha = 0.45f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

@Composable
private fun NoAlertCard() {
    val colors = LanternTheme.colors
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.border, shape)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Info,
            contentDescription = null,
            tint = colors.teal,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = stringResource(R.string.ask_no_alerts),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
        )
    }
}

@Composable
private fun AskInputCard(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    isError: Boolean,
    isListening: Boolean,
    onMicClick: () -> Unit,
) {
    val colors = LanternTheme.colors
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(24.dp)
    val borderColor = when {
        isError -> colors.amber
        focused -> colors.teal
        else -> colors.teal.copy(alpha = 0.32f)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, borderColor, shape)
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 84.dp)
                .onFocusChanged { focused = it.isFocused },
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.textPrimary),
            cursorBrush = SolidColor(colors.amber),
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (query.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.textSecondary,
                        )
                    }
                    innerTextField()
                }
            },
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
        ) {
            if (isListening) {
                Text(
                    text = stringResource(R.string.voice_listening),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.amber,
                )
                Spacer(Modifier.width(10.dp))
            }
            MicButton(isListening = isListening, onClick = onMicClick)
        }
    }
}

@Composable
private fun MicButton(
    isListening: Boolean,
    onClick: () -> Unit,
) {
    val colors = LanternTheme.colors
    val contentDescription = stringResource(
        if (isListening) R.string.cd_voice_stop else R.string.cd_voice_input,
    )
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (isListening) colors.amberTint else colors.surfaceElevated)
            .border(1.dp, if (isListening) colors.amber else colors.border, CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = LanternIcons.Mic,
            contentDescription = null,
            tint = if (isListening) colors.amber else colors.textSecondary,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun QuickChipGrid(
    onQuickChip: (String) -> Unit,
    floodRising: String,
    earthquakeNow: String,
    safeShelter: String,
    missingFamily: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            QuickChip(
                text = floodRising,
                onClick = { onQuickChip(floodRising) },
                modifier = Modifier.weight(1f),
            )
            QuickChip(
                text = earthquakeNow,
                onClick = { onQuickChip(earthquakeNow) },
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            QuickChip(
                text = safeShelter,
                onClick = { onQuickChip(safeShelter) },
                modifier = Modifier.weight(1f),
            )
            QuickChip(
                text = missingFamily,
                onClick = { onQuickChip(missingFamily) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun EmergencyFooter(modifier: Modifier = Modifier) {
    val colors = LanternTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(colors.surface)
            .border(1.dp, colors.border, CircleShape)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Phone,
            contentDescription = null,
            tint = colors.amber,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = stringResource(R.string.call_emergency_first),
            style = MaterialTheme.typography.labelSmall,
            color = colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview(name = "Ask — online with alert", widthDp = 390, heightDp = 844)
@Composable
private fun AskPreview() {
    LanternPreview {
        AskScreen(
            state = AskUiState(
                query = "Zalzala aa raha hai, abhi kya karun?",
                isOnline = true,
                language = AppLanguage.EN,
                isLoading = false,
                latestAlert = Alert(
                    id = "a1",
                    level = AlertLevel.WARNING,
                    title = LocalizedText(en = "Flash flood watch — Swat Valley"),
                    message = LocalizedText(
                        en = "Heavy monsoon rain upstream. Keep phones charged and stay clear of low riverbanks.",
                    ),
                    region = "Swat Valley",
                    issuedAt = System.currentTimeMillis() - 8 * 60_000L,
                ),
            ),
            onQueryChange = {},
            onAsk = {},
            onQuickChip = {},
            onOpenSettings = {},
            onOpenDebug = {},
            onRetry = {},
            onTabSelected = {},
        )
    }
}

@Preview(name = "Ask — offline, empty", widthDp = 390, heightDp = 844)
@Composable
private fun AskOfflinePreview() {
    LanternPreview {
        AskScreen(
            state = AskUiState(
                query = "",
                isOnline = false,
                language = AppLanguage.EN,
                isLoading = false,
            ),
            onQueryChange = {},
            onAsk = {},
            onQuickChip = {},
            onOpenSettings = {},
            onOpenDebug = {},
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
            onRetry = {},
            onTabSelected = {},
        )
    }
}
