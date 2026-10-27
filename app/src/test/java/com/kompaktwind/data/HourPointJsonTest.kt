package com.kompaktwind.data

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HourPointJsonTest {
    @Test fun `round trips tide height`() {
        val point = HourPoint(timeEpochMs = 1_000L, seaLevelM = 1.25)

        val decoded = HourPointJson.decode(HourPointJson.encode(listOf(point)))

        assertThat(decoded.single().seaLevelM).isEqualTo(1.25)
    }

    @Test fun `decodes legacy json without sea level field`() {
        val legacy = """[{"t":1000,"ws":5.0}]"""

        val decoded = HourPointJson.decode(legacy)

        assertThat(decoded).hasSize(1)
        assertThat(decoded.single().seaLevelM).isNull()
    }
}
