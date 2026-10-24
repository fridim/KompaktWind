package com.kompaktwind.ui.spots

import com.google.common.truth.Truth.assertThat
import com.kompaktwind.data.HourPoint
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class ForecastPreviewTest {
    @Test fun `target rounds to the nearest local hour`() {
        val now = Instant.parse("2026-10-23T10:23:00Z").toEpochMilli()
        assertThat(previewTargetEpochMs(now, 6 * 60, ZoneId.of("UTC")))
            .isEqualTo(Instant.parse("2026-10-23T16:00:00Z").toEpochMilli())
        assertThat(previewTargetEpochMs(now, 0, ZoneId.of("UTC")))
            .isEqualTo(Instant.parse("2026-10-23T10:00:00Z").toEpochMilli())
    }

    @Test fun `target rounds up past the half hour`() {
        val now = Instant.parse("2026-10-23T10:40:00Z").toEpochMilli()
        assertThat(previewTargetEpochMs(now, 0, ZoneId.of("UTC")))
            .isEqualTo(Instant.parse("2026-10-23T11:00:00Z").toEpochMilli())
    }

    @Test fun `nearest hour picks the closest forecast`() {
        val points = listOf(hour(10), hour(11), hour(12))
        val target = Instant.parse("2026-10-23T11:20:00Z").toEpochMilli()
        assertThat(nearestHour(points, target)?.timeEpochMs).isEqualTo(hour(11).timeEpochMs)
    }

    @Test fun `nearest hour returns null for an empty forecast`() {
        assertThat(nearestHour(emptyList(), 0)).isNull()
    }

    private fun hour(h: Int) = HourPoint(
        timeEpochMs = Instant.parse("2026-10-23T${h.toString().padStart(2, '0')}:00:00Z").toEpochMilli()
    )
}
