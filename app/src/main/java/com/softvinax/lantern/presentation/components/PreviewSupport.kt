package com.softvinax.lantern.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.softvinax.lantern.domain.model.AppLanguage
import com.softvinax.lantern.presentation.theme.LanternTheme
import com.softvinax.lantern.presentation.theme.ProvideAppLanguage

@Composable
fun LanternPreview(content: @Composable () -> Unit) {
    LanternTheme {
        ProvideAppLanguage(AppLanguage.EN) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LanternTheme.colors.background),
            ) {
                content()
            }
        }
    }
}

@Composable
fun LanternPreviewRtl(content: @Composable () -> Unit) {
    LanternTheme {
        ProvideAppLanguage(AppLanguage.UR) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LanternTheme.colors.background),
            ) {
                content()
            }
        }
    }
}
