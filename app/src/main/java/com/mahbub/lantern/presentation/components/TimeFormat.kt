package com.mahbub.lantern.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mahbub.lantern.R

@Composable
fun relativeTime(
    epochMillis: Long,
    nowMillis: Long = System.currentTimeMillis(),
): String {
    val elapsed = (nowMillis - epochMillis).coerceAtLeast(0L)
    val minutes = elapsed / 60_000L
    return when {
        minutes < 1L -> stringResource(R.string.time_just_now)
        minutes < 60L -> stringResource(R.string.time_min_ago, minutes.toInt())
        minutes < 120L -> stringResource(R.string.time_hour_ago)
        minutes < 1_440L -> stringResource(R.string.time_hours_ago, (minutes / 60L).toInt())
        minutes < 2_880L -> stringResource(R.string.time_day_ago)
        else -> stringResource(R.string.time_days_ago, (minutes / 1_440L).toInt())
    }
}

@Composable
fun savedAgoLabel(
    epochMillis: Long,
    nowMillis: Long = System.currentTimeMillis(),
): String = stringResource(R.string.ask_alert_saved, relativeTime(epochMillis, nowMillis))
