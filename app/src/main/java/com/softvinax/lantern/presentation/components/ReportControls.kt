package com.softvinax.lantern.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.softvinax.lantern.R
import com.softvinax.lantern.domain.model.EmergencyType
import com.softvinax.lantern.domain.model.Need
import com.softvinax.lantern.presentation.theme.LanternTheme
import kotlin.math.roundToInt

@Composable
fun typeLabel(type: EmergencyType): String = stringResource(
    when (type) {
        EmergencyType.FLOOD -> R.string.type_flood
        EmergencyType.EARTHQUAKE -> R.string.type_earthquake
        EmergencyType.OTHER -> R.string.type_other
    },
)

@Composable
fun needLabel(need: Need): String = stringResource(
    when (need) {
        Need.RESCUE -> R.string.need_rescue
        Need.SHELTER -> R.string.need_shelter
        Need.WATER -> R.string.need_water
        Need.FOOD -> R.string.need_food
        Need.MEDICAL -> R.string.need_medical
        Need.CLOTHING -> R.string.need_clothing
        Need.POWER -> R.string.need_power
        Need.OTHER -> R.string.need_other
    },
)

@Composable
fun LanternTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    minLines: Int = 3,
    maxLines: Int = 6,
    supportingText: String? = null,
    isError: Boolean = false,
) {
    val colors = LanternTheme.colors
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        minLines = minLines,
        maxLines = maxLines,
        isError = isError,
        shape = RoundedCornerShape(16.dp),
        textStyle = MaterialTheme.typography.bodyLarge,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Default,
        ),
        supportingText = supportingText?.let {
            {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isError) colors.amber else colors.textSecondary,
                )
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = colors.textPrimary,
            unfocusedTextColor = colors.textPrimary,
            focusedContainerColor = colors.surfaceElevated,
            unfocusedContainerColor = colors.surfaceElevated,
            cursorColor = colors.amber,
            focusedBorderColor = colors.amber,
            unfocusedBorderColor = colors.border,
            focusedLabelColor = colors.amber,
            unfocusedLabelColor = colors.textSecondary,
            focusedPlaceholderColor = colors.textSecondary,
            unfocusedPlaceholderColor = colors.textSecondary,
            errorTextColor = colors.textPrimary,
            errorContainerColor = colors.surfaceElevated,
            errorCursorColor = colors.amber,
            errorBorderColor = colors.amber,
            errorLabelColor = colors.amber,
            errorSupportingTextColor = colors.amber,
            focusedSupportingTextColor = colors.textSecondary,
            unfocusedSupportingTextColor = colors.textSecondary,
        ),
    )
}

@Composable
fun UrgencySelector(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LanternTheme.colors
    val clamped = value.coerceIn(1, 5)
    val urgent = clamped >= 4
    val accent = if (urgent) colors.coral else colors.amber
    val description = stringResource(R.string.cd_urgency, clamped)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.review_urgency),
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
            )
            Text(
                text = stringResource(R.string.review_urgency_value, clamped),
                style = MaterialTheme.typography.labelLarge,
                color = accent,
            )
        }
        Slider(
            value = clamped.toFloat(),
            onValueChange = { onValueChange(it.roundToInt().coerceIn(1, 5)) },
            valueRange = 1f..5f,
            steps = 3,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = description },
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent,
                inactiveTrackColor = colors.surfaceVariant,
                activeTickColor = colors.background,
                inactiveTickColor = colors.textSecondary,
            ),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.review_urgency_low),
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary,
            )
            Text(
                text = stringResource(R.string.review_urgency_high),
                style = MaterialTheme.typography.labelSmall,
                color = if (urgent) colors.coral else colors.textSecondary,
            )
        }
    }
}

@Composable
fun PeopleStepper(
    count: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LanternTheme.colors
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(R.string.review_people),
            style = MaterialTheme.typography.titleMedium,
            color = colors.textPrimary,
        )
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
                modifier = Modifier.width(40.dp),
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

@Composable
private fun StepButton(
    text: String,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = LanternTheme.colors
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(colors.surfaceElevated)
            .border(1.dp, colors.border, CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.headlineSmall,
            color = if (enabled) colors.amber else colors.textSecondary,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TypeSelector(
    selected: EmergencyType,
    onSelect: (EmergencyType) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        EmergencyType.entries.forEach { type ->
            ToggleChip(
                text = typeLabel(type),
                selected = selected == type,
                onClick = { onSelect(type) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NeedSelector(
    selected: Set<Need>,
    onToggle: (Need) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Need.entries.forEach { need ->
            ToggleChip(
                text = needLabel(need),
                selected = need in selected,
                onClick = { onToggle(need) },
            )
        }
    }
}

@Composable
fun ShareLocationRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = LanternTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) colors.textPrimary else colors.textSecondary,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
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
