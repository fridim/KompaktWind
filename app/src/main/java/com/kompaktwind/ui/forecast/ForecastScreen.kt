package com.kompaktwind.ui.forecast

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kompaktwind.R
import com.kompaktwind.data.DistanceUnit
import com.kompaktwind.data.ForecastUiState
import com.kompaktwind.data.live.NearbyStation
import com.kompaktwind.data.live.NearbyStationsState
import com.kompaktwind.data.MarineDisplay
import com.kompaktwind.data.TempUnit
import com.kompaktwind.data.WindColorMode
import com.kompaktwind.data.WindUnit
import com.kompaktwind.ui.KompaktWindViewModel
import com.kompaktwind.ui.common.UnitFormatters
import com.kompaktwind.ui.common.WindPair
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.lazy.LazyColumnMMD
import com.mudita.mmd.components.progress_indicator.CircularProgressIndicatorMMD
import com.mudita.mmd.components.text.TextMMD
import java.time.Instant
import java.time.ZoneId

@Composable
fun ForecastScreen(
    viewModel: KompaktWindViewModel,
    spotId: String,
    onOpenLive: (String) -> Unit = {},
    onOpenHelp: () -> Unit = {}
) {
    val spots by viewModel.spots.collectAsState()
    val forecast by viewModel.forecast.collectAsState()
    val settings by viewModel.settingsState.collectAsState()
    val nearby by viewModel.nearbyStations.collectAsState()
    val spot = spots.firstOrNull { it.id == spotId }

    LaunchedEffect(spotId, spot) {
        if (spot != null) {
            viewModel.loadForecast(spot)
            viewModel.loadNearbyStations(spot)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        when (val s = forecast) {
            is ForecastUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicatorMMD()
                }
            }
            is ForecastUiState.Data -> ForecastTable(
                state = s,
                windUnit = settings.windUnit,
                tempUnit = settings.tempUnit,
                showMarine = shouldShowMarine(settings.marineDisplay, spot?.isCoastal ?: false),
                nearby = nearby,
                distanceUnit = settings.distanceUnit,
                windColorMode = settings.windColorMode,
                onOpenLive = { spot?.let { onOpenLive(it.id) } },
                onOpenHelp = onOpenHelp,
                modifier = Modifier.weight(1f)
            )
            is ForecastUiState.Error -> {
                Column(modifier = Modifier.weight(1f).padding(16.dp)) {
                    TextMMD(text = stringResource(R.string.forecast_error, s.message))
                    ButtonMMD(onClick = { spot?.let { viewModel.loadForecast(it, forceRefresh = true) } }) {
                        TextMMD(stringResource(R.string.try_again))
                    }
                    if (s.cached != null) {
                        HorizontalDividerMMD()
                        ForecastTable(
                            state = s.cached.copy(isStale = true),
                            windUnit = settings.windUnit,
                            tempUnit = settings.tempUnit,
                            showMarine = shouldShowMarine(settings.marineDisplay, spot?.isCoastal ?: false),
                            nearby = nearby,
                            distanceUnit = settings.distanceUnit,
                            windColorMode = settings.windColorMode,
                            onOpenLive = { spot?.let { onOpenLive(it.id) } },
                            onOpenHelp = onOpenHelp
                        )
                    }
                }
            }
        }
    }
}

private fun shouldShowMarine(setting: MarineDisplay, isCoastal: Boolean): Boolean = when (setting) {
    MarineDisplay.AUTO -> isCoastal
    MarineDisplay.ALWAYS -> true
    MarineDisplay.NEVER -> false
}

