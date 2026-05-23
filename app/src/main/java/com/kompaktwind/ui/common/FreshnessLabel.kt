package com.kompaktwind.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import com.mudita.mmd.components.text.TextMMD
import java.util.concurrent.TimeUnit

@Composable
fun FreshnessLabel(fetchedAt: Long, now: Long = System.currentTimeMillis()) {
    val ageMs = (now - fetchedAt).coerceAtLeast(0)
    val text = when {
        ageMs < TimeUnit.MINUTES.toMillis(1) -> "updated just now"
        ageMs < TimeUnit.HOURS.toMillis(1) -> "updated ${TimeUnit.MILLISECONDS.toMinutes(ageMs)} min ago"
        ageMs < TimeUnit.DAYS.toMillis(1) -> "updated ${TimeUnit.MILLISECONDS.toHours(ageMs)} h ago"
        else -> "updated ${TimeUnit.MILLISECONDS.toDays(ageMs)} d ago"
    }
    TextMMD(text = text, fontSize = 14.sp)
}
