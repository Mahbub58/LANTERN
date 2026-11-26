package com.softvinax.lantern.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.softvinax.lantern.R
import com.softvinax.lantern.presentation.theme.LanternTheme

/**
 * The persistent Lantern app bar: mark, name, connection pill and settings.
 *
 * Used on Ask and every report step so the top bar never disappears. The Ask
 * screen passes [onTitleClick] for the hidden 7-tap demo panel.
 */
@Composable
fun LanternAppBar(
    isOnline: Boolean,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    onTitleClick: (() -> Unit)? = null,
    titleContentDescription: String? = null,
) {
    val colors = LanternTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .then(
                        if (onTitleClick != null) {
                            Modifier.clickable(onClick = onTitleClick)
                        } else {
                            Modifier
                        },
                    )
                    .then(
                        if (titleContentDescription != null) {
                            Modifier.semantics {
                                contentDescription = titleContentDescription
                            }
                        } else {
                            Modifier
                        },
                    )
                    .padding(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LanternMark(size = 42.dp)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.ask_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.textPrimary,
                    maxLines = 1,
                )
            }
            Spacer(Modifier.weight(1f))
            SignalChip(isOnline = isOnline)
            Spacer(Modifier.width(2.dp))
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = stringResource(R.string.cd_settings),
                    tint = colors.textSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        HorizontalDivider(
            color = colors.border.copy(alpha = 0.45f),
            thickness = 1.dp,
        )
    }
}
