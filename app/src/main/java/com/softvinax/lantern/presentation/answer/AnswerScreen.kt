package com.softvinax.lantern.presentation.answer

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
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
import com.softvinax.lantern.domain.model.Answer
import com.softvinax.lantern.domain.model.AppLanguage
import com.softvinax.lantern.domain.model.EmergencyNumber
import com.softvinax.lantern.domain.model.InferenceSource
import com.softvinax.lantern.domain.model.LocalizedList
import com.softvinax.lantern.domain.model.LocalizedText
import com.softvinax.lantern.domain.model.SafetyCard
import com.softvinax.lantern.presentation.components.CloudGradientButton
import com.softvinax.lantern.presentation.components.InferenceBadge
import com.softvinax.lantern.presentation.components.LanternCard
import com.softvinax.lantern.presentation.components.LanternPreview
import com.softvinax.lantern.presentation.components.LanternTextButton
import com.softvinax.lantern.presentation.components.LanternTopBar
import com.softvinax.lantern.presentation.components.LoadingState
import com.softvinax.lantern.presentation.components.NeedSelector
import com.softvinax.lantern.presentation.components.NeverDoBox
import com.softvinax.lantern.presentation.components.NumberedSteps
import com.softvinax.lantern.presentation.components.OutlineActionButton
import com.softvinax.lantern.presentation.components.PrimaryGradientButton
import com.softvinax.lantern.presentation.components.SignalChip
import com.softvinax.lantern.presentation.components.inferenceSourceLabel
import com.softvinax.lantern.presentation.theme.LanternTheme

@Composable
fun AnswerScreenRoute(
    onBack: () -> Unit,
    onAskAnother: () -> Unit,
    onFileReport: (String) -> Unit,
    viewModel: AnswerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is AnswerEvent.FileReport -> onFileReport(event.question)
                AnswerEvent.AskAnother -> onAskAnother()
            }
        }
    }
    AnswerScreen(
        state = state,
        onBack = onBack,
        onRetry = viewModel::ask,
        onAskDeeper = viewModel::askDeeper,
        onFileReport = viewModel::fileReport,
        onAskAnother = viewModel::askAnother,
    )
}

@Composable
fun AnswerScreen(
    state: AnswerUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onAskDeeper: () -> Unit,
    onFileReport: () -> Unit,
    onAskAnother: () -> Unit,
) {
    val colors = LanternTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .navigationBarsPadding(),
    ) {
        LanternTopBar(
            title = stringResource(R.string.answer_title),
            onBack = onBack,
            actions = { SignalChip(isOnline = state.isOnline) },
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Safety line is always the first thing on screen.
            LanternCard(borderColor = colors.coral.copy(alpha = 0.5f)) {
                Text(
                    text = stringResource(R.string.call_emergency_first),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.coral,
                )
            }

            when {
                state.isLoading -> LoadingState(
                    message = stringResource(R.string.answer_loading),
                    modifier = Modifier.fillMaxWidth(),
                )

                state.hasError -> com.softvinax.lantern.presentation.components.ErrorState(onRetry = onRetry)

                state.answer == null -> com.softvinax.lantern.presentation.components.ErrorState(onRetry = onRetry)

                else -> AnswerContent(
                    state = state,
                    onAskDeeper = onAskDeeper,
                    onFileReport = onFileReport,
                    onAskAnother = onAskAnother,
                )
            }
        }
    }
}

@Composable
private fun AnswerContent(
    state: AnswerUiState,
    onAskDeeper: () -> Unit,
    onFileReport: () -> Unit,
    onAskAnother: () -> Unit,
) {
    val answer = state.answer ?: return
    val colors = LanternTheme.colors
    val context = LocalContext.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        InferenceBadge(source = answer.source)
        if (!answer.isNoMatch) {
            Text(
                text = stringResource(R.string.answer_title),
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary,
            )
        }
    }

    if (answer.isNoMatch) {
        NoMatchContent(answer = answer)
    } else {
        val card = answer.card
        if (card != null) {
            LanternCard {
                Text(
                    text = card.title.forLanguage(state.language),
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(18.dp))
                Text(
                    text = stringResource(R.string.answer_steps),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.amber,
                )
                Spacer(Modifier.height(12.dp))
                NumberedSteps(steps = card.steps.forLanguage(state.language))
                if (card.never.forLanguage(state.language).isNotEmpty()) {
                    Spacer(Modifier.height(18.dp))
                    NeverDoBox(
                        title = stringResource(R.string.answer_never),
                        items = card.never.forLanguage(state.language),
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.answer_source_verify, card.source),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary,
                )
            }
        }
    }

    if (answer.source == InferenceSource.CARD_TEXT) {
        Text(
            text = stringResource(R.string.answer_card_text_note),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
        )
    }

    if (answer.isDeeper) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(colors.cloudTint)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.answer_deeper),
                style = MaterialTheme.typography.bodySmall,
                color = colors.blue,
            )
        }
    }

    Text(
        text = stringResource(R.string.answer_safety_note),
        style = MaterialTheme.typography.bodySmall,
        color = colors.textSecondary,
    )

    if (state.showDeeperButton) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CloudGradientButton(
                text = when {
                    state.isAskingDeeper -> stringResource(R.string.answer_deeper_loading)
                    !state.isOnline -> stringResource(R.string.answer_deeper_offline)
                    !state.cloudFallbackEnabled -> stringResource(R.string.answer_cloud_disabled)
                    else -> stringResource(R.string.answer_deeper)
                },
                onClick = onAskDeeper,
                enabled = state.canUseCloud && !state.isAskingDeeper,
                loading = state.isAskingDeeper,
            )
            if (state.deeperFailed) {
                Text(
                    text = stringResource(R.string.answer_deeper_failed),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
            }
        }
    }

    PrimaryGradientButton(
        text = stringResource(R.string.answer_file_report),
        onClick = onFileReport,
    )

    LanternTextButton(
        text = stringResource(R.string.answer_ask_another),
        onClick = onAskAnother,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun NoMatchContent(answer: Answer) {
    val colors = LanternTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        LanternCard(borderColor = colors.border) {
            Text(
                text = stringResource(R.string.answer_no_match_title),
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.answer_no_match_body),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
            )
        }

        if (answer.emergencyNumbers.isNotEmpty()) {
            Text(
                text = stringResource(R.string.answer_emergency_numbers),
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
            )
            answer.emergencyNumbers.forEach { number ->
                EmergencyNumberRow(number = number)
            }
            Text(
                text = stringResource(R.string.answer_no_match_note),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
        }
    }
}

