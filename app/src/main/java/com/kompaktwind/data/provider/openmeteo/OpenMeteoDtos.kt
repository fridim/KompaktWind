package com.kompaktwind.data.provider.openmeteo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenMeteoForecastDto(
    val latitude: Double,
    val longitude: Double,
    val timezone: String,
    @SerialName("utc_offset_seconds") val utcOffsetSeconds: Int = 0,
    val hourly: ForecastHourlyDto
)

@Serializable
data class ForecastHourlyDto(
    val time: List<String>,
    @SerialName("temperature_2m") val temperature2m: List<Double?> = emptyList(),
    @SerialName("wind_speed_10m") val windSpeed10m: List<Double?> = emptyList(),
    @SerialName("wind_gusts_10m") val windGusts10m: List<Double?> = emptyList(),
    @SerialName("wind_direction_10m") val windDirection10m: List<Int?> = emptyList(),
    @SerialName("precipitation") val precipitation: List<Double?> = emptyList(),
    @SerialName("precipitation_probability") val precipitationProbability: List<Int?> = emptyList()
)

@Serializable
data class OpenMeteoMarineDto(
    val latitude: Double,
    val longitude: Double,
    val timezone: String,
    val hourly: MarineHourlyDto
)

@Serializable
data class MarineHourlyDto(
    val time: List<String>,
    @SerialName("wave_height") val waveHeight: List<Double?> = emptyList(),
    @SerialName("wave_period") val wavePeriod: List<Double?> = emptyList(),
    @SerialName("sea_surface_temperature") val seaSurfaceTemperature: List<Double?> = emptyList()
)
