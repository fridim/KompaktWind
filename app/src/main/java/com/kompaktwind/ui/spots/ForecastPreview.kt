package com.kompaktwind.ui.spots

import com.kompaktwind.data.HourPoint
import java.time.Instant
import java.time.ZoneId
import kotlin.math.abs

data class ForecastPreviewState(
    val targetEpochMs: Long,
    val values: Map<String, HourPoint> = emptyMap(),
    val loading: Boolean = true
)

internal fun previewTargetEpochMs(
    nowMs: Long,
    offsetMinutes: Long,
    zoneId: ZoneId = ZoneId.systemDefault()
): Long {
    val hourMs = 3_600_000L
    val raw = nowMs + offsetMinutes * 60_000L
    val offsetMs = zoneId.rules.getOffset(Instant.ofEpochMilli(raw)).totalSeconds * 1000L
    val rounded = ((raw + offsetMs + hourMs / 2) / hourMs) * hourMs
    return rounded - offsetMs
}

internal fun nearestHour(points: List<HourPoint>, targetMs: Long): HourPoint? =
    points.minByOrNull { abs(it.timeEpochMs - targetMs) }
