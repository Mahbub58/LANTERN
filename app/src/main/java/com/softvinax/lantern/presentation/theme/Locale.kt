package com.softvinax.lantern.presentation.theme

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.softvinax.lantern.domain.model.AppLanguage
import java.util.Locale

/**
 * In-app language + RTL. Wraps [LocalContext] with a locale-overridden context so
 * `stringResource` resolves from values/, values-ur/ and values-b+ur+Latn/ without
 * restarting the activity or requiring AppCompat.
 */
@Composable
fun ProvideAppLanguage(
    language: AppLanguage,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val localizedContext = remember(language, context) {
        val locale = when (language) {
            AppLanguage.EN -> Locale.forLanguageTag("en")
            AppLanguage.UR -> Locale.forLanguageTag("ur")
            AppLanguage.ROMAN_UR -> Locale.forLanguageTag("ur-Latn")
        }
        val configuration = Configuration(context.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        context.createConfigurationContext(configuration)
    }
    val direction = if (language == AppLanguage.UR) LayoutDirection.Rtl else LayoutDirection.Ltr
    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalLayoutDirection provides direction,
        content = content,
    )
}
