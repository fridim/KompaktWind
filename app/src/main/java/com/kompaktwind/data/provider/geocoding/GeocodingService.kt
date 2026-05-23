package com.kompaktwind.data.provider.geocoding

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface GeocodingApi {
    @GET("v1/search")
    suspend fun search(
        @Query("name") query: String,
        @Query("count") count: Int = 10,
        @Query("language") language: String = "en",
        @Query("format") format: String = "json"
    ): GeocodingResponseDto
}

class GeocodingService(private val api: GeocodingApi) {
    suspend fun search(query: String): Result<List<GeocodingResultDto>> = runCatching {
        if (query.isBlank()) emptyList()
        else api.search(query.trim()).results.orEmpty()
    }

    companion object {
        fun create(baseUrl: String = "https://geocoding-api.open-meteo.com/"): GeocodingService {
            val json = Json { ignoreUnknownKeys = true }
            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()
            val api = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(GeocodingApi::class.java)
            return GeocodingService(api)
        }
    }
}
