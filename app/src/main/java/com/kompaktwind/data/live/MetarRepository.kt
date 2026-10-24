package com.kompaktwind.data.live

import com.kompaktwind.BuildConfig

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.time.Instant
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class MetarRepository(
    private val awc: MetarApi,
    private val iem: IemApi,
    private val now: () -> Long = { System.currentTimeMillis() }
) {
    private val liveCache = ConcurrentHashMap<String, Pair<Long, StationLive>>()
    private val historyCache = ConcurrentHashMap<String, Pair<Long, List<StationObservation>>>()
    private val nearbyCache = ConcurrentHashMap<NearbyKey, Pair<Long, List<NearbyStation>>>()

    suspend fun nearby(
        lat: Double,
        lon: Double,
        limit: Int = 20,
        forceRefresh: Boolean = false
    ): Result<List<NearbyStation>> = runCatching {
        val key = NearbyKey(lat, lon, limit)
        if (!forceRefresh) {
            nearbyCache[key]?.takeIf { now() - it.first < NEARBY_TTL_MS }?.let {
                return@runCatching it.second
            }
        }
        val stations = loadNearby(lat, lon, limit)
        nearbyCache[key] = now() to stations
        stations
    }

    private suspend fun loadNearby(lat: Double, lon: Double, limit: Int): List<NearbyStation> {
        val awcStations = runCatching {
            awc.metar(bbox = bbox(lat, lon), hours = 1, format = "json")
                .mapNotNull { it.toNearby(lat, lon) }
                .sortedBy { it.distanceKm }
                .take(limit)
        }.getOrDefault(emptyList())

        if (awcStations.isNotEmpty()) return awcStations

        if (!inFrance(lat, lon)) return emptyList()

        return iem.currents()
            .features
            .mapNotNull { it.toNearby(lat, lon) }
            .sortedBy { it.distanceKm }
            .take(limit)
    }

    suspend fun live(id: String, forceRefresh: Boolean = false): Result<StationLive> = runCatching {
        val cached = liveCache[id]
        if (!forceRefresh && cached != null && now() - cached.first < LIVE_TTL_MS) {
            return@runCatching cached.second
        }
        val dto = awc.metar(ids = id, hours = 1, format = "json")
            .filter { it.icaoId == id }
            .maxByOrNull { it.observedAtMs() ?: 0L }
            ?: error("No METAR for $id")
        val station = dto.toStationLive()
        liveCache[id] = now() to station
        station
    }

    suspend fun history(
        id: String,
        hours: Int = 24,
        forceRefresh: Boolean = false
    ): Result<List<StationObservation>> = runCatching {
        val cached = historyCache[id]
        if (!forceRefresh && cached != null && now() - cached.first < HISTORY_TTL_MS) {
            return@runCatching cached.second
        }
        val rows = awc.metar(ids = id, hours = hours, format = "json")
            .mapNotNull { it.toObservation() }
            .sortedBy { it.timeMs }
        historyCache[id] = now() to rows
        rows
    }

    private fun AwcMetarDto.toNearby(originLat: Double, originLon: Double): NearbyStation? {
        val stationLat = lat ?: return null
        val stationLon = lon ?: return null
        val icao = icaoId ?: return null
        return NearbyStation(
            id = "metar:$icao",
            source = LiveSource.METAR,
            name = name ?: icao,
            lat = stationLat,
            lon = stationLon,
            distanceKm = haversineKm(originLat, originLon, stationLat, stationLon),
            windAvgMs = wspd?.div(1.94384),
            windMaxMs = wgst?.div(1.94384),
            windHeadingDeg = wdir?.toInt(),
            observedAtMs = observedAtMs()
        )
    }

    private fun AwcMetarDto.toStationLive(): StationLive = StationLive(
        id = "metar:${icaoId.orEmpty()}",
        source = LiveSource.METAR,
        name = name ?: icaoId ?: "METAR",
        lat = lat ?: 0.0,
        lon = lon ?: 0.0,
        windAvgMs = wspd?.div(1.94384),
        windMaxMs = wgst?.div(1.94384),
        windMinMs = null,
        headingDeg = wdir?.toInt(),
        observedAtMs = observedAtMs()
    )

    private fun AwcMetarDto.toObservation(): StationObservation? {
        val time = observedAtMs() ?: return null
        val avgKt = wspd ?: return null
        val maxKt = wgst ?: avgKt
        return StationObservation(
            timeMs = time,
            windAvgMs = avgKt / 1.94384,
            windMaxMs = maxKt / 1.94384,
            windMinMs = null,
            headingDeg = wdir?.toInt()
        )
    }

    private fun AwcMetarDto.observedAtMs(): Long? =
        obsTime?.let { it * 1000L }
            ?: reportTime?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }

    private fun IemFeature.toNearby(originLat: Double, originLon: Double): NearbyStation? {
        val p = properties ?: return null
        val coords = geometry?.coordinates ?: return null
        if (coords.size < 2) return null
        val stationLon = coords[0]
        val stationLat = coords[1]
        val icao = p.station ?: return null
        return NearbyStation(
            id = "metar:$icao",
            source = LiveSource.METAR,
            name = p.name ?: icao,
            lat = stationLat,
            lon = stationLon,
            distanceKm = haversineKm(originLat, originLon, stationLat, stationLon),
            windAvgMs = p.sknt?.div(1.94384),
            windMaxMs = p.gust?.div(1.94384),
            windHeadingDeg = p.drct?.toInt(),
            observedAtMs = p.utcValid?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }
        )
    }

    private fun bbox(lat: Double, lon: Double): String =
        String.format(Locale.US, "%.2f,%.2f,%.2f,%.2f", lat - 2.0, lon - 2.0, lat + 2.0, lon + 2.0)

    private fun inFrance(lat: Double, lon: Double): Boolean =
        lat in 41.0..51.5 && lon in -5.5..9.7

    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val radius = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
        return radius * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    private data class NearbyKey(val lat: Double, val lon: Double, val limit: Int)

    companion object {
        private const val LIVE_TTL_MS = 10 * 60_000L
        private const val HISTORY_TTL_MS = 10 * 60_000L
        private const val NEARBY_TTL_MS = 10 * 60_000L

        fun create(): MetarRepository {
            val json = Json { ignoreUnknownKeys = true; isLenient = true }
            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .addInterceptor { chain ->
                    chain.proceed(
                        chain.request().newBuilder()
                            .header("User-Agent", "KompaktWind/${BuildConfig.VERSION_NAME}")
                            .build()
                    )
                }
                .build()
            fun retrofit(baseUrl: String) = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()

            return MetarRepository(
                awc = retrofit("https://aviationweather.gov/").create(MetarApi::class.java),
                iem = retrofit("https://mesonet.agron.iastate.edu/").create(IemApi::class.java)
            )
        }
    }
}
