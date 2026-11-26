package com.softvinax.lantern.presentation.language

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.softvinax.lantern.R
import com.softvinax.lantern.domain.model.AppLanguage
import com.softvinax.lantern.presentation.components.LanternMark
import com.softvinax.lantern.presentation.components.LanternPreview
import com.softvinax.lantern.presentation.components.LanternPreviewRtl
import com.softvinax.lantern.presentation.components.OfflineNotice
import com.softvinax.lantern.presentation.components.PrimaryGradientButton
import com.softvinax.lantern.presentation.theme.LanternTheme

@Composable
fun LanguageRoute(
    onSaved: () -> Unit,
    viewModel: LanguageViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            if (event is LanguageEvent.Saved) onSaved()
        }
    }
    LanguageScreen(
        state = state,
        onSelect = viewModel::select,
        onContinue = viewModel::confirm,
    )
}

@Composable
fun LanguageScreen(
    state: LanguageUiState,
    onSelect: (AppLanguage) -> Unit,
    onContinue: () -> Unit,
) {
    val colors = LanternTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            LanternMark()
            Column {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.textPrimary,
                )
                Text(
                    text = stringResource(R.string.app_tagline),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
            }
        }

        Spacer(Modifier.height(40.dp))
        Text(
            text = stringResource(R.string.language_title),
            style = MaterialTheme.typography.headlineSmall,
            color = colors.textPrimary,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.language_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
        )
        Spacer(Modifier.height(24.dp))

        AppLanguage.entries.forEach { language ->
            LanguageOptionCard(
                language = language,
                selected = state.selected == language,
                onClick = { onSelect(language) },
            )
            Spacer(Modifier.height(12.dp))
        }

        if (state.hasError) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.language_error),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.amber,
                textAlign = TextAlign.Start,
            )
        }

        Spacer(Modifier.height(20.dp))
        PrimaryGradientButton(
            text = stringResource(
                if (state.isSaving) R.string.language_saving else R.string.language_continue,
            ),
            onClick = onContinue,
            loading = state.isSaving,
        )
        Spacer(Modifier.height(16.dp))
        OfflineNotice(text = stringResource(R.string.language_offline_note))
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.version_label),
            style = MaterialTheme.typography.labelSmall,
            color = colors.textSecondary,
        )
    }
}

@Composable
private fun LanguageOptionCard(
    language: AppLanguage,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = LanternTheme.colors
    val shape = RoundedCornerShape(18.dp)
    val name = stringResource(
        when (language) {
            AppLanguage.EN -> R.string.language_english
            AppLanguage.UR -> R.string.language_urdu
            AppLanguage.ROMAN_UR -> R.string.language_roman_urdu
        },
    )
    val description = stringResource(R.string.cd_language_option, name)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .clip(shape)
            .background(if (selected) colors.amberTint else colors.surface)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) colors.amber else colors.border,
                shape = shape,
            )
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .semantics { contentDescription = description }
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
            )
            if (selected) {
                Text(
                    text = stringResource(R.string.language_selected),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.amber,
                )
            }
        }
        if (selected) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(colors.amber),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = colors.onAccent,
                    modifier = Modifier.size(18.dp),
                )
            }
        } else {
            Spacer(Modifier.width(28.dp))
        }
    }
}

@Preview(name = "Language — default", widthDp = 390, heightDp = 844)
@Composable
private fun LanguageDefaultPreview() {
    LanternPreview {
        LanguageScreen(
            state = LanguageUiState(selected = AppLanguage.EN),
            onSelect = {},
            onContinue = {},
        )
    }
}

@Preview(name = "Language — saving", widthDp = 390, heightDp = 844)
@Composable
private fun LanguageSavingPreview() {
    LanternPreview {
        LanguageScreen(
            state = LanguageUiState(selected = AppLanguage.ROMAN_UR, isSaving = true),
            onSelect = {},
            onContinue = {},
        )
    }
}

@Preview(name = "Language — error", widthDp = 390, heightDp = 844)
@Composable
private fun LanguageErrorPreview() {
    LanternPreview {
        LanguageScreen(
            state = LanguageUiState(selected = AppLanguage.UR, hasError = true),
            onSelect = {},
            onContinue = {},
        )
    }
}

@Preview(name = "Language — RTL Urdu", widthDp = 390, heightDp = 844, locale = "ur")
@Composable
private fun LanguageRtlPreview() {
    LanternPreviewRtl {
        LanguageScreen(
            state = LanguageUiState(selected = AppLanguage.UR),
            onSelect = {},
            onContinue = {},
        )
    }
}
