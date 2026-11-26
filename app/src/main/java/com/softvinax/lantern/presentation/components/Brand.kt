package com.softvinax.lantern.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.softvinax.lantern.presentation.theme.LanternTheme

@Composable
fun LanternMark(
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
) {
    val colors = LanternTheme.colors
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size / 4))
            .background(Brush.linearGradient(colors.primaryGradient)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "L",
            style = MaterialTheme.typography.headlineMedium,
            color = colors.onAccent,
        )
    }
}
