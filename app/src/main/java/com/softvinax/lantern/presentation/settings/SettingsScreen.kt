package com.softvinax.lantern.presentation.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.softvinax.lantern.R
import com.softvinax.lantern.domain.ai.NanoStatus
import com.softvinax.lantern.domain.model.AppLanguage
import com.softvinax.lantern.domain.model.EmergencyNumber
import com.softvinax.lantern.presentation.components.LanternCard
import com.softvinax.lantern.presentation.components.LanternPreview
import com.softvinax.lantern.presentation.components.LanternTopBar
import com.softvinax.lantern.presentation.components.LoadingState
import com.softvinax.lantern.presentation.components.SectionLabel
import com.softvinax.lantern.presentation.components.SignalChip
import com.softvinax.lantern.presentation.components.ToggleChip
import com.softvinax.lantern.presentation.theme.LanternTheme

@Composable
fun SettingsScreenRoute(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        // Language changes re-theme the whole tree via RootViewModel; nothing to navigate.
        viewModel.events.collect { }
    }
    SettingsScreen(
        state = state,
        onBack = onBack,
        onRetry = viewModel::retry,
        onLanguageChange = viewModel::selectLanguage,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
) {
    val colors = LanternTheme.colors
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        LanternTopBar(
            title = stringResource(R.string.settings_title),
            onBack = onBack,
            actions = { SignalChip(isOnline = state.isOnline) },
        )

        when {
            state.isLoading -> LoadingState(
                message = stringResource(R.string.settings_loading),
                modifier = Modifier.fillMaxWidth(),
            )

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                if (state.hasError) {
                    Text(
                        text = stringResource(R.string.settings_error),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.amber,
                    )
                }

                // Language ------------------------------------------------------
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionLabel(text = stringResource(R.string.settings_language))
                    Text(
                        text = stringResource(R.string.settings_language_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AppLanguage.entries.forEach { language ->
                            ToggleChip(
                                text = stringResource(
                                    when (language) {
                                        AppLanguage.EN -> R.string.language_english
                                        AppLanguage.UR -> R.string.language_urdu
                                        AppLanguage.ROMAN_UR -> R.string.language_roman_urdu
                                    },
                                ),
                                selected = state.language == language,
                                onClick = { if (!state.isSaving) onLanguageChange(language) },
                            )
                        }
                    }
                    if (state.isSaving) {
                        Text(
                            text = stringResource(R.string.settings_saving),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.teal,
                        )
                    }
                }

                // Emergency numbers --------------------------------------------
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionLabel(text = stringResource(R.string.settings_emergency))
                    Text(
                        text = stringResource(R.string.settings_emergency_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                    )
                    state.emergencyNumbers.forEach { number ->
                        EmergencyNumberCard(number = number)
                    }
                    Text(
                        text = stringResource(R.string.settings_verify_numbers),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.amber,
                    )
                }

                // On-device model ----------------------------------------------
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel(text = stringResource(R.string.settings_model))
                    Text(
                        text = stringResource(
                            R.string.settings_model_body,
                            stringResource(nanoStatusRes(state.nanoStatus)),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                    )
                }

                // About -------------------------------------------------------
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel(text = stringResource(R.string.settings_about))
                    Text(
                        text = stringResource(R.string.settings_about_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                    )
                    Text(
                        text = stringResource(R.string.settings_cards_version, state.cardsVersion),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.teal,
                    )
                    state.uid?.let { uid ->
                        Text(
                            text = stringResource(R.string.settings_anonymous_id, uid),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary,
                        )
                    }
                    Text(
                        text = stringResource(R.string.settings_debug_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary,
                    )
                    Text(
                        text = stringResource(R.string.version_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary,
                    )
                }

                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun EmergencyNumberCard(number: EmergencyNumber) {
    val colors = LanternTheme.colors
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .clickable(role = Role.Button) {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${number.number}"))
                runCatching { context.startActivity(intent) }
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = number.label,
                style = MaterialTheme.typography.titleSmall,
                color = colors.textPrimary,
            )
            if (number.description.isNotBlank()) {
                Text(
                    text = number.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
            }
        }
        Text(
            text = number.number,
            style = MaterialTheme.typography.titleSmall,
            color = colors.amber,
        )
    }
}

private fun nanoStatusRes(status: NanoStatus?): Int = when (status) {
    NanoStatus.READY -> R.string.nano_ready
    NanoStatus.DOWNLOADING -> R.string.nano_downloading
    NanoStatus.NOT_DOWNLOADED -> R.string.nano_not_downloaded
    NanoStatus.UNAVAILABLE, null -> R.string.nano_unavailable
}

@Preview(name = "Settings", widthDp = 390, heightDp = 844)
@Composable
private fun SettingsPreview() {
    LanternPreview {
        SettingsScreen(
            state = SettingsUiState(
                isLoading = false,
                language = AppLanguage.EN,
                nanoStatus = NanoStatus.READY,
                cardsVersion = "1.0.0 (6 cards, TO VERIFY)",
                emergencyNumbers = listOf(
                    EmergencyNumber("Rescue 1122", "1122", "Punjab emergency service"),
                    EmergencyNumber("NDMA helpline", "111-157-157", "National disaster management"),
                ),
                isOnline = false,
                uid = "anon_7f3c21",
            ),
            onBack = {},
            onRetry = {},
            onLanguageChange = {},
        )
    }
}

@Preview(name = "Settings — loading", widthDp = 390, heightDp = 844)
@Composable
private fun SettingsLoadingPreview() {
    LanternPreview {
        SettingsScreen(
            state = SettingsUiState(isLoading = true),
            onBack = {},
            onRetry = {},
            onLanguageChange = {},
        )
    }
}
