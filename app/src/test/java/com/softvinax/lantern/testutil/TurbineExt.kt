package com.softvinax.lantern.testutil

import app.cash.turbine.ReceiveTurbine

/**
 * Consumes emissions until [predicate] matches. Keeps ViewModel tests robust to
 * the exact number of intermediate state updates.
 */
suspend fun <T> ReceiveTurbine<T>.awaitUntil(predicate: (T) -> Boolean): T {
    while (true) {
        val value = awaitItem()
        if (predicate(value)) return value
    }
}
