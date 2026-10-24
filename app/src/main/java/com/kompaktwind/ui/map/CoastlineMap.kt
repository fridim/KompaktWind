package com.kompaktwind.ui.map

import android.content.Context
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kompaktwind.data.LatLon
import com.mudita.mmd.components.text.TextMMD
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.util.Locale
import kotlin.math.cos
import kotlin.math.log2
import kotlin.math.pow

data class MapCamera(val lat: Double = 46.6, val lon: Double = 2.4, val zoom: Double = 6.5)

data class MapMarker(val id: String, val lat: Double, val lon: Double)

internal enum class CoastlineDetail { LOW, MEDIUM, HIGH, ULTRA }

private const val EUROPE_MIN_LAT = 30.0
private const val EUROPE_MAX_LAT = 72.0
private const val EUROPE_MIN_LON = -25.0
private const val EUROPE_MAX_LON = 45.0

internal fun detailFor(zoom: Double, lat: Double, lon: Double): CoastlineDetail {
    val europe = lat in EUROPE_MIN_LAT..EUROPE_MAX_LAT && lon in EUROPE_MIN_LON..EUROPE_MAX_LON
    return when {
        zoom >= 7.0 && europe -> CoastlineDetail.ULTRA
        zoom >= 6.5 && europe -> CoastlineDetail.HIGH
        zoom >= 4.0 -> CoastlineDetail.MEDIUM
        else -> CoastlineDetail.LOW
    }
}

internal data class Bounds(
    val minLat: Double,
    val maxLat: Double,
    val minLon: Double,
    val maxLon: Double
) {
    fun intersects(other: Bounds): Boolean =
        maxLat >= other.minLat && minLat <= other.maxLat &&
            maxLon >= other.minLon && minLon <= other.maxLon
}

internal data class LandPolygon(val rings: List<List<LatLon>>, val bounds: Bounds)

internal data class MapData(
    val land: List<LandPolygon> = emptyList()
)

