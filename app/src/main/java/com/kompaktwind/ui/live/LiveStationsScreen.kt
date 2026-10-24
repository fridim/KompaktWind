package com.kompaktwind.ui.live

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kompaktwind.R
import com.kompaktwind.data.live.LiveSource
import com.kompaktwind.data.live.NearbyStationsState
import com.kompaktwind.ui.KompaktWindViewModel
import com.kompaktwind.ui.common.UnitFormatters
import com.kompaktwind.ui.common.WindPair
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.lazy.LazyColumnMMD
import com.mudita.mmd.components.progress_indicator.CircularProgressIndicatorMMD
import com.mudita.mmd.components.text.TextMMD

@Composable
fun LiveStationsScreen(
    viewModel: KompaktWindViewModel,
    spotId: String,
    onOpenStation: (String) -> Unit
) {
    val spots by viewModel.spots.collectAsState()
    val state by viewModel.nearbyStations.collectAsState()
    val settings by viewModel.settingsState.collectAsState()
    val spot = spots.firstOrNull { it.id == spotId }

    LaunchedEffect(spot?.id) {
        spot?.let { viewModel.loadNearbyStations(it) }
    }

    if (spot == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            TextMMD(text = stringResource(R.string.spot_not_found), fontSize = 18.sp)
        }
        return
    }

    when (val s = state) {
        is NearbyStationsState.Loading -> LiveStationsLoading()
        is NearbyStationsState.Error -> {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                TextMMD(text = stringResource(R.string.live_error), fontSize = 18.sp)
                ButtonMMD(onClick = { viewModel.loadNearbyStations(spot) }) {
                    TextMMD(stringResource(R.string.try_again))
                }
            }
        }
        is NearbyStationsState.Data -> {
            if (s.stations.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    TextMMD(text = stringResource(R.string.no_stations_nearby), fontSize = 18.sp)
                }
            } else {
                LazyColumnMMD(modifier = Modifier.fillMaxSize()) {
                    item {
                        TextMMD(
                            text = stringResource(R.string.nearest_stations),
                            fontSize = 20.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        HorizontalDividerMMD(thickness = 0.5.dp)
                    }
                    items(items = s.stations, key = { it.id }) { station ->
                        val distanceText = UnitFormatters.distanceLabel(station.distanceKm, settings.distanceUnit)
                        val sourceLabel = when (station.source) {
                            LiveSource.OPENWINDMAP -> "OpenWindMap"
                            LiveSource.METAR -> "METAR"
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenStation(station.id) }
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            TextMMD(
                                text = station.name,
                                fontSize = 20.sp,
                                lineHeight = 20.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextMMD(
                                    text = "$sourceLabel · $distanceText · ",
                                    fontSize = 16.sp,
                                    lineHeight = 18.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                WindPair(
                                    avgMs = station.windAvgMs,
                                    maxMs = station.windMaxMs,
                                    unit = settings.windUnit,
                                    fontSize = 18.sp,
                                    colorMode = settings.windColorMode,
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
                    item {
                        TextMMD(
                            text = stringResource(R.string.live_attribution),
                            fontSize = 16.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                        TextMMD(
                            text = stringResource(R.string.metar_attribution),
                            fontSize = 16.sp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveStationsLoading() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicatorMMD()
            Spacer(Modifier.height(12.dp))
            TextMMD(
                text = stringResource(R.string.loading_live_stations),
                fontSize = 18.sp,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}
