package com.mahbub.lantern.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mahbub.lantern.R
import com.mahbub.lantern.presentation.theme.LanternTheme

@Composable
fun LanternTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    onTitleClick: (() -> Unit)? = null,
    titleContentDescription: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = LanternTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background)
            .statusBarsPadding()
            .heightIn(min = 64.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back),
                    tint = colors.textPrimary,
                )
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .then(
                    if (onTitleClick != null) {
                        Modifier.clickable(onClick = onTitleClick)
                    } else {
                        Modifier
                    },
                )
                .then(
                    if (titleContentDescription != null) {
                        Modifier.semantics { contentDescription = titleContentDescription }
                    } else {
                        Modifier
                    },
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            content = actions,
        )
    }
}

@Composable
fun TopBarAction(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(48.dp)
            .semantics { this.contentDescription = contentDescription },
    ) {
        content()
    }
}
