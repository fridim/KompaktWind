package com.kompaktwind.data.provider.openmeteo

import com.kompaktwind.data.Forecast
import com.kompaktwind.data.HourPoint
import java.time.LocalDateTime
import java.time.ZoneOffset

object OpenMeteoMapper {
    fun toForecast(
        spotId: String,
        forecast: OpenMeteoForecastDto,
        marine: OpenMeteoMarineDto?,
        fetchedAt: Long
    ): Forecast {
        val tzOffset = ZoneOffset.ofTotalSeconds(forecast.utcOffsetSeconds)
        val marineByTime: Map<String, Triple<Double?, Double?, Double?>> = marine?.let { m ->
            m.hourly.time.mapIndexed { i, t ->
                t to Triple(
                    m.hourly.waveHeight.getOrNull(i),
                    m.hourly.wavePeriod.getOrNull(i),
                    m.hourly.seaSurfaceTemperature.getOrNull(i)
                )
            }.toMap()
        } ?: emptyMap()

        val hours = forecast.hourly.time.mapIndexed { i, isoLocal ->
            val instant = LocalDateTime.parse(isoLocal).toInstant(tzOffset)
            val triple = marineByTime[isoLocal]
            HourPoint(
                timeEpochMs = instant.toEpochMilli(),
                tempC = forecast.hourly.temperature2m.getOrNull(i),
                windSpeedMs = forecast.hourly.windSpeed10m.getOrNull(i),
                windGustMs = forecast.hourly.windGusts10m.getOrNull(i),
                windDirDeg = forecast.hourly.windDirection10m.getOrNull(i),
                precipMm = forecast.hourly.precipitation.getOrNull(i),
                precipProbability = forecast.hourly.precipitationProbability.getOrNull(i),
                waveHeightM = triple?.first,
                wavePeriodS = triple?.second,
                waterTempC = triple?.third
            )
        }

        return Forecast(
            spotId = spotId,
            providerId = OpenMeteoProvider.ID,
            fetchedAt = fetchedAt,
            timezone = forecast.timezone,
            hours = hours
        )
    }
}
