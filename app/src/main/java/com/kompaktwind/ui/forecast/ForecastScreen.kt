package com.kompaktwind.ui.forecast

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kompaktwind.data.ForecastUiState
import com.kompaktwind.data.MarineDisplay
import com.kompaktwind.data.TempUnit
import com.kompaktwind.data.WindUnit
import com.kompaktwind.ui.KompaktWindViewModel
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.lazy.LazyColumnMMD
import com.mudita.mmd.components.progress_indicator.CircularProgressIndicatorMMD
import com.mudita.mmd.components.text.TextMMD
import java.util.Calendar
import java.util.TimeZone

@Composable
fun ForecastScreen(
    viewModel: KompaktWindViewModel,
    spotId: String
) {
    val spots by viewModel.spots.collectAsState()
    val forecast by viewModel.forecast.collectAsState()
    val settings by viewModel.settingsState.collectAsState()
    val spot = spots.firstOrNull { it.id == spotId }

    LaunchedEffect(spotId, spot) {
        if (spot != null) viewModel.loadForecast(spot)
    }

    when (val s = forecast) {
        is ForecastUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicatorMMD()
            }
        }
        is ForecastUiState.Data -> ForecastTable(
            state = s,
            windUnit = settings.windUnit,
            tempUnit = settings.tempUnit,
            showMarine = shouldShowMarine(settings.marineDisplay, spot?.isCoastal ?: false),
            providerAttribution = "Data: Open-Meteo",
            onRefresh = { spot?.let { viewModel.loadForecast(it, forceRefresh = true) } }
        )
        is ForecastUiState.Error -> {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                TextMMD(text = "Couldn't load forecast: ${s.message}")
                ButtonMMD(onClick = { spot?.let { viewModel.loadForecast(it, forceRefresh = true) } }) {
                    TextMMD("Try again")
                }
                if (s.cached != null) {
                    HorizontalDividerMMD()
                    ForecastTable(
                        state = s.cached.copy(isStale = true),
                        windUnit = settings.windUnit,
                        tempUnit = settings.tempUnit,
                        showMarine = shouldShowMarine(settings.marineDisplay, spot?.isCoastal ?: false),
                        providerAttribution = "Data: Open-Meteo (cached)",
                        onRefresh = { spot?.let { viewModel.loadForecast(it, forceRefresh = true) } }
                    )
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
    providerAttribution: String,
    @Suppress("UNUSED_PARAMETER") onRefresh: () -> Unit
) {
    val nowMs = System.currentTimeMillis()
    val currentHourMs = nowMs - (nowMs % 3_600_000)
    val hours = state.forecast.hours.filter { it.timeEpochMs >= currentHourMs }
    val grouped = hours.groupBy { dayKey(it.timeEpochMs, state.forecast.timezone) }

    Column(modifier = Modifier.fillMaxSize()) {
        if (state.isStale) {
            TextMMD(
                text = "Offline — showing cached data",
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
        LazyColumnMMD(modifier = Modifier.weight(1f)) {
            grouped.forEach { (_, dayHours) ->
                item { DayHeader(epochMs = dayHours.first().timeEpochMs) }
                item { ColumnHeader(windUnit = windUnit, showMarineCols = showMarine) }
                item { HorizontalDividerMMD() }
                items(items = dayHours, key = { it.timeEpochMs }) { hour ->
                    HourRow(
                        hour = hour,
                        windUnit = windUnit,
                        tempUnit = tempUnit,
                        showMarineCols = showMarine
                    )
                }
            }
        }
        TextMMD(text = providerAttribution, fontSize = 16.sp, modifier = Modifier.padding(8.dp))
    }
}

private fun dayKey(epochMs: Long, tz: String): String {
    val cal = Calendar.getInstance(TimeZone.getTimeZone(tz))
    cal.timeInMillis = epochMs
    return "%04d-%02d-%02d".format(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
}
