package com.mahbub.lantern.data.appcheck

import android.content.Context

/** Release builds attest with Play Integrity, so there is no debug token. */
object AppCheckDebugToken {

    @Suppress("UNUSED_PARAMETER")
    fun current(context: Context): String? = null
}
