package com.kompaktwind.data.provider.openmeteo

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Test

class OpenMeteoProviderTest {

    @Test fun `arome provider requests arome model and clamps the horizon`() = runBlocking {
        val forecastApi = FakeForecastApi()
        val provider = OpenMeteoProvider(
            id = OpenMeteoProvider.AROME_FR_ID,
            displayName = "AROME FR",
            attributionText = "Météo-France AROME via Open-Meteo",
            models = OpenMeteoProvider.AROME_FR_MODEL,
            maxDays = 3,
            forecastApi = forecastApi,
            marineApi = FakeMarineApi(),
            now = { 0L }
        )

        val result = provider.fetchForecast(48.39, -4.49, wantMarine = false, days = 7)

        assertThat(result.isSuccess).isTrue()
        assertThat(forecastApi.models).isEqualTo(OpenMeteoProvider.AROME_FR_MODEL)
        assertThat(forecastApi.forecastDays).isEqualTo(3)
    }

    @Test fun `default provider leaves model selection to best match`() = runBlocking {
        val forecastApi = FakeForecastApi()
        val provider = OpenMeteoProvider(
            id = OpenMeteoProvider.ID,
            displayName = "Open-Meteo",
            attributionText = "Data provided by Open-Meteo",
            forecastApi = forecastApi,
            marineApi = FakeMarineApi()
        )

        val result = provider.fetchForecast(0.0, 0.0, wantMarine = false, days = 7)

        assertThat(result.isSuccess).isTrue()
        assertThat(forecastApi.models).isNull()
        assertThat(forecastApi.forecastDays).isEqualTo(7)
    }

    private class FakeForecastApi : OpenMeteoForecastApi {
        var models: String? = null
        var forecastDays: Int? = null

        override suspend fun forecast(
            lat: Double,
            lon: Double,
            hourly: String,
            windSpeedUnit: String,
            models: String?,
            forecastDays: Int,
            timezone: String
        ): OpenMeteoForecastDto {
            this.models = models
            this.forecastDays = forecastDays
            return OpenMeteoForecastDto(
                latitude = lat,
                longitude = lon,
                timezone = timezone,
                hourly = ForecastHourlyDto(time = emptyList())
            )
        }
    }

    @Test fun `marine request includes tide height`() = runBlocking {
        val marineApi = FakeMarineApi()
        val provider = OpenMeteoProvider(
            id = OpenMeteoProvider.ID,
            displayName = "Open-Meteo",
            attributionText = "Data provided by Open-Meteo",
            forecastApi = FakeForecastApi(),
            marineApi = marineApi
        )

        val result = provider.fetchForecast(0.0, 0.0, wantMarine = true, days = 7)

        assertThat(result.isSuccess).isTrue()
        assertThat(marineApi.hourly).contains("sea_level_height_msl")
    }

    private class FakeMarineApi : OpenMeteoMarineApi {
        var hourly: String? = null

        override suspend fun marine(
            lat: Double,
            lon: Double,
            hourly: String,
            forecastDays: Int,
            timezone: String
        ): OpenMeteoMarineDto {
            this.hourly = hourly
            return OpenMeteoMarineDto(
                latitude = lat,
                longitude = lon,
                timezone = timezone,
                hourly = MarineHourlyDto(time = emptyList())
            )
        }
    }
}
