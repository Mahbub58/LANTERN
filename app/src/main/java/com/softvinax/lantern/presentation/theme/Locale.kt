package com.softvinax.lantern.presentation.theme

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.LayoutDirection
import com.softvinax.lantern.domain.model.AppLanguage
import java.util.Locale

/**
 * In-app language + RTL without restarting the activity and without AppCompat.
 *
 * Only the resource lookups are redirected: [LocalResources] resolves from
 * `values/`, `values-ur/` and `values-b+ur+Latn/`, and [LocalConfiguration] carries the
 * locale for anything that formats dates or numbers. [LocalContext] intentionally stays
 * the Activity so `hiltViewModel()` keeps finding the activity-scoped factory and so
 * `startActivity` (dialer, share sheet) runs from an Activity context.
 */
@Composable
fun ProvideAppLanguage(
    language: AppLanguage,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val locale = remember(language) {
        when (language) {
            AppLanguage.EN -> Locale.forLanguageTag("en")
            AppLanguage.UR -> Locale.forLanguageTag("ur")
            AppLanguage.ROMAN_UR -> Locale.forLanguageTag("ur-Latn")
        }
    }
    val localizedResources = remember(locale, context) {
        val configuration = Configuration(context.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        context.createConfigurationContext(configuration).resources
    }
    val direction = if (language == AppLanguage.UR) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(
        LocalResources provides localizedResources,
        LocalConfiguration provides localizedResources.configuration,
        LocalLayoutDirection provides direction,
        content = content,
    )
}
