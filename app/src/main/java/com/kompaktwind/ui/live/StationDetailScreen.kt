package com.kompaktwind.ui.live

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kompaktwind.R
import com.kompaktwind.data.WindUnit
import com.kompaktwind.data.live.LiveSource
import com.kompaktwind.data.live.StationDetailState
import com.kompaktwind.data.live.StationLive
import com.kompaktwind.data.live.StationObservation
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
import java.time.format.DateTimeFormatter

@Composable
fun StationDetailScreen(
    viewModel: KompaktWindViewModel,
    stationId: String
) {
    val state by viewModel.stationDetail.collectAsState()
    val settings by viewModel.settingsState.collectAsState()

    LaunchedEffect(stationId) {
        viewModel.loadStation(stationId)
    }

    when (val s = state) {
        is StationDetailState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicatorMMD()
            }
        }
        is StationDetailState.Error -> {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                TextMMD(text = stringResource(R.string.live_error), fontSize = 18.sp)
                ButtonMMD(onClick = { viewModel.loadStation(stationId) }) {
                    TextMMD(stringResource(R.string.try_again))
                }
            }
        }
        is StationDetailState.Data -> {
            StationContent(
                station = s.station,
                history = s.history,
                windUnit = settings.windUnit,
                windColorMode = settings.windColorMode
            )
        }
    }
}

@Composable
private fun StationContent(
    station: StationLive,
    history: List<StationObservation>,
    windUnit: WindUnit,
    windColorMode: com.kompaktwind.data.WindColorMode
) {
    val locale = LocalConfiguration.current.locales[0]
    val timeFormatter = remember(locale) {
        DateTimeFormatter.ofPattern("HH:mm", locale).withZone(ZoneId.systemDefault())
    }
    val dateFormatter = remember(locale) {
        DateTimeFormatter.ofPattern("EEE d MMM HH:mm", locale).withZone(ZoneId.systemDefault())
    }
    val direction = station.headingDeg?.let {
        "${UnitFormatters.arrowGlyph(it)} ${UnitFormatters.cardinal(it)} $it°"
    } ?: "—"
    val observed = station.observedAtMs?.let {
        dateFormatter.format(Instant.ofEpochMilli(it))
    } ?: "—"

    LazyColumnMMD(modifier = Modifier.fillMaxSize()) {
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                TextMMD(
                    text = station.name,
                    fontSize = 20.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                TextMMD(
                    text = when (station.source) {
                        LiveSource.OPENWINDMAP -> "OpenWindMap"
                        LiveSource.METAR -> "METAR"
                    },
                    fontSize = 16.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
                TextMMD(
                    text = "${stringResource(R.string.observed_at)} $observed",
                    fontSize = 16.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
                WindPair(
                    avgMs = station.windAvgMs,
                    maxMs = station.windMaxMs,
                    unit = windUnit,
                    fontSize = 32.sp,
                    modifier = Modifier.padding(top = 8.dp),
                    colorMode = windColorMode,
                    showUnit = true
                )
                TextMMD(
                    text = direction,
                    fontSize = 20.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
                TextMMD(
                    text = stringResource(
                        if (station.source == LiveSource.METAR) R.string.metar_attribution
                        else R.string.live_attribution
                    ),
                    fontSize = 16.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            HorizontalDividerMMD(thickness = 1.dp)
        }

        item {
            TextMMD(
                text = stringResource(R.string.history_24h),
                fontSize = 20.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            HistoryHeader(windUnit = windUnit)
            HorizontalDividerMMD(thickness = 1.dp)
        }

        if (history.isEmpty()) {
            item {
                TextMMD(
                    text = stringResource(R.string.no_history),
                    fontSize = 18.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            items(items = history, key = { it.timeMs }) { observation ->
                HistoryRow(
                    observation = observation,
                    windUnit = windUnit,
                    timeFormatter = timeFormatter,
                    windColorMode = windColorMode
                )
            }
        }
    }
}

@Composable
private fun HistoryHeader(windUnit: WindUnit) {
    val unit = UnitFormatters.windUnitLabel(windUnit)
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextMMD(text = stringResource(R.string.header_hour), fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.1f))
        TextMMD(
            text = "${stringResource(R.string.wind_avg)}/${stringResource(R.string.wind_gust)} ($unit)",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(2.4f)
        )
        TextMMD(text = stringResource(R.string.header_dir), fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.8f))
    }
}

@Composable
private fun HistoryRow(
    observation: StationObservation,
    windUnit: WindUnit,
    timeFormatter: DateTimeFormatter,
    windColorMode: com.kompaktwind.data.WindColorMode
) {
    val time = timeFormatter.format(Instant.ofEpochMilli(observation.timeMs))
    val direction = observation.headingDeg?.let {
        "${UnitFormatters.arrowGlyph(it)} ${UnitFormatters.cardinal(it)}"
    } ?: "—"

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextMMD(text = time, fontSize = 18.sp, modifier = Modifier.weight(1.1f))
        WindPair(
            avgMs = observation.windAvgMs,
            maxMs = observation.windMaxMs,
            unit = windUnit,
            fontSize = 18.sp,
            modifier = Modifier.weight(2.4f),
            colorMode = windColorMode
        )
        TextMMD(text = direction, fontSize = 18.sp, modifier = Modifier.weight(0.8f))
    }
}
