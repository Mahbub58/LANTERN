package com.mahbub.lantern.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mahbub.lantern.R
import com.mahbub.lantern.domain.model.AppLanguage
import com.mahbub.lantern.presentation.components.LanternMark
import com.mahbub.lantern.presentation.components.LanternPreview
import com.mahbub.lantern.presentation.language.LanguageRoute
import com.mahbub.lantern.presentation.theme.LanternTheme
import com.mahbub.lantern.presentation.theme.ProvideAppLanguage

@Composable
fun LanternRoot(
    viewModel: RootViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ProvideAppLanguage(state.language) {
        when {
            state.isLoading -> SplashScreen()
            !state.languageChosen -> LanguageRoute(onSaved = {})
            else -> LanternNavHost()
        }
    }
}

@Composable
fun SplashScreen() {
    val colors = LanternTheme.colors
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            LanternMark(size = 76.dp)
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                color = colors.textPrimary,
            )
            CircularProgressIndicator(
                color = colors.amber,
                trackColor = colors.surfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Preview(name = "Splash", widthDp = 390, heightDp = 844)
@Composable
private fun SplashPreview() {
    LanternPreview {
        ProvideAppLanguage(AppLanguage.EN) {
            SplashScreen()
        }
    }
}
