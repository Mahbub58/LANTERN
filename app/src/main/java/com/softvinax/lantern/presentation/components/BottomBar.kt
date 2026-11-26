package com.softvinax.lantern.presentation.components

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.softvinax.lantern.R
import com.softvinax.lantern.presentation.theme.LanternTheme

enum class LanternTab(
    @StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    ASK(R.string.nav_ask, Icons.Filled.Home),
    REPORT(R.string.nav_report, Icons.Filled.Edit),
    ALERTS(R.string.nav_alerts, Icons.Filled.Notifications),
}

@Composable
fun LanternBottomBar(
    selectedTab: LanternTab,
    onSelect: (LanternTab) -> Unit,
) {
    val colors = LanternTheme.colors
    NavigationBar(
        containerColor = colors.surface,
        contentColor = colors.textSecondary,
        windowInsets = NavigationBarDefaults.windowInsets,
    ) {
        LanternTab.entries.forEach { tab ->
            val label = stringResource(tab.labelRes)
            NavigationBarItem(
                selected = tab == selectedTab,
                onClick = { onSelect(tab) },
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = null,
                    )
                },
                label = { Text(text = label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = colors.onAccent,
                    selectedTextColor = colors.amber,
                    indicatorColor = colors.amber,
                    unselectedIconColor = colors.textSecondary,
                    unselectedTextColor = colors.textSecondary,
                ),
            )
        }
    }
}
