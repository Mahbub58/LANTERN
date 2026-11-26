package com.softvinax.lantern.data.remote

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.softvinax.lantern.domain.repository.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext

/**
 * Anonymous Firebase Auth. No email, phone number or password is ever asked
 * for. The uid is what Firestore rules use to scope reports.
 *
 * Firebase Auth persists the anonymous user on the device, so the app keeps
 * working offline after the first successful sign-in. If the very first launch
 * is offline, [ensureAnonymousUid] fails and the report repository keeps the
 * report in a local queue until auth is possible.
 */
@Singleton
class FirebaseAuthRepository @Inject constructor(
    @ApplicationContext context: Context,
) : AuthRepository {

    private val auth = FirebaseAuth.getInstance()
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    override val uid: Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val current = firebaseAuth.currentUser?.uid
            if (current != null) cacheUid(current)
            trySend(current)
        }
        auth.addAuthStateListener(listener)
        trySend(auth.currentUser?.uid)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.distinctUntilChanged()

    override suspend fun ensureAnonymousUid(): String {
        auth.currentUser?.uid?.let { uid ->
            cacheUid(uid)
            return uid
        }
        return withContext(Dispatchers.IO) {
            try {
                val result = Tasks.await(auth.signInAnonymously())
                val uid = result.user?.uid
                    ?: throw IllegalStateException("Anonymous sign-in returned no user")
                cacheUid(uid)
                Log.i(TAG, "Anonymous sign-in succeeded")
                uid
            } catch (t: Throwable) {
                throw IllegalStateException(
                    "Anonymous sign-in needs one online launch before offline reports can sync",
                    t,
                )
            }
        }
    }

    private fun cacheUid(uid: String) {
        if (prefs.getString(KEY_UID, null) != uid) {
            prefs.edit().putString(KEY_UID, uid).apply()
        }
    }

    private companion object {
        const val TAG = "LanternAuth"
        const val PREFS = "lantern_auth"
        const val KEY_UID = "anonymous_uid"
    }
}