@Composable
private fun ForecastTable(
    state: ForecastUiState.Data,
    windUnit: WindUnit,
    tempUnit: TempUnit,
    showMarine: Boolean,
    nearby: NearbyStationsState,
    distanceUnit: DistanceUnit,
    windColorMode: WindColorMode,
    onOpenLive: () -> Unit,
    onOpenHelp: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedStation by remember(state.forecast.spotId) { mutableIntStateOf(0) }
    val nearbyStations = (nearby as? NearbyStationsState.Data)?.stations.orEmpty()
    val nowMs = System.currentTimeMillis()
    val currentHourMs = nowMs - (nowMs % 3_600_000)
    val hours = state.forecast.hours.filter { it.timeEpochMs >= currentHourMs }
    val grouped = hours.groupBy { dayKey(it.timeEpochMs, state.forecast.timezone) }

    Column(modifier = modifier.fillMaxSize()) {
        if (state.isStale) {
            TextMMD(
                text = stringResource(R.string.offline_cached),
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
        val tz = state.forecast.timezone
        LazyColumnMMD(modifier = Modifier.weight(1f)) {
            item(key = "live-station") {
                NearestStationStrip(
                    state = nearby,
                    station = nearbyStations.getOrNull(selectedStation.coerceIn(0, (nearbyStations.size - 1).coerceAtLeast(0))),
                    windUnit = windUnit,
                    distanceUnit = distanceUnit,
                    colorMode = windColorMode,
                    onOpenLive = onOpenLive,
                    onPrev = {
                        if (nearbyStations.size > 1) {
                            selectedStation = (selectedStation - 1 + nearbyStations.size) % nearbyStations.size
                        }
                    },
                    onNext = {
                        if (nearbyStations.size > 1) {
                            selectedStation = (selectedStation + 1) % nearbyStations.size
                        }
                    }
                )
            }
            grouped.forEach { (_, dayHours) ->
                item { DayHeader(epochMs = dayHours.first().timeEpochMs, timezone = tz) }
                item { ColumnHeader(windUnit = windUnit, showMarineCols = showMarine, onOpenHelp = onOpenHelp) }
                item { HorizontalDividerMMD() }
                items(items = dayHours, key = { it.timeEpochMs }) { hour ->
                    HourRow(
                        hour = hour,
                        windUnit = windUnit,
                        tempUnit = tempUnit,
                        showMarineCols = showMarine,
                        timezone = tz,
                        colorMode = windColorMode
                    )
                }
            }
        }
    }
}

@Composable
private fun NearestStationStrip(
    state: NearbyStationsState,
    station: NearbyStation?,
    windUnit: WindUnit,
    distanceUnit: DistanceUnit,
    colorMode: WindColorMode,
    onOpenLive: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    val stationCount = (state as? NearbyStationsState.Data)?.stations?.size ?: 0
    if (station == null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenLive)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicatorMMD(modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            TextMMD(
                text = stringResource(R.string.loading_live_stations),
                fontSize = 18.sp,
                lineHeight = 18.sp
            )
        }
        HorizontalDividerMMD(thickness = 0.5.dp)
        return
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenLive)
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextMMD(
                text = station.name,
                fontSize = 20.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onPrev,
                enabled = stationCount > 1,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = stringResource(R.string.previous),
                    modifier = Modifier.size(18.dp).rotate(180f)
                )
            }
            IconButton(
                onClick = onNext,
                enabled = stationCount > 1,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = stringResource(R.string.next),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextMMD(
                text = "${stringResource(R.string.live)} · ${UnitFormatters.distanceLabel(station.distanceKm, distanceUnit)} · ",
                fontSize = 18.sp,
                lineHeight = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            WindPair(
                avgMs = station.windAvgMs,
                maxMs = station.windMaxMs,
                unit = windUnit,
                fontSize = 18.sp,
                colorMode = colorMode,
                showUnit = true
            )
            station.windHeadingDeg?.let {
                TextMMD(
                    text = " ${UnitFormatters.arrowGlyph(it)}",
                    fontSize = 18.sp,
                    lineHeight = 18.sp,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
    HorizontalDividerMMD(thickness = 0.5.dp)
}

private fun dayKey(epochMs: Long, tz: String): String =
    Instant.ofEpochMilli(epochMs).atZone(ZoneId.of(tz)).toLocalDate().toString()
