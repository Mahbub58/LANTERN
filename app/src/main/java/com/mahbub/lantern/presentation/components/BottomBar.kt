package com.mahbub.lantern.presentation.components

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.mahbub.lantern.R
import com.mahbub.lantern.presentation.theme.LanternTheme

enum class LanternTab(
    @StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    ASK(R.string.nav_ask, LanternIcons.Chat),
    REPORT(R.string.nav_report, Icons.AutoMirrored.Filled.Send),
    ALERTS(R.string.nav_alerts, Icons.Filled.Notifications),
}

/**
 * Floating rounded navigation bar from the reference design: a dark surface
 * with a hairline border, and a warm amber pill behind the selected tab.
 */
@Composable
fun LanternBottomBar(
    selectedTab: LanternTab,
    onSelect: (LanternTab) -> Unit,
) {
    val colors = LanternTheme.colors
    val shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.surface,
        shape = shape,
        border = BorderStroke(1.dp, colors.border.copy(alpha = 0.55f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            LanternTab.entries.forEach { tab ->
                val selected = tab == selectedTab
                val label = stringResource(tab.labelRes)
                val contentColor = if (selected) colors.amber else colors.textSecondary
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            if (selected) colors.amber.copy(alpha = 0.16f) else Color.Transparent,
                        )
                        .clickable(role = Role.Tab) { onSelect(tab) }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(22.dp),
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor,
                    )
                }
            }
        }
    }
}
