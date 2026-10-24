package com.kompaktwind.ui.spots

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kompaktwind.R
import com.kompaktwind.data.HourPoint
import com.kompaktwind.data.Spot
import com.kompaktwind.data.TempUnit
import com.kompaktwind.data.WindColorMode
import com.kompaktwind.data.WindUnit
import com.kompaktwind.data.catalog.CatalogSpot
import com.kompaktwind.ui.common.UnitFormatters
import com.kompaktwind.ui.common.WindPair
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.text.TextMMD

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SpotRow(
    spot: Spot,
    catalogSpot: CatalogSpot?,
    windUnit: WindUnit,
    tempUnit: TempUnit,
    windColorMode: WindColorMode = WindColorMode.MONO,
    showForecast: Boolean = false,
    forecastHour: HourPoint? = null,
    forecastLoading: Boolean = false,
    showActions: Boolean = false,
    canMoveUp: Boolean = true,
    canMoveDown: Boolean = true,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMoveUp: () -> Unit = {},
    onMoveDown: () -> Unit = {},
    onDelete: () -> Unit = {},
    onInfo: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextMMD(
                text = spot.name,
                fontSize = 20.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            if (showActions) {
                OutlinedButtonMMD(
                    onClick = onMoveUp,
                    enabled = canMoveUp,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Filled.ArrowUpward,
                        contentDescription = stringResource(R.string.move_up),
                        modifier = Modifier.size(24.dp)
                    )
                }
                OutlinedButtonMMD(
                    onClick = onMoveDown,
                    enabled = canMoveDown,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Filled.ArrowDownward,
                        contentDescription = stringResource(R.string.move_down),
                        modifier = Modifier.size(24.dp)
                    )
                }
                OutlinedButtonMMD(
                    onClick = onDelete,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(R.string.delete),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            if (onInfo != null) {
                Box(
                    modifier = Modifier.size(40.dp).clickable(onClick = onInfo),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Lightbulb,
                        contentDescription = stringResource(R.string.spot_info),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
        if (showForecast) {
            when {
                forecastHour != null -> HourLine(
                    forecastHour,
                    windUnit,
                    tempUnit,
                    windColorMode,
                    showTemperature = false
                )
                forecastLoading -> TextMMD(text = "…", fontSize = 18.sp)
                else -> TextMMD(text = "—", fontSize = 18.sp)
            }
        } else if (catalogSpot != null) {
            val recommended = stringResource(R.string.recommended_winds)
            val avoid = stringResource(R.string.avoid_winds)
            val details = buildList {
                val place = listOfNotNull(catalogSpot.region, catalogSpot.country).joinToString(", ")
                if (place.isNotBlank()) add(place)
                if (catalogSpot.windDirections.isNotEmpty()) {
                    add("$recommended: ${catalogSpot.windDirections.joinToString(" ")}")
                }
                if (catalogSpot.avoidWindDirections.isNotEmpty()) {
                    add("$avoid: ${catalogSpot.avoidWindDirections.joinToString(" ")}")
                }
            }.joinToString(" · ")
            if (details.isNotBlank()) {
                TextMMD(text = details, fontSize = 16.sp)
            }
        } else {
            TextMMD(
                text = "%.2f, %.2f".format(spot.lat, spot.lon),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun HourLine(
    hour: HourPoint,
    windUnit: WindUnit,
    tempUnit: TempUnit,
    windColorMode: WindColorMode,
    showTemperature: Boolean = true
) {
    val arrow = hour.windDirDeg?.let { UnitFormatters.arrowGlyph(it) } ?: ""
    val card = hour.windDirDeg?.let { UnitFormatters.cardinal(it) } ?: "—"
    val t = hour.tempC?.let { UnitFormatters.tempFromC(it, tempUnit) }
    val tUnit = UnitFormatters.tempUnitLabel(tempUnit)
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextMMD(text = "$arrow $card ", fontSize = 18.sp, lineHeight = 18.sp, maxLines = 1, softWrap = false)
        WindPair(
            avgMs = hour.windSpeedMs,
            maxMs = hour.windGustMs,
            unit = windUnit,
            fontSize = 18.sp,
            colorMode = windColorMode,
            showUnit = true
        )
        if (showTemperature) {
            TextMMD(
                text = " · ${t?.let { "%.0f".format(it) } ?: "—"}$tUnit",
                fontSize = 18.sp,
                lineHeight = 18.sp,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}
