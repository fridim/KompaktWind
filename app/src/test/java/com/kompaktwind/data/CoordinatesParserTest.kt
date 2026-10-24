package com.kompaktwind.data

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CoordinatesParserTest {
    @Test fun `parses decimal pairs`() {
        assertThat(CoordinatesParser.parse("48.123, -4.567")).isEqualTo(LatLon(48.123, -4.567))
        assertThat(CoordinatesParser.parse("48.123 -4.567")).isEqualTo(LatLon(48.123, -4.567))
    }

    @Test fun `parses hemisphere decimals`() {
        assertThat(CoordinatesParser.parse("48.123N, 4.567W")).isEqualTo(LatLon(48.123, -4.567))
        assertThat(CoordinatesParser.parse("S48.123 E4.567")).isEqualTo(LatLon(-48.123, 4.567))
    }

    @Test fun `parses degrees minutes seconds`() {
        val parsed = CoordinatesParser.parse("48°07'24\"N 4°33'54\"W")!!
        assertThat(parsed.lat).isWithin(0.001).of(48.1233)
        assertThat(parsed.lon).isWithin(0.001).of(-4.565)
    }

    @Test fun `parses degrees decimal minutes`() {
        val parsed = CoordinatesParser.parse("N48 07.400 W004 33.900")!!
        assertThat(parsed.lat).isWithin(0.001).of(48.1233)
        assertThat(parsed.lon).isWithin(0.001).of(-4.565)
    }

    @Test fun `parses spaced dms with hemisphere letters`() {
        val parsed = CoordinatesParser.parse("48 07 24 N 4 33 54 W")!!
        assertThat(parsed.lat).isWithin(0.001).of(48.1233)
        assertThat(parsed.lon).isWithin(0.001).of(-4.565)
    }

    @Test fun `rejects invalid input`() {
        assertThat(CoordinatesParser.parse("hello")).isNull()
        assertThat(CoordinatesParser.parse("95, 4")).isNull()
        assertThat(CoordinatesParser.parse("48, 200")).isNull()
    }
}
