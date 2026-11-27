package com.mahbub.lantern.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mahbub.lantern.R
import com.mahbub.lantern.domain.model.EmergencyType
import com.mahbub.lantern.domain.model.Need
import com.mahbub.lantern.presentation.theme.LanternTheme

/** Header + 3-segment progress bar used by every report step. */
@Composable
fun ReportHeader(
    currentStep: Int,
    modifier: Modifier = Modifier,
) {
    val colors = LanternTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.report_header),
                style = MaterialTheme.typography.labelMedium,
                color = colors.textSecondary,
                letterSpacing = 1.sp,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(R.string.report_step_of, currentStep),
                style = MaterialTheme.typography.labelMedium,
                color = colors.amber,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(3) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(
                            if (index < currentStep) colors.amber else colors.surfaceVariant,
                        ),
                )
            }
        }
    }
}

/** 2-column emergency-type grid (Flood / Earthquake / Landslide / Other). */
@Composable
fun TypeGrid(
    selected: EmergencyType,
    onSelect: (EmergencyType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        EmergencyType.entries.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                row.forEach { type ->
                    TypeCard(
                        type = type,
                        selected = type == selected,
                        onClick = { onSelect(type) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun TypeCard(
    type: EmergencyType,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LanternTheme.colors
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .heightIn(min = 60.dp)
            .clip(shape)
            .background(if (selected) colors.amberTint else colors.surfaceElevated)
            .border(1.dp, if (selected) colors.amber else colors.border, shape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = typeLabel(type),
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) colors.amber else colors.textPrimary,
            textAlign = TextAlign.Center,
        )
    }
}

/** 2-column needs grid with the design's teal selected state. */
@Composable
fun NeedGrid(
    selected: Set<Need>,
    onToggle: (Need) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ordered = listOf(
        Need.WATER,
        Need.FOOD,
        Need.SHELTER,
        Need.RESCUE,
        Need.MEDICAL,
        Need.CLOTHING,
        Need.POWER,
        Need.OTHER,
    )
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ordered.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                row.forEach { need ->
                    NeedCard(
                        need = need,
                        selected = need in selected,
                        onClick = { onToggle(need) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun NeedCard(
    need: Need,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LanternTheme.colors
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .heightIn(min = 60.dp)
            .clip(shape)
            .background(if (selected) colors.tealTint else colors.surfaceElevated)
            .border(1.dp, if (selected) colors.teal else colors.border, shape)
            .clickable(role = Role.Checkbox, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = needLabel(need),
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) colors.teal else colors.textPrimary,
            textAlign = TextAlign.Center,
        )
    }
}

/** "People with you" card with the − / + stepper. */
@Composable
fun PeopleCard(
    count: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LanternTheme.colors
    LanternCard(
        modifier = modifier,
        contentPadding = PaddingValues(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.report_people_with_you),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                )
                Text(
                    text = stringResource(R.string.report_people_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StepButton(
                    text = "−",
                    contentDescription = stringResource(R.string.cd_people_decrease),
                    enabled = count > 1,
                    onClick = onDecrease,
                )
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.textPrimary,
                    modifier = Modifier.width(34.dp),
                    textAlign = TextAlign.Center,
                )
                StepButton(
                    text = "+",
                    contentDescription = stringResource(R.string.cd_people_increase),
                    enabled = count < 99,
                    onClick = onIncrease,
                )
            }
        }
    }
}

/** "Use my location" card with a teal switch and a cached-coordinate subtitle. */
@Composable
fun LocationCard(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = LanternTheme.colors
    LanternCard(
        modifier = modifier,
        contentPadding = PaddingValues(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = if (checked) colors.teal else colors.textSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = stringResource(R.string.report_use_location),
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary,
                    )
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (checked) colors.teal else colors.textSecondary,
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = colors.background,
                    checkedTrackColor = colors.teal,
                    uncheckedThumbColor = colors.textSecondary,
                    uncheckedTrackColor = colors.surfaceVariant,
                    uncheckedBorderColor = colors.border,
                ),
            )
        }
    }
}

/** Compact urgency pill with five dots, used on the review card. */
@Composable
fun UrgencyPill(
    urgency: Int,
    modifier: Modifier = Modifier,
) {
    val colors = LanternTheme.colors
    val clamped = urgency.coerceIn(1, 5)
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(colors.coralTint)
            .border(1.dp, colors.coral.copy(alpha = 0.4f), CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.report_urgency_pill, clamped),
            style = MaterialTheme.typography.labelMedium,
            color = colors.coral,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(5) { index ->
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(
                            if (index < clamped) colors.coral else colors.border,
                            CircleShape,
                        ),
                )
            }
        }
    }
}

/** Teal "Structured on this phone" badge. */
@Composable
fun StructuredPill(modifier: Modifier = Modifier) {
    val colors = LanternTheme.colors
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(colors.tealTint)
            .border(1.dp, colors.teal.copy(alpha = 0.35f), CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = LanternIcons.Smartphone,
            contentDescription = null,
            tint = colors.teal,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = stringResource(R.string.report_structured),
            style = MaterialTheme.typography.labelMedium,
            color = colors.teal,
        )
    }
}

/** One label/value row in the review card. */
@Composable
fun ReviewRow(
    label: String,
    modifier: Modifier = Modifier,
    value: @Composable () -> Unit,
) {
    val colors = LanternTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterEnd,
        ) {
            value()
        }
    }
}

@Composable
fun ReviewDivider() {
    HorizontalDivider(
        color = LanternTheme.colors.border.copy(alpha = 0.5f),
        thickness = 1.dp,
    )
}

/** Small teal chip used for each selected need on the review card. */
@Composable
fun NeedPill(
    need: Need,
    modifier: Modifier = Modifier,
) {
    val colors = LanternTheme.colors
    Text(
        text = needLabel(need),
        style = MaterialTheme.typography.labelMedium,
        color = colors.teal,
        modifier = modifier
            .clip(CircleShape)
            .background(colors.tealTint)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}
