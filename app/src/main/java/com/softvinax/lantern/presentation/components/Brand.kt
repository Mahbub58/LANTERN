package com.softvinax.lantern.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.softvinax.lantern.presentation.theme.LanternTheme

/**
 * The Lantern mark: a small gradient tile with a line-art lantern.
 *
 * Kept as a Canvas so the brand mark stays crisp at any size and does not add
 * an image asset to the APK.
 */
@Composable
fun LanternMark(
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
) {
    val colors = LanternTheme.colors
    Canvas(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size / 4))
            .background(Brush.linearGradient(colors.primaryGradient)),
    ) {
        val w = this.size.width
        val h = this.size.height
        val ink = colors.textPrimary
        val stroke = w * 0.055f
        val line = Stroke(width = stroke, cap = StrokeCap.Round)

        // Handle
        drawArc(
            color = ink,
            startAngle = 200f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(w * 0.30f, h * 0.10f),
            size = Size(w * 0.40f, h * 0.30f),
            style = line,
        )

        // Top cap
        drawRoundRect(
            color = ink,
            topLeft = Offset(w * 0.28f, h * 0.28f),
            size = Size(w * 0.44f, h * 0.09f),
            cornerRadius = CornerRadius(w * 0.04f),
            style = line,
        )

        // Body
        drawRoundRect(
            color = ink,
            topLeft = Offset(w * 0.25f, h * 0.40f),
            size = Size(w * 0.50f, h * 0.42f),
            cornerRadius = CornerRadius(w * 0.08f),
            style = line,
        )

        // Flame
        val flame = Path().apply {
            moveTo(w * 0.50f, h * 0.48f)
            cubicTo(w * 0.61f, h * 0.55f, w * 0.58f, h * 0.67f, w * 0.50f, h * 0.71f)
            cubicTo(w * 0.42f, h * 0.67f, w * 0.39f, h * 0.55f, w * 0.50f, h * 0.48f)
            close()
        }
        drawPath(flame, color = ink)

        // Base
        drawRoundRect(
            color = ink,
            topLeft = Offset(w * 0.32f, h * 0.84f),
            size = Size(w * 0.36f, h * 0.06f),
            cornerRadius = CornerRadius(w * 0.03f),
            style = line,
        )
    }
}
