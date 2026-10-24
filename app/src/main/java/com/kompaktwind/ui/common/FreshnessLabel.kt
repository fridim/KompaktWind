package com.kompaktwind.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.kompaktwind.R
import java.util.concurrent.TimeUnit

@Composable
fun freshnessText(fetchedAt: Long, now: Long = System.currentTimeMillis()): String {
    val ageMs = (now - fetchedAt).coerceAtLeast(0)
    return when {
        ageMs < TimeUnit.MINUTES.toMillis(1) -> stringResource(R.string.age_now)
        ageMs < TimeUnit.HOURS.toMillis(1) ->
            stringResource(R.string.age_minutes, TimeUnit.MILLISECONDS.toMinutes(ageMs).toInt())
        else ->
            stringResource(R.string.age_hours, TimeUnit.MILLISECONDS.toHours(ageMs).toInt())
    }
}
