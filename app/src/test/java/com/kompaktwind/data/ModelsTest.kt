package com.kompaktwind.data

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ModelsTest {
    @Test fun `HourPoint allows all numeric fields to be null`() {
        val h = HourPoint(timeEpochMs = 0L)
        assertThat(h.tempC).isNull()
        assertThat(h.windSpeedMs).isNull()
    }

    @Test fun `Spot stores coordinates and coastal flag`() {
        val s = Spot(
            id = "abc",
            name = "Calais",
            lat = 50.95,
            lon = 1.85,
            isCoastal = true,
            createdAt = 0L
        )
        assertThat(s.isCoastal).isTrue()
    }
}
