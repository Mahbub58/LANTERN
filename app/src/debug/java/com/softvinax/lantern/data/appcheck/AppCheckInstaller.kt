package com.softvinax.lantern.data.appcheck

import android.content.Context
import android.util.Log
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

/**
 * Debug builds use the App Check debug provider so judges can run the app
 * before Play Integrity is configured. The debug secret is printed by the
 * provider on the first token request; register it in
 * Firebase console > App Check > Apps > Manage debug tokens.
 */
object AppCheckInstaller {

    fun install(context: Context) {
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
            DebugAppCheckProviderFactory.getInstance(),
        )
        Log.i(
            "LanternAppCheck",
            "App Check debug provider installed. Filter logcat for DebugAppCheckProvider to read the debug token.",
        )
    }
}