@Composable
private fun EmergencyNumberRow(number: EmergencyNumber) {
    val colors = LanternTheme.colors
    val context = LocalContext.current
    val description = stringResource(R.string.cd_call_number, number.number)
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
        Column {
            Text(
                text = number.label,
                style = MaterialTheme.typography.titleMedium,
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
            style = MaterialTheme.typography.titleMedium,
            color = colors.amber,
        )
    }
}

@Preview(name = "Answer — on-device card", widthDp = 390, heightDp = 844)
@Composable
private fun AnswerCardPreview() {
    LanternPreview {
        AnswerScreen(
            state = AnswerUiState(
                question = "Water is entering my home",
                isLoading = false,
                isOnline = false,
                language = AppLanguage.EN,
                answer = Answer(
                    card = previewCard(),
                    text = "Move to the highest floor.",
                    source = InferenceSource.ON_DEVICE,
                    retrievalScore = 0.82f,
                ),
            ),
            onBack = {},
            onRetry = {},
            onAskDeeper = {},
            onFileReport = {},
            onAskAnother = {},
        )
    }
}

@Preview(name = "Answer — no match", widthDp = 390, heightDp = 844)
@Composable
private fun AnswerNoMatchPreview() {
    LanternPreview {
        AnswerScreen(
            state = AnswerUiState(
                question = "My neighbour has a fever",
                isLoading = false,
                isOnline = true,
                language = AppLanguage.EN,
                answer = Answer(
                    card = null,
                    text = "",
                    source = InferenceSource.CARD_TEXT,
                    isNoMatch = true,
                    emergencyNumbers = listOf(
                        EmergencyNumber("Rescue 1122", "1122", "Punjab emergency service"),
                        EmergencyNumber("NDMA", "111-157-157", "National disaster helpline"),
                    ),
                ),
            ),
            onBack = {},
            onRetry = {},
            onAskDeeper = {},
            onFileReport = {},
            onAskAnother = {},
        )
    }
}

@Preview(name = "Answer — cloud, deeper available", widthDp = 390, heightDp = 844)
@Composable
private fun AnswerCloudPreview() {
    LanternPreview {
        AnswerScreen(
            state = AnswerUiState(
                question = "Earthquake just now",
                isLoading = false,
                isOnline = true,
                language = AppLanguage.EN,
                answer = Answer(
                    card = previewCard(),
                    text = "Drop, cover, hold on.",
                    source = InferenceSource.CLOUD,
                    isDeeper = true,
                ),
            ),
            onBack = {},
            onRetry = {},
            onAskDeeper = {},
            onFileReport = {},
            onAskAnother = {},
        )
    }
}

@Preview(name = "Answer — loading", widthDp = 390, heightDp = 844)
@Composable
private fun AnswerLoadingPreview() {
    LanternPreview {
        AnswerScreen(
            state = AnswerUiState(isLoading = true, language = AppLanguage.EN),
            onBack = {},
            onRetry = {},
            onAskDeeper = {},
            onFileReport = {},
            onAskAnother = {},
        )
    }
}

private fun previewCard() = SafetyCard(
    id = "flood-1",
    title = LocalizedText(en = "Water is entering your home"),
    steps = LocalizedList(
        en = listOf(
            "Move everyone to the highest floor.",
            "Switch off electricity at the main breaker.",
            "Take drinking water and a torch to the roof.",
        ),
    ),
    never = LocalizedList(
        en = listOf("Never walk or drive through moving flood water."),
    ),
    source = "TO VERIFY",
    keywords = LocalizedList(en = listOf("water", "flood", "home")),
)
