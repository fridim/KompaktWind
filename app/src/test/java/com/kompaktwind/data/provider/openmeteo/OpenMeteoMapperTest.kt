package com.kompaktwind.data.provider.openmeteo

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class OpenMeteoMapperTest {

    private val forecast = OpenMeteoForecastDto(
        latitude = 50.95,
        longitude = 1.85,
        timezone = "Europe/Paris",
        utcOffsetSeconds = 7200,
        hourly = ForecastHourlyDto(
            time = listOf("2026-05-23T00:00", "2026-05-23T01:00"),
            temperature2m = listOf(14.0, 14.2),
            windSpeed10m = listOf(3.5, 3.7),
            windGusts10m = listOf(6.0, 6.4),
            windDirection10m = listOf(220, 215),
            precipitation = listOf(0.0, 0.1),
            precipitationProbability = listOf(10, 15)
        )
    )

    private val marine = OpenMeteoMarineDto(
        latitude = 50.95,
        longitude = 1.85,
        timezone = "Europe/Paris",
        hourly = MarineHourlyDto(
            time = listOf("2026-05-23T00:00", "2026-05-23T01:00"),
            waveHeight = listOf(1.2, 1.3),
            wavePeriod = listOf(8.0, 8.1),
            seaSurfaceTemperature = listOf(16.0, 16.0),
            seaLevelHeight = listOf(0.8, 0.9)
        )
    )

    @Test fun `mapper produces aligned hour points`() {
        val result = OpenMeteoMapper.toForecast("spot1", forecast, marine, fetchedAt = 1000L)
        assertThat(result.hours).hasSize(2)
        assertThat(result.hours[0].tempC).isEqualTo(14.0)
        assertThat(result.hours[0].waveHeightM).isEqualTo(1.2)
        assertThat(result.hours[0].seaLevelM).isEqualTo(0.8)
    }

    @Test fun `mapper without marine leaves wave fields null`() {
        val result = OpenMeteoMapper.toForecast("spot1", forecast, marine = null, fetchedAt = 1000L)
        assertThat(result.hours[0].waveHeightM).isNull()
        assertThat(result.hours[0].waterTempC).isNull()
        assertThat(result.hours[0].seaLevelM).isNull()
    }

    @Test fun `mapper parses ISO time to epoch ms using utc offset`() {
        // 2026-05-23T00:00 local (UTC+2) = 2026-05-22T22:00Z = 1779487200000 ms
        val result = OpenMeteoMapper.toForecast("spot1", forecast, marine = null, fetchedAt = 0L)
        assertThat(result.hours[0].timeEpochMs).isEqualTo(1779487200000L)
    }

    @Test fun `mapper applies DST rules instead of a fixed offset`() {
        val dstForecast = forecast.copy(
            utcOffsetSeconds = 3600,
            hourly = forecast.hourly.copy(
                time = listOf("2026-03-29T01:00", "2026-03-29T03:00")
            )
        )
        val result = OpenMeteoMapper.toForecast("spot1", dstForecast, marine = null, fetchedAt = 0L)
        // 01:00 CET = 00:00Z, 03:00 CEST = 01:00Z. A fixed +01:00 would put the second hour at 02:00Z.
        assertThat(result.hours[0].timeEpochMs).isEqualTo(1774742400000L)
        assertThat(result.hours[1].timeEpochMs).isEqualTo(1774746000000L)
    }

    @Test fun `mapper drops mismatched marine hours by aligning on count`() {
        val shortMarine = marine.copy(hourly = marine.hourly.copy(time = listOf("2026-05-23T00:00")))
        val result = OpenMeteoMapper.toForecast("spot1", forecast, shortMarine, fetchedAt = 0L)
        assertThat(result.hours).hasSize(2)
        assertThat(result.hours[0].waveHeightM).isEqualTo(1.2)
        assertThat(result.hours[1].waveHeightM).isNull()
    }
}
