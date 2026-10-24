package com.kompaktwind.data.live

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class LiveStationRepository(
    private val api: PiouPiouApi,
    private val now: () -> Long = { System.currentTimeMillis() }
) {
    @Volatile
    private var allCache: Pair<Long, List<PiouPiouLiveDto>>? = null
    private val liveCache = ConcurrentHashMap<Int, Pair<Long, PiouPiouLiveDto>>()
    private val historyCache = ConcurrentHashMap<Int, Pair<Long, List<StationObservation>>>()

    suspend fun nearby(
        lat: Double,
        lon: Double,
        limit: Int = 20,
        forceRefresh: Boolean = false
    ): Result<List<NearbyStation>> = runCatching {
        val all = loadAll(forceRefresh)
        all.mapNotNull { dto ->
            val location = dto.location ?: return@mapNotNull null
            if (!location.success) return@mapNotNull null
            val stationLat = location.latitude ?: return@mapNotNull null
            val stationLon = location.longitude ?: return@mapNotNull null
            val m = dto.measurements
            NearbyStation(
                id = "pioupiou:${dto.id}",
                source = LiveSource.OPENWINDMAP,
                name = dto.meta?.name?.takeIf { it.isNotBlank() } ?: "Pioupiou ${dto.id}",
                lat = stationLat,
                lon = stationLon,
                distanceKm = haversineKm(lat, lon, stationLat, stationLon),
                windAvgMs = m?.windSpeedAvg?.div(3.6),
                windMaxMs = m?.windSpeedMax?.div(3.6),
                windHeadingDeg = m?.windHeading?.toInt(),
                observedAtMs = parseInstant(m?.date)
            )
        }.sortedBy { it.distanceKm }.take(limit)
    }

    suspend fun live(id: Int, forceRefresh: Boolean = false): Result<StationLive> = runCatching {
        loadLive(id, forceRefresh).toStationLive()
    }

    suspend fun history(
        id: Int,
        hours: Int = 24,
        forceRefresh: Boolean = false
    ): Result<List<StationObservation>> = runCatching {
        val cached = historyCache[id]
        if (!forceRefresh && cached != null && now() - cached.first < HISTORY_TTL_MS) {
            return@runCatching cached.second
        }

        val stop = Instant.ofEpochMilli(now())
        val start = stop.minus(hours.toLong(), ChronoUnit.HOURS)
        val response = api.archive(id, start.toString(), stop.toString(), "json")
        val index = response.legend.withIndex().associate { it.value to it.index }

        fun value(row: List<JsonElement?>, key: String): JsonElement? =
            index[key]?.let { row.getOrNull(it) }

        fun number(row: List<JsonElement?>, key: String): Double? =
            (value(row, key) as? JsonPrimitive)?.doubleOrNull

        fun text(row: List<JsonElement?>, key: String): String? =
            (value(row, key) as? JsonPrimitive)?.contentOrNull

        val rows = response.data.mapNotNull { row ->
            val time = parseInstant(text(row, "time")) ?: return@mapNotNull null
            val avg = number(row, "wind_speed_avg") ?: return@mapNotNull null
            val max = number(row, "wind_speed_max") ?: return@mapNotNull null
            StationObservation(
                timeMs = time,
                windAvgMs = avg / 3.6,
                windMaxMs = max / 3.6,
                windMinMs = number(row, "wind_speed_min")?.div(3.6),
                headingDeg = number(row, "wind_heading")?.toInt()
            )
        }.sortedBy { it.timeMs }

        historyCache[id] = now() to rows
        rows
    }

    private suspend fun loadAll(forceRefresh: Boolean): List<PiouPiouLiveDto> {
        val cached = allCache
        if (!forceRefresh && cached != null && now() - cached.first < LIVE_TTL_MS) {
            return cached.second
        }
        val data = api.liveAll().data
        allCache = now() to data
        return data
    }

    private suspend fun loadLive(id: Int, forceRefresh: Boolean): PiouPiouLiveDto {
        val cached = liveCache[id]
        if (!forceRefresh && cached != null && now() - cached.first < LIVE_TTL_MS) {
            return cached.second
        }
        val data = api.live(id).data ?: error("Station $id returned no data")
        liveCache[id] = now() to data
        return data
    }

    private fun PiouPiouLiveDto.toStationLive(): StationLive {
        val m = measurements
        return StationLive(
            id = "pioupiou:$id",
            source = LiveSource.OPENWINDMAP,
            name = meta?.name?.takeIf { it.isNotBlank() } ?: "Pioupiou $id",
            lat = location?.latitude ?: 0.0,
            lon = location?.longitude ?: 0.0,
            windAvgMs = m?.windSpeedAvg?.div(3.6),
            windMaxMs = m?.windSpeedMax?.div(3.6),
            windMinMs = m?.windSpeedMin?.div(3.6),
            headingDeg = m?.windHeading?.toInt(),
            observedAtMs = parseInstant(m?.date)
        )
    }

    private fun parseInstant(value: String?): Long? =
        value?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }

    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val radius = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
        return radius * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    companion object {
        private const val LIVE_TTL_MS = 5 * 60_000L
        private const val HISTORY_TTL_MS = 10 * 60_000L

        fun create(): LiveStationRepository {
            val json = Json { ignoreUnknownKeys = true; isLenient = true }
            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build()
            val api = Retrofit.Builder()
                .baseUrl("https://api.pioupiou.fr/")
                .client(client)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(PiouPiouApi::class.java)
            return LiveStationRepository(api)
        }
    }
}
