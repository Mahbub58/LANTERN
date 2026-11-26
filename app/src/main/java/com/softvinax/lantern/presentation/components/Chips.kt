package com.softvinax.lantern.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.softvinax.lantern.R
import com.softvinax.lantern.presentation.theme.LanternTheme

@Composable
fun SignalChip(
    isOnline: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = LanternTheme.colors
    val content = if (isOnline) colors.blue else colors.teal
    val container = if (isOnline) colors.blueTint else colors.tealTint
    val label = stringResource(
        if (isOnline) R.string.status_online_cloud else R.string.status_offline_device,
    )
    val description = stringResource(
        if (isOnline) R.string.cd_network_online else R.string.cd_network_offline,
    )

    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(container)
            .border(1.dp, content.copy(alpha = 0.25f), CircleShape)
            .semantics { contentDescription = description }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = if (isOnline) LanternIcons.Cloud else LanternIcons.Smartphone,
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = content,
            maxLines = 1,
        )
    }
}

@Composable
fun QuickChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LanternTheme.colors
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = modifier
            .heightIn(min = 56.dp)
            .clip(shape)
            .background(colors.surfaceElevated)
            .border(1.dp, colors.border, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = colors.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun ToggleChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LanternTheme.colors
    val shape = RoundedCornerShape(14.dp)
    val container = if (selected) colors.amberTint else colors.surfaceElevated
    val borderColor = if (selected) colors.amber else colors.border
    val textColor = if (selected) colors.amber else colors.textPrimary

    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(shape)
            .background(container)
            .border(1.dp, borderColor, shape)
            .clickable(role = Role.Checkbox, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            maxLines = 2,
        )
    }
}

@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = LanternTheme.colors.textPrimary,
        modifier = modifier.fillMaxWidth(),
    )
}
