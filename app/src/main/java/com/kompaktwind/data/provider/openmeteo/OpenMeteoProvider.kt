package com.kompaktwind.data.provider.openmeteo

import com.kompaktwind.data.Forecast
import com.kompaktwind.data.provider.WeatherProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

class OpenMeteoProvider(
    private val forecastApi: OpenMeteoForecastApi,
    private val marineApi: OpenMeteoMarineApi,
    private val now: () -> Long = { System.currentTimeMillis() }
) : WeatherProvider {

    override val id: String = ID
    override val displayName: String = "Open-Meteo"
    override val attributionText: String = "Data provided by Open-Meteo"
    override val attributionUrl: String? = "https://open-meteo.com"
    override val disallowCaching: Boolean = false

    override suspend fun fetchForecast(
        lat: Double,
        lon: Double,
        wantMarine: Boolean,
        days: Int
    ): Result<Forecast> = runCatching {
        coroutineScope {
            val forecastDeferred = async {
                runCatching { forecastApi.forecast(lat, lon, forecastDays = days) }
            }
            val marineDeferred = async {
                if (wantMarine) runCatching { marineApi.marine(lat, lon, forecastDays = days) }
                else Result.success(null as OpenMeteoMarineDto?)
            }
            val forecastDto = forecastDeferred.await().getOrThrow()
            val marineDto = marineDeferred.await().getOrNull()
            OpenMeteoMapper.toForecast(
                spotId = "",
                forecast = forecastDto,
                marine = marineDto,
                fetchedAt = now()
            )
        }
    }

    companion object {
        const val ID = "open-meteo"

        fun create(
            forecastBaseUrl: String = "https://api.open-meteo.com/",
            marineBaseUrl: String = "https://marine-api.open-meteo.com/"
        ): OpenMeteoProvider {
            val json = Json { ignoreUnknownKeys = true; isLenient = true }
            val converter = json.asConverterFactory("application/json".toMediaType())

            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                })
                .build()

            fun api(url: String) = Retrofit.Builder()
                .baseUrl(url)
                .client(client)
                .addConverterFactory(converter)
                .build()

            return OpenMeteoProvider(
                forecastApi = api(forecastBaseUrl).create(OpenMeteoForecastApi::class.java),
                marineApi = api(marineBaseUrl).create(OpenMeteoMarineApi::class.java)
            )
        }
    }
}
