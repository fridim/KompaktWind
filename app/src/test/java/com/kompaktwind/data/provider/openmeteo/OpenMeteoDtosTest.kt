package com.kompaktwind.data.provider.openmeteo

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import org.junit.Test

class OpenMeteoDtosTest {
    private val json = Json { ignoreUnknownKeys = true }

    private fun loadResource(path: String): String =
        checkNotNull(this::class.java.classLoader!!.getResourceAsStream(path))
            .use { it.bufferedReader().readText() }

    @Test fun `forecast DTO parses real fixture`() {
        val raw = loadResource("openmeteo/forecast_paris.json")
        val dto = json.decodeFromString<OpenMeteoForecastDto>(raw)
        assertThat(dto.timezone).isEqualTo("Europe/Paris")
        assertThat(dto.hourly.time).hasSize(3)
        assertThat(dto.hourly.temperature2m).isEqualTo(listOf(14.2, 13.8, 13.4))
        assertThat(dto.hourly.windSpeed10m).isEqualTo(listOf(3.5, 3.8, 4.1))
        assertThat(dto.hourly.windDirection10m).isEqualTo(listOf(220, 215, 210))
    }

    @Test fun `marine DTO parses real fixture`() {
        val raw = loadResource("openmeteo/marine_calais.json")
        val dto = json.decodeFromString<OpenMeteoMarineDto>(raw)
        assertThat(dto.hourly.waveHeight).isEqualTo(listOf(1.2, 1.3, 1.4))
        assertThat(dto.hourly.seaSurfaceTemperature).isEqualTo(listOf(16.0, 16.0, 15.9))
    }
}
