package com.softvinax.lantern

import android.app.Application
import android.util.Log
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.softvinax.lantern.data.appcheck.AppCheckInstaller
import com.softvinax.lantern.domain.repository.AuthRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class LanternApplication : Application() {

    @Inject
    lateinit var authRepository: AuthRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        // App Check must be installed before any AI Logic or Firestore call.
        AppCheckInstaller.install(this)
        FirebaseAppCheck.getInstance().getAppCheckToken(false)
            .addOnSuccessListener { Log.i(TAG, "App Check token acquired") }
            .addOnFailureListener { Log.w(TAG, "App Check token failed", it) }

        // Unlimited persistent cache keeps reports and alerts usable offline.
        runCatching {
            val settings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                .build()
            FirebaseFirestore.getInstance().firestoreSettings = settings
        }.onFailure { Log.w(TAG, "Could not enable Firestore persistence", it) }

        // Anonymous sign-in on first launch. If the first launch is offline the
        // report repository keeps submissions in a local queue until this works.
        scope.launch {
            runCatching { authRepository.ensureAnonymousUid() }
                .onFailure { Log.i(TAG, "Anonymous sign-in deferred", it) }
        }
    }

    private companion object {
        const val TAG = "LanternApp"
    }
}
