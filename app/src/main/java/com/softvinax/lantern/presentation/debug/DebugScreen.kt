package com.softvinax.lantern.presentation.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.softvinax.lantern.R
import com.softvinax.lantern.domain.ai.NanoStatus
import com.softvinax.lantern.domain.model.AlertLevel
import com.softvinax.lantern.domain.model.InferenceSource
import com.softvinax.lantern.domain.model.RoutingConfig
import com.softvinax.lantern.presentation.components.LanternCard
import com.softvinax.lantern.presentation.components.LanternPreview
import com.softvinax.lantern.presentation.components.LanternTextButton
import com.softvinax.lantern.presentation.components.LanternTopBar
import com.softvinax.lantern.presentation.components.LoadingState
import com.softvinax.lantern.presentation.components.SectionLabel
import com.softvinax.lantern.presentation.components.SignalChip
import com.softvinax.lantern.presentation.components.ToggleChip
import com.softvinax.lantern.presentation.components.inferenceSourceLabel
import com.softvinax.lantern.presentation.theme.LanternTheme
import kotlin.math.roundToInt

@Composable
fun DebugScreenRoute(
    onBack: () -> Unit,
    viewModel: DebugViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    DebugScreen(
        state = state,
        onBack = onBack,
        onRetry = viewModel::retry,
        onOnlineChange = viewModel::setOnline,
        onThresholdChange = viewModel::setThreshold,
        onCloudFallbackChange = viewModel::setCloudFallback,
        onAlertLevelChange = viewModel::updateAlertLevel,
        onReset = viewModel::reset,
    )
}

@Composable
fun DebugScreen(
    state: DebugUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onOnlineChange: (Boolean) -> Unit,
    onThresholdChange: (Float) -> Unit,
    onCloudFallbackChange: (Boolean) -> Unit,
    onAlertLevelChange: (AlertLevel) -> Unit,
    onReset: () -> Unit,
) {
    val colors = LanternTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        LanternTopBar(
            title = stringResource(R.string.debug_title),
            onBack = onBack,
            actions = { SignalChip(isOnline = state.isOnline) },
        )

        when {
            state.isLoading -> LoadingState(
                message = stringResource(R.string.debug_loading),
                modifier = Modifier.fillMaxWidth(),
            )

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.debug_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )

                if (state.hasError) {
                    Text(
                        text = stringResource(R.string.debug_error),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.amber,
                    )
                }

                // Network simulation ------------------------------------------
                LanternCard {
                    SectionLabel(text = stringResource(R.string.debug_network))
                    SwitchRow(
                        label = stringResource(
                            if (state.isOnline) R.string.debug_online else R.string.debug_offline,
                        ),
                        checked = state.isOnline,
                        onCheckedChange = onOnlineChange,
                    )
                    Text(
                        text = stringResource(R.string.debug_signal_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                    )
                }

                // Routing ------------------------------------------------------
                LanternCard {
                    SectionLabel(text = stringResource(R.string.debug_routing))
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = stringResource(R.string.debug_threshold),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textPrimary,
                        )
                        Text(
                            text = stringResource(
                                R.string.debug_threshold_value,
                                state.config.confidenceThreshold,
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.amber,
                        )
                    }
                    Slider(
                        value = state.config.confidenceThreshold,
                        onValueChange = { onThresholdChange((it * 100f).roundToInt() / 100f) },
                        valueRange = 0f..1f,
                        steps = 19,
                        colors = SliderDefaults.colors(
                            thumbColor = colors.amber,
                            activeTrackColor = colors.amber,
                            inactiveTrackColor = colors.surfaceVariant,
                            activeTickColor = colors.background,
                            inactiveTickColor = colors.textSecondary,
                        ),
                    )
                    SwitchRow(
                        label = stringResource(R.string.debug_cloud_fallback),
                        checked = state.config.cloudFallbackEnabled,
                        onCheckedChange = onCloudFallbackChange,
                    )
                }

                // On-device + identity -----------------------------------------
                LanternCard {
                    DebugFactRow(
                        label = stringResource(R.string.debug_last_inference),
                        value = state.lastSource?.let { source -> inferenceSourceLabel(source) }
                            ?: stringResource(R.string.debug_none),
                    )
                    DebugFactRow(
                        label = stringResource(R.string.debug_nano),
                        value = stringResource(nanoStatusRes(state.nanoStatus)),
                    )
                    DebugFactRow(
                        label = stringResource(R.string.debug_cards_version),
                        value = state.cardsVersion,
                    )
                    state.uid?.let { uid ->
                        DebugFactRow(
                            label = stringResource(R.string.debug_anonymous_id),
                            value = uid,
                        )
                    }
                }

                // Alert level --------------------------------------------------
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel(text = stringResource(R.string.debug_alert_level))
                    AlertLevelSelector(
                        selected = state.alertLevel,
                        onSelect = onAlertLevelChange,
                    )
                }

                LanternTextButton(
                    text = stringResource(R.string.debug_reset),
                    onClick = onReset,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val colors = LanternTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.amber,
                checkedTrackColor = colors.amberTint,
                uncheckedThumbColor = colors.textSecondary,
                uncheckedTrackColor = colors.surfaceVariant,
                uncheckedBorderColor = colors.border,
            ),
        )
    }
}

