package com.kompaktwind.data.live

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface PiouPiouApi {
    @GET("v1/live/all")
    suspend fun liveAll(): PiouPiouLiveAllResponse

    @GET("v1/live/{id}")
    suspend fun live(@Path("id") id: Int): PiouPiouLiveSingleResponse

    @GET("v1/archive/{id}")
    suspend fun archive(
        @Path("id") id: Int,
        @Query("start") start: String,
        @Query("stop") stop: String,
        @Query("format") format: String = "json"
    ): PiouPiouArchiveResponse
}

@Serializable
data class PiouPiouLiveAllResponse(
    val data: List<PiouPiouLiveDto> = emptyList()
)

@Serializable
data class PiouPiouLiveSingleResponse(
    val data: PiouPiouLiveDto? = null
)

@Serializable
data class PiouPiouLiveDto(
    val id: Int = 0,
    val meta: PiouPiouMeta? = null,
    val location: PiouPiouLocation? = null,
    val measurements: PiouPiouMeasurements? = null
)

@Serializable
data class PiouPiouMeta(
    val name: String? = null
)

@Serializable
data class PiouPiouLocation(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val date: String? = null,
    val success: Boolean = true
)

@Serializable
data class PiouPiouMeasurements(
    val date: String? = null,
    @SerialName("wind_heading") val windHeading: Double? = null,
    @SerialName("wind_speed_avg") val windSpeedAvg: Double? = null,
    @SerialName("wind_speed_max") val windSpeedMax: Double? = null,
    @SerialName("wind_speed_min") val windSpeedMin: Double? = null
)

@Serializable
data class PiouPiouArchiveResponse(
    val legend: List<String> = emptyList(),
    val units: List<String> = emptyList(),
    val data: List<List<JsonElement?>> = emptyList()
)
