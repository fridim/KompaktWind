package com.kompaktwind.data.provider

import com.kompaktwind.data.Forecast

interface WeatherProvider {
    val id: String
    val displayName: String
    val attributionText: String
    val attributionUrl: String?
    val disallowCaching: Boolean

    suspend fun fetchForecast(
        lat: Double,
        lon: Double,
        wantMarine: Boolean,
        days: Int = 7
    ): Result<Forecast>
}
