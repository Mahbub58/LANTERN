package com.softvinax.lantern.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.softvinax.lantern.R
import com.softvinax.lantern.domain.model.Alert
import com.softvinax.lantern.domain.model.AlertLevel
import com.softvinax.lantern.domain.model.AppLanguage
import com.softvinax.lantern.domain.model.InferenceSource
import com.softvinax.lantern.domain.model.ReportStatus
import com.softvinax.lantern.presentation.theme.LanternTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh

@Composable
fun inferenceSourceLabel(source: InferenceSource): String = stringResource(
    when (source) {
        InferenceSource.ON_DEVICE -> R.string.badge_on_device
        InferenceSource.CLOUD -> R.string.badge_cloud
        InferenceSource.CARD_TEXT -> R.string.badge_card_text
    },
)

@Composable
fun InferenceBadge(
    source: InferenceSource,
    modifier: Modifier = Modifier,
) {
    val colors = LanternTheme.colors
    val (container, content) = when (source) {
        InferenceSource.ON_DEVICE -> colors.tealTint to colors.teal
        InferenceSource.CLOUD -> colors.blueTint to colors.blue
        InferenceSource.CARD_TEXT -> colors.neutralTint to colors.neutralBadgeText
    }
    val label = inferenceSourceLabel(source)
    val description = stringResource(R.string.cd_report_source, label)

    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(container)
            .border(1.dp, content.copy(alpha = 0.35f), CircleShape)
            .semantics { contentDescription = description }
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(content, CircleShape),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = content,
        )
    }
}

@Composable
fun alertLevelLabel(level: AlertLevel): String = stringResource(
    when (level) {
        AlertLevel.INFO -> R.string.alerts_level_info
        AlertLevel.WARNING -> R.string.alerts_level_warning
        AlertLevel.URGENT -> R.string.alerts_level_urgent
    },
)

@Composable
fun alertVisuals(level: AlertLevel): Pair<Color, Color> {
    val colors = LanternTheme.colors
    return when (level) {
        AlertLevel.INFO -> colors.blueTint to colors.blue
        AlertLevel.WARNING -> colors.amberTint to colors.amber
        AlertLevel.URGENT -> colors.coralTint to colors.coral
    }
}

@Composable
fun AlertLevelPill(
    level: AlertLevel,
    modifier: Modifier = Modifier,
) {
    val (container, content) = alertVisuals(level)
    val label = alertLevelLabel(level)
    val description = stringResource(R.string.cd_alert_level, label)
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(container)
            .semantics { contentDescription = description }
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = content,
        )
    }
}

@Composable
fun AlertBanner(
    alert: Alert,
    language: AppLanguage,
    savedLabel: String,
    modifier: Modifier = Modifier,
) {
    val colors = LanternTheme.colors
    val (container, content) = alertVisuals(alert.level)
    val shape = RoundedCornerShape(20.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(shape)
            .background(container)
            .border(1.dp, content.copy(alpha = 0.35f), shape),
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(content),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AlertLevelPill(alert.level)
                Text(
                    text = alert.region,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = savedLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary,
                )
            }
            Text(
                text = alert.title.forLanguage(language),
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
            )
            Text(
                text = alert.message.forLanguage(language),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
            )
        }
    }
}

@Composable
fun ReportStatusChip(
    status: ReportStatus,
    modifier: Modifier = Modifier,
) {
    val colors = LanternTheme.colors
    val (container, content, label) = when (status) {
        ReportStatus.NEW -> Triple(colors.blueTint, colors.blue, stringResource(R.string.reports_status_new))
        ReportStatus.ASSIGNED -> Triple(
            colors.amberTint,
            colors.amber,
            stringResource(R.string.reports_status_assigned),
        )
        ReportStatus.RESOLVED -> Triple(
            colors.tealTint,
            colors.teal,
            stringResource(R.string.reports_status_resolved),
        )
    }
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(container)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = content)
    }
}

/**
 * Shown once a queued report has been handed off. The demo flips from
 * [PendingSyncChip] to this chip as soon as the debug panel goes online.
 */
@Composable
fun SyncedChip(
    modifier: Modifier = Modifier,
) {
    val colors = LanternTheme.colors
    val label = stringResource(R.string.reports_sent)
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(colors.tealTint)
            .semantics { contentDescription = label }
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            tint = colors.teal,
            modifier = Modifier.size(12.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = colors.teal,
        )
    }
}

@Composable
fun PendingSyncChip(
    modifier: Modifier = Modifier,
) {
    val colors = LanternTheme.colors
    val label = stringResource(R.string.reports_pending)
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(colors.neutralTint)
            .semantics { contentDescription = label }
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Refresh,
            contentDescription = null,
            tint = colors.neutralBadgeText,
            modifier = Modifier.size(12.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = colors.neutralBadgeText,
        )
    }
}
