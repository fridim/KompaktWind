package com.kompaktwind.data.provider.openmeteo

import com.kompaktwind.data.Forecast
import com.kompaktwind.data.HourPoint
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

object OpenMeteoMapper {
    fun toForecast(
        spotId: String,
        forecast: OpenMeteoForecastDto,
        marine: OpenMeteoMarineDto?,
        fetchedAt: Long
    ): Forecast {
        val zone = runCatching { ZoneId.of(forecast.timezone) }
            .getOrElse { ZoneOffset.ofTotalSeconds(forecast.utcOffsetSeconds) }
        val marineByTime: Map<String, MarineValues> = marine?.let { m ->
            m.hourly.time.mapIndexed { i, t ->
                t to MarineValues(
                    waveHeightM = m.hourly.waveHeight.getOrNull(i),
                    wavePeriodS = m.hourly.wavePeriod.getOrNull(i),
                    waterTempC = m.hourly.seaSurfaceTemperature.getOrNull(i),
                    seaLevelM = m.hourly.seaLevelHeight.getOrNull(i)
                )
            }.toMap()
        } ?: emptyMap()

        val hours = forecast.hourly.time.mapIndexed { i, isoLocal ->
            val instant = LocalDateTime.parse(isoLocal).atZone(zone).toInstant()
            val marineValues = marineByTime[isoLocal]
            HourPoint(
                timeEpochMs = instant.toEpochMilli(),
                tempC = forecast.hourly.temperature2m.getOrNull(i),
                windSpeedMs = forecast.hourly.windSpeed10m.getOrNull(i),
                windGustMs = forecast.hourly.windGusts10m.getOrNull(i),
                windDirDeg = forecast.hourly.windDirection10m.getOrNull(i),
                precipMm = forecast.hourly.precipitation.getOrNull(i),
                precipProbability = forecast.hourly.precipitationProbability.getOrNull(i),
                waveHeightM = marineValues?.waveHeightM,
                wavePeriodS = marineValues?.wavePeriodS,
                waterTempC = marineValues?.waterTempC,
                seaLevelM = marineValues?.seaLevelM
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

private data class MarineValues(
    val waveHeightM: Double?,
    val wavePeriodS: Double?,
    val waterTempC: Double?,
    val seaLevelM: Double?
)
