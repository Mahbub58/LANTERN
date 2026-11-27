package com.mahbub.lantern.presentation.navigation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    val appCheckFailure by viewModel.appCheckFailure.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(appCheckFailure) {
        val failure = appCheckFailure ?: return@LaunchedEffect
        val token = failure.debugToken
        val result = snackbarHostState.showSnackbar(
            message = if (token != null) {
                context.getString(R.string.app_check_failed_token, token)
            } else {
                context.getString(R.string.app_check_failed)
            },
            actionLabel = context.getString(
                if (token != null) R.string.app_check_copy_token else R.string.app_check_retry,
            ),
            withDismissAction = true,
            duration = SnackbarDuration.Long,
        )
        when (result) {
            SnackbarResult.ActionPerformed -> {
                if (token != null) {
                    copyToClipboard(context, token)
                    snackbarHostState.showSnackbar(
                        message = context.getString(R.string.app_check_token_copied),
                        duration = SnackbarDuration.Short,
                    )
                    viewModel.dismissAppCheckFailure()
                } else {
                    viewModel.retryAppCheck()
                }
            }

            SnackbarResult.Dismissed -> viewModel.dismissAppCheckFailure()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ProvideAppLanguage(state.language) {
            when {
                state.isLoading -> SplashScreen()
                !state.languageChosen -> LanguageRoute(onSaved = {})
                else -> LanternNavHost()
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .padding(bottom = 88.dp),
        )
    }
}

private fun copyToClipboard(context: Context, token: String) {
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText("App Check debug token", token))
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
