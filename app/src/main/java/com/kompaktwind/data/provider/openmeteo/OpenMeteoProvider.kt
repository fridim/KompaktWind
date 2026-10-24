package com.kompaktwind.data.provider.openmeteo

import android.util.Log

import com.kompaktwind.data.Forecast
import com.kompaktwind.data.ProviderIds
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
    override val id: String,
    override val displayName: String,
    override val attributionText: String,
    private val models: String? = null,
    private val maxDays: Int = 7,
    private val forecastApi: OpenMeteoForecastApi,
    private val marineApi: OpenMeteoMarineApi,
    private val now: () -> Long = { System.currentTimeMillis() }
) : WeatherProvider {

    override val attributionUrl: String? = "https://open-meteo.com"
    override val disallowCaching: Boolean = false

    override suspend fun fetchForecast(
        lat: Double,
        lon: Double,
        wantMarine: Boolean,
        days: Int
    ): Result<Forecast> = runCatching {
        coroutineScope {
            val forecastDays = days.coerceIn(1, maxDays)
            val forecastDeferred = async {
                runCatching {
                    forecastApi.forecast(
                        lat = lat,
                        lon = lon,
                        models = models,
                        forecastDays = forecastDays
                    )
                }
            }
            val marineDeferred = async {
                if (wantMarine) {
                    runCatching { marineApi.marine(lat, lon, forecastDays = forecastDays) }
                } else {
                    Result.success(null as OpenMeteoMarineDto?)
                }
            }
            val forecastDto = forecastDeferred.await().getOrThrow()
            val marineDto = marineDeferred.await()
                .onFailure {
                    Log.w("OpenMeteoProvider", "Marine forecast unavailable for $lat,$lon", it)
                }
                .getOrNull()
            OpenMeteoMapper.toForecast(
                spotId = "",
                forecast = forecastDto,
                marine = marineDto,
                fetchedAt = now()
            )
        }
    }

    companion object {
        const val ID = ProviderIds.OPEN_METEO
        const val AROME_FR_ID = ProviderIds.AROME_FR
        const val AROME_FR_MODEL = "meteofrance_arome_france_hd"

        fun create(
            id: String = ID,
            displayName: String = "Open-Meteo",
            attributionText: String = "Data provided by Open-Meteo",
            models: String? = null,
            maxDays: Int = 7,
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
                id = id,
                displayName = displayName,
                attributionText = attributionText,
                models = models,
                maxDays = maxDays,
                forecastApi = api(forecastBaseUrl).create(OpenMeteoForecastApi::class.java),
                marineApi = api(marineBaseUrl).create(OpenMeteoMarineApi::class.java)
            )
        }

        fun createArome(
            forecastBaseUrl: String = "https://api.open-meteo.com/",
            marineBaseUrl: String = "https://marine-api.open-meteo.com/"
        ): OpenMeteoProvider = create(
            id = AROME_FR_ID,
            displayName = "AROME FR",
            attributionText = "Météo-France AROME via Open-Meteo",
            models = AROME_FR_MODEL,
            maxDays = 3,
            forecastBaseUrl = forecastBaseUrl,
            marineBaseUrl = marineBaseUrl
        )
    }
}
