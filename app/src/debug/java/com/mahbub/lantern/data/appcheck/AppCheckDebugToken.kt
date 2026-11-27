package com.mahbub.lantern.data.appcheck

import android.content.Context
import java.io.File

/**
 * Reads the App Check debug token that `DebugAppCheckProvider` stores on disk.
 *
 * The debug provider prints the token to logcat, but the snackbar needs it in
 * the UI so it can be copied. Debug builds only; release builds use Play
 * Integrity and have no debug token.
 */
object AppCheckDebugToken {

    private const val KEY = "com.google.firebase.appcheck.debug.DEBUG_SECRET"
    private const val PREFIX = "com.google.firebase.appcheck.debug.store."

    fun current(context: Context): String? = runCatching {
        val prefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
        val file = prefsDir.listFiles()
            ?.firstOrNull { it.name.startsWith(PREFIX) && it.name.endsWith(".xml") }
            ?: return@runCatching null

        context.getSharedPreferences(file.nameWithoutExtension, Context.MODE_PRIVATE)
            .getString(KEY, null)
            ?.takeIf { it.isNotBlank() }
    }.getOrNull()
}
