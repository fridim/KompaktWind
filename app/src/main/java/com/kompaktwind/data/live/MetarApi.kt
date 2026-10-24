package com.kompaktwind.data.live

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

interface MetarApi {
    @GET("api/data/metar")
    suspend fun metar(
        @Query("bbox") bbox: String? = null,
        @Query("ids") ids: String? = null,
        @Query("hours") hours: Int? = null,
        @Query("format") format: String = "json"
    ): List<AwcMetarDto>
}

interface IemApi {
    @GET("api/1/currents.geojson")
    suspend fun currents(
        @Query("network") network: String = "FR__ASOS"
    ): IemResponse
}

@Serializable
data class AwcMetarDto(
    val icaoId: String? = null,
    val name: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    val wdir: Double? = null,
    val wspd: Double? = null,
    val wgst: Double? = null,
    val obsTime: Long? = null,
    val reportTime: String? = null,
    val rawOb: String? = null
)

@Serializable
data class IemResponse(
    val features: List<IemFeature> = emptyList()
)

@Serializable
data class IemFeature(
    val properties: IemProperties? = null,
    val geometry: IemGeometry? = null
)

@Serializable
data class IemProperties(
    val station: String? = null,
    val name: String? = null,
    @SerialName("utc_valid") val utcValid: String? = null,
    val sknt: Double? = null,
    val gust: Double? = null,
    val drct: Double? = null
)

@Serializable
data class IemGeometry(
    val coordinates: List<Double> = emptyList()
)
