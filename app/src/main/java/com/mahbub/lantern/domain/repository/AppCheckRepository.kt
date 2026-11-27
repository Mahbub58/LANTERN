package com.mahbub.lantern.domain.repository

import kotlinx.coroutines.flow.StateFlow

/**
 * A failed App Check attestation. [atMillis] makes every failure a distinct
 * value so the UI shows a new snackbar after a failed retry.
 */
data class AppCheckFailure(
    val reason: String,
    /** Debug builds only: the token to register in the Firebase console. */
    val debugToken: String? = null,
    val atMillis: Long = System.currentTimeMillis(),
)

/**
 * App Check is installed before any Firebase call. If a token cannot be
 * obtained, Firebase-backed features are not trustworthy, so the UI tells the
 * person instead of failing silently.
 */
interface AppCheckRepository {
    val failure: StateFlow<AppCheckFailure?>

    /** Request a token again; clears [failure] on success. */
    fun retry()

    /** Hide the current failure. A later failure is reported again. */
    fun dismiss()
}
