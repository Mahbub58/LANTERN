package com.mahbub.lantern.data.appcheck

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.firebase.appcheck.FirebaseAppCheck
import com.mahbub.lantern.domain.repository.AppCheckFailure
import com.mahbub.lantern.domain.repository.AppCheckRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Watches App Check attestation.
 *
 * Firebase App Check has no failure callback, so the monitor requests a token
 * at launch and whenever the UI retries. A failed token request is surfaced as
 * an [AppCheckFailure]; the root screen shows it in a snackbar.
 */
@Singleton
class AppCheckMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
) : AppCheckRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val checking = AtomicBoolean(false)
    private val _failure = MutableStateFlow<AppCheckFailure?>(null)
    override val failure: StateFlow<AppCheckFailure?> = _failure.asStateFlow()

    /** Called once from the Application after the provider is installed. */
    fun start() = retry()

    override fun retry() {
        if (!checking.compareAndSet(false, true)) return
        scope.launch {
            try {
                Tasks.await(FirebaseAppCheck.getInstance().getAppCheckToken(false))
                _failure.value = null
                Log.i(TAG, "App Check token acquired")
            } catch (t: Throwable) {
                Log.w(TAG, "App Check token failed", t)
                _failure.value = AppCheckFailure(
                    reason = t.message?.takeIf { it.isNotBlank() } ?: "App Check token unavailable",
                    debugToken = AppCheckDebugToken.current(context),
                )
            } finally {
                checking.set(false)
            }
        }
    }

    override fun dismiss() {
        _failure.value = null
    }

    private companion object {
        const val TAG = "LanternAppCheck"
    }
}
