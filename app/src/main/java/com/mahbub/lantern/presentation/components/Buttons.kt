package com.mahbub.lantern.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mahbub.lantern.presentation.theme.LanternTheme

@Composable
fun PrimaryGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
    contentDescription: String? = null,
    shape: Shape = RoundedCornerShape(28.dp),
    glow: Boolean = true,
    minHeight: Dp = 60.dp,
) {
    GradientButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        loading = loading,
        gradient = LanternTheme.colors.primaryGradient,
        icon = icon,
        contentDescription = contentDescription,
        shape = shape,
        glow = glow,
        minHeight = minHeight,
    )
}

@Composable
fun CloudGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    contentDescription: String? = null,
) {
    val colors = LanternTheme.colors
    GradientButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        loading = loading,
        gradient = colors.cloudGradient,
        icon = LanternIcons.Cloud,
        contentDescription = contentDescription,
        shape = RoundedCornerShape(28.dp),
        glow = false,
        minHeight = 56.dp,
        disabledContainer = colors.surfaceElevated,
        disabledContent = colors.blue.copy(alpha = 0.72f),
        disabledBorder = colors.blue.copy(alpha = 0.25f),
    )
}

@Composable
private fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    loading: Boolean,
    gradient: List<Color>,
    icon: ImageVector? = null,
    contentDescription: String? = null,
    shape: Shape = RoundedCornerShape(28.dp),
    glow: Boolean = false,
    minHeight: Dp = 60.dp,
    disabledContainer: Color = Color.Unspecified,
    disabledContent: Color? = null,
    disabledBorder: Color? = null,
) {
    val colors = LanternTheme.colors
    val brush: Brush = if (enabled) {
        Brush.linearGradient(gradient)
    } else {
        SolidColor(
            if (disabledContainer == Color.Unspecified) colors.surfaceVariant else disabledContainer,
        )
    }
    val contentColor = if (enabled) colors.onAccent else (disabledContent ?: colors.textSecondary)
    val glowModifier = if (glow && enabled) {
        Modifier.shadow(
            elevation = 18.dp,
            shape = shape,
            clip = false,
            ambientColor = gradient.first().copy(alpha = 0.55f),
            spotColor = gradient.last().copy(alpha = 0.55f),
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .then(glowModifier)
            .clip(shape)
            .background(brush)
            .then(
                if (!enabled && disabledBorder != null) {
                    Modifier.border(1.dp, disabledBorder, shape)
                } else {
                    Modifier
                },
            )
            .clickable(
                enabled = enabled && !loading,
                role = Role.Button,
                onClick = onClick,
            )
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 20.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = contentColor,
                    strokeWidth = 2.dp,
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = contentColor,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun OutlineActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = LanternTheme.colors
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, if (enabled) colors.border else colors.border.copy(alpha = 0.5f)),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) colors.textPrimary else colors.textSecondary,
        )
    }
}


@Composable
fun OutlinePillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    val colors = LanternTheme.colors
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 60.dp),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, colors.border),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = colors.surface,
            contentColor = colors.textPrimary,
            disabledContainerColor = colors.surfaceVariant,
            disabledContentColor = colors.textSecondary,
        ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) colors.textPrimary else colors.textSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = if (enabled) colors.textPrimary else colors.textSecondary,
            )
        }
    }
}

@Composable
fun LanternTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 48.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) LanternTheme.colors.amber else LanternTheme.colors.textSecondary,
        )
    }
}
