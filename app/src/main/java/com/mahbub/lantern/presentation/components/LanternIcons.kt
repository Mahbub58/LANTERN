package com.mahbub.lantern.presentation.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Small custom icons that are not in material-icons-core.
 *
 * The project deliberately avoids material-icons-extended (it is a large
 * dependency). These four paths are the only extra glyphs the redesign needs.
 */
object LanternIcons {

    val Cloud: ImageVector by lazy {
        ImageVector.Builder(
            name = "Cloud",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(19.35f, 10.04f)
                curveTo(18.67f, 6.59f, 15.64f, 4f, 12f, 4f)
                curveTo(9.11f, 4f, 6.6f, 5.64f, 5.35f, 8.04f)
                curveTo(2.34f, 8.36f, 0f, 10.91f, 0f, 14f)
                curveTo(0f, 17.31f, 2.69f, 20f, 6f, 20f)
                lineTo(19f, 20f)
                curveTo(21.76f, 20f, 24f, 17.76f, 24f, 15f)
                curveTo(24f, 12.36f, 21.95f, 10.22f, 19.35f, 10.04f)
                close()
            }
        }.build()
    }

    val Mic: ImageVector by lazy {
        ImageVector.Builder(
            name = "Mic",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 14f)
                curveTo(13.66f, 14f, 15f, 12.66f, 15f, 11f)
                lineTo(15f, 5f)
                curveTo(15f, 3.34f, 13.66f, 2f, 12f, 2f)
                curveTo(10.34f, 2f, 9f, 3.34f, 9f, 5f)
                lineTo(9f, 11f)
                curveTo(9f, 12.66f, 10.34f, 14f, 12f, 14f)
                close()
                moveTo(17f, 11f)
                curveTo(17f, 13.76f, 14.76f, 16f, 12f, 16f)
                curveTo(9.24f, 16f, 7f, 13.76f, 7f, 11f)
                lineTo(5f, 11f)
                curveTo(5f, 14.53f, 7.61f, 17.43f, 11f, 17.92f)
                lineTo(11f, 21f)
                lineTo(13f, 21f)
                lineTo(13f, 17.92f)
                curveTo(16.39f, 17.43f, 19f, 14.53f, 19f, 11f)
                close()
            }
        }.build()
    }

    val Chat: ImageVector by lazy {
        ImageVector.Builder(
            name = "Chat",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(20f, 2f)
                lineTo(4f, 2f)
                curveTo(2.9f, 2f, 2f, 2.9f, 2f, 4f)
                lineTo(2f, 22f)
                lineTo(6f, 18f)
                lineTo(20f, 18f)
                curveTo(21.1f, 18f, 22f, 17.1f, 22f, 16f)
                lineTo(22f, 4f)
                curveTo(22f, 2.9f, 21.1f, 2f, 20f, 2f)
                close()
            }
        }.build()
    }

    val Smartphone: ImageVector by lazy {
        ImageVector.Builder(
            name = "Smartphone",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.EvenOdd,
            ) {
                moveTo(17f, 1.01f)
                lineTo(7f, 1f)
                curveTo(5.9f, 1f, 5f, 1.9f, 5f, 3f)
                lineTo(5f, 21f)
                curveTo(5f, 22.1f, 5.9f, 23f, 7f, 23f)
                lineTo(17f, 23f)
                curveTo(18.1f, 23f, 19f, 22.1f, 19f, 21f)
                lineTo(19f, 3f)
                curveTo(19f, 1.9f, 18.1f, 1.01f, 17f, 1.01f)
                close()
                moveTo(17f, 19f)
                lineTo(7f, 19f)
                lineTo(7f, 5f)
                lineTo(17f, 5f)
                lineTo(17f, 19f)
                close()
            }
        }.build()
    }
}
