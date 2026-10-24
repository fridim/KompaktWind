package com.kompaktwind.data.live

enum class LiveSource { OPENWINDMAP, METAR }

data class NearbyStation(
    val id: String,
    val source: LiveSource,
    val name: String,
    val lat: Double,
    val lon: Double,
    val distanceKm: Double,
    val windAvgMs: Double?,
    val windMaxMs: Double?,
    val windHeadingDeg: Int?,
    val observedAtMs: Long?
)

data class StationObservation(
    val timeMs: Long,
    val windAvgMs: Double,
    val windMaxMs: Double,
    val windMinMs: Double?,
    val headingDeg: Int?
)

data class StationLive(
    val id: String,
    val source: LiveSource,
    val name: String,
    val lat: Double,
    val lon: Double,
    val windAvgMs: Double?,
    val windMaxMs: Double?,
    val windMinMs: Double?,
    val headingDeg: Int?,
    val observedAtMs: Long?
)

sealed interface NearbyStationsState {
    data object Loading : NearbyStationsState
    data class Data(
        val stations: List<NearbyStation>,
        val fetchedAt: Long = System.currentTimeMillis()
    ) : NearbyStationsState
    data class Error(val message: String) : NearbyStationsState
}

sealed interface StationDetailState {
    data object Loading : StationDetailState
    data class Data(
        val station: StationLive,
        val history: List<StationObservation>,
        val fetchedAt: Long = System.currentTimeMillis()
    ) : StationDetailState
    data class Error(val message: String) : StationDetailState
}