@Composable
fun CoastlineMap(
    camera: MapCamera,
    onCameraChange: (MapCamera) -> Unit,
    markers: List<MapMarker> = emptyList(),
    selectedId: String? = null,
    onMarkerClick: (MapMarker) -> Unit = {},
    onMapTap: () -> Unit = {},
    showCrosshair: Boolean = false
) {
    val context = LocalContext.current.applicationContext
    val detail = detailFor(camera.zoom, camera.lat, camera.lon)
    val cache = remember { mutableMapOf<CoastlineDetail, MapData>() }
    val data by produceState(initialValue = cache[detail] ?: MapData(), detail) {
        cache[detail]?.let {
            value = it
            return@produceState
        }
        value = withContext(Dispatchers.IO) { loadMapData(context, detail) }
        cache[detail] = value
    }
    val lineColor = MaterialTheme.colorScheme.onBackground
    val background = MaterialTheme.colorScheme.background
    val lightTheme = background.luminance() > 0.5f
    val seaColor = if (lightTheme) Color(0xFFB8B8B8) else Color(0xFF000000)
    val landColor = if (lightTheme) background else Color(0xFF1E1E1E)
    val currentCamera by rememberUpdatedState(camera)

    Box(modifier = Modifier.fillMaxSize().background(seaColor)) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(markers, camera) {
                    detectTapGestures { tap ->
                        val projector = Projector(camera, size.width.toFloat(), size.height.toFloat())
                        val nearest = markers.minByOrNull { marker ->
                            (projector.project(marker.lat, marker.lon) - tap).getDistance()
                        }
                        if (nearest != null) {
                            val distance = (projector.project(nearest.lat, nearest.lon) - tap).getDistance()
                            if (distance <= 36.dp.toPx()) onMarkerClick(nearest) else onMapTap()
                        } else {
                            onMapTap()
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val cameraNow = currentCamera
                        val newZoom = (cameraNow.zoom + log2(zoom.toDouble())).coerceIn(1.0, 12.0)
                        val ppd = pixelsPerDegree(newZoom)
                        val cosLat = cos(Math.toRadians(cameraNow.lat)).coerceAtLeast(0.2)
                        onCameraChange(
                            MapCamera(
                                lat = (cameraNow.lat + pan.y / ppd).coerceIn(-85.0, 85.0),
                                lon = normaliseLon(cameraNow.lon - pan.x / (ppd * cosLat)),
                                zoom = newZoom
                            )
                        )
                    }
                }
        ) {
            val width = size.width
            val height = size.height
            val projector = Projector(camera, width, height)
            val visible = projector.bounds(width, height)
            val strokeWidth = 1.dp.toPx()
            for (polygon in data.land) {
                if (!polygon.bounds.intersects(visible)) continue
                val screenWidth = (polygon.bounds.maxLon - polygon.bounds.minLon) * projector.xScale
                val screenHeight = (polygon.bounds.maxLat - polygon.bounds.minLat) * projector.yScale
                if (screenWidth * screenHeight < MIN_VISIBLE_AREA_PX) continue
                val path = Path().apply { fillType = PathFillType.EvenOdd }
                for (ring in polygon.rings) {
                    var started = false
                    for (point in ring) {
                        val x = projector.x(point.lon)
                        val y = projector.y(point.lat)
                        if (!started) {
                            path.moveTo(x, y)
                            started = true
                        } else {
                            path.lineTo(x, y)
                        }
                    }
                    if (started) path.close()
                }
                drawPath(path, landColor)
                drawPath(path, lineColor, style = Stroke(width = strokeWidth))
            }

            for (marker in markers) {
                val point = projector.project(marker.lat, marker.lon)
                if (point.x in -20f..width + 20f && point.y in -20f..height + 20f) {
                    if (marker.id == selectedId) {
                        drawCircle(background, radius = 9.dp.toPx(), center = point)
                        drawCircle(lineColor, radius = 7.dp.toPx(), center = point)
                    } else {
                        drawCircle(lineColor, radius = 3.5.dp.toPx(), center = point)
                    }
                }
            }
        }
        if (showCrosshair) {
            TextMMD(
                text = "+",
                fontSize = 30.sp,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

private const val MIN_VISIBLE_AREA_PX = 2.0

private class Projector(
    private val camera: MapCamera,
    width: Float,
    height: Float
) {
    private val ppd = pixelsPerDegree(camera.zoom)
    private val cosLat = cos(Math.toRadians(camera.lat)).coerceAtLeast(0.2)
    val xScale = ppd * cosLat
    val yScale = ppd
    private val originX = width / 2.0
    private val originY = height / 2.0

    fun x(lon: Double): Float = (originX + (lon - camera.lon) * xScale).toFloat()

    fun y(lat: Double): Float = (originY - (lat - camera.lat) * yScale).toFloat()

    fun project(lat: Double, lon: Double): Offset = Offset(x(lon), y(lat))

    fun bounds(width: Float, height: Float): Bounds {
        val latHalf = (height / 2.0) / yScale
        val lonHalf = (width / 2.0) / xScale
        return Bounds(
            minLat = camera.lat - latHalf,
            maxLat = camera.lat + latHalf,
            minLon = camera.lon - lonHalf,
            maxLon = camera.lon + lonHalf
        )
    }
}

private fun pixelsPerDegree(zoom: Double): Double = 256.0 * 2.0.pow(zoom) / 360.0

private fun normaliseLon(lon: Double): Double {
    var value = lon
    while (value > 180.0) value -= 360.0
    while (value < -180.0) value += 360.0
    return value
}

internal fun loadMapData(context: Context, detail: CoastlineDetail): MapData = when (detail) {
    CoastlineDetail.LOW -> MapData(land = loadLand(context, "land_110m.bin"))
    CoastlineDetail.MEDIUM -> MapData(land = loadLand(context, "land_50m.bin"))
    CoastlineDetail.HIGH -> MapData(land = loadLand(context, "land_10m_europe.bin"))
    CoastlineDetail.ULTRA -> MapData(land = loadLand(context, "land_gshhg_europe.bin"))
}

private fun loadLand(context: Context, asset: String): List<LandPolygon> = runCatching {
    DataInputStream(context.assets.open(asset).buffered()).use { input ->
        val magic = ByteArray(MAGIC.length)
        input.readFully(magic)
        require(String(magic, Charsets.US_ASCII) == MAGIC) { "Bad map asset $asset" }
        val polygonCount = input.readInt()
        require(polygonCount in 0..MAX_POLYGONS) { "Bad polygon count in $asset" }
        List(polygonCount) {
            val ringCount = input.readInt()
            require(ringCount in 0..MAX_RINGS) { "Bad ring count in $asset" }
            val rings = List(ringCount) {
                val pointCount = input.readInt()
                require(pointCount in 0..MAX_POINTS) { "Bad point count in $asset" }
                List(pointCount) {
                    val lat = input.readFloat().toDouble()
                    val lon = input.readFloat().toDouble()
                    LatLon(lat, lon)
                }
            }.filter { it.isNotEmpty() }
            LandPolygon(rings, boundsOf(rings.flatten()))
        }.filter { it.rings.isNotEmpty() }
    }
}.onFailure { Log.w("CoastlineMap", "Failed to load map asset $asset", it) }
    .getOrDefault(emptyList())

private const val MAGIC = "KWLAND1\n"
private const val MAX_POLYGONS = 200_000
private const val MAX_RINGS = 10_000
private const val MAX_POINTS = 1_000_000

private fun boundsOf(points: List<LatLon>): Bounds {
    var minLat = Double.MAX_VALUE
    var maxLat = -Double.MAX_VALUE
    var minLon = Double.MAX_VALUE
    var maxLon = -Double.MAX_VALUE
    for (point in points) {
        if (point.lat < minLat) minLat = point.lat
        if (point.lat > maxLat) maxLat = point.lat
        if (point.lon < minLon) minLon = point.lon
        if (point.lon > maxLon) maxLon = point.lon
    }
    return Bounds(minLat, maxLat, minLon, maxLon)
}
