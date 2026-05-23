package com.kompaktwind.data.provider

import com.kompaktwind.data.Forecast
import com.kompaktwind.data.HourPoint

class FakeWeatherProvider(
    override val id: String = "fake",
    override val displayName: String = "Fake",
    override val attributionText: String = "Fake provider",
    override val attributionUrl: String? = null,
    override val disallowCaching: Boolean = false,
    private var nextResult: Result<Forecast> = Result.success(
        Forecast("spot", "fake", 0L, "UTC", listOf(HourPoint(0L)))
    )
) : WeatherProvider {
    var calls: Int = 0
        private set
    var lastArgs: List<Any?> = emptyList()
        private set

    fun setNext(result: Result<Forecast>) { nextResult = result }

    override suspend fun fetchForecast(
        lat: Double, lon: Double, wantMarine: Boolean, days: Int
    ): Result<Forecast> {
        calls++
        lastArgs = listOf(lat, lon, wantMarine, days)
        return nextResult
    }
}