@Composable
private fun DebugFactRow(label: String, value: String) {
    val colors = LanternTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = colors.textPrimary,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AlertLevelSelector(
    selected: AlertLevel,
    onSelect: (AlertLevel) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AlertLevel.entries.forEach { level ->
            ToggleChip(
                text = stringResource(
                    when (level) {
                        AlertLevel.INFO -> R.string.alerts_level_info
                        AlertLevel.WARNING -> R.string.alerts_level_warning
                        AlertLevel.URGENT -> R.string.alerts_level_urgent
                    },
                ),
                selected = selected == level,
                onClick = { onSelect(level) },
            )
        }
    }
}

private fun nanoStatusRes(status: NanoStatus?): Int = when (status) {
    NanoStatus.READY -> R.string.nano_ready
    NanoStatus.DOWNLOADING -> R.string.nano_downloading
    NanoStatus.NOT_DOWNLOADED -> R.string.nano_not_downloaded
    NanoStatus.UNAVAILABLE, null -> R.string.nano_unavailable
}

@Preview(name = "Debug — offline", widthDp = 390, heightDp = 844)
@Composable
private fun DebugPreview() {
    LanternPreview {
        DebugScreen(
            state = DebugUiState(
                isLoading = false,
                isOnline = false,
                lastSource = InferenceSource.CARD_TEXT,
                config = RoutingConfig(),
                nanoStatus = NanoStatus.READY,
                cardsVersion = "1.0.0 (6 cards, TO VERIFY)",
                alertLevel = AlertLevel.WARNING,
                uid = "anon_7f3c21",
            ),
            onBack = {},
            onRetry = {},
            onOnlineChange = {},
            onThresholdChange = {},
            onCloudFallbackChange = {},
            onAlertLevelChange = {},
            onReset = {},
        )
    }
}

@Preview(name = "Debug — online", widthDp = 390, heightDp = 844)
@Composable
private fun DebugOnlinePreview() {
    LanternPreview {
        DebugScreen(
            state = DebugUiState(
                isLoading = false,
                isOnline = true,
                lastSource = InferenceSource.CLOUD,
                config = RoutingConfig(confidenceThreshold = 0.7f, cloudFallbackEnabled = false),
                nanoStatus = NanoStatus.READY,
                cardsVersion = "1.0.0 (6 cards, TO VERIFY)",
                alertLevel = AlertLevel.URGENT,
                uid = "anon_7f3c21",
            ),
            onBack = {},
            onRetry = {},
            onOnlineChange = {},
            onThresholdChange = {},
            onCloudFallbackChange = {},
            onAlertLevelChange = {},
            onReset = {},
        )
    }
}
