package com.kompaktwind.ui.spots

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kompaktwind.data.HourPoint
import com.kompaktwind.data.Spot
import com.kompaktwind.data.TempUnit
import com.kompaktwind.data.WindUnit
import com.kompaktwind.ui.common.UnitFormatters
import com.mudita.mmd.components.text.TextMMD

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SpotRow(
    spot: Spot,
    latestHour: HourPoint?,
    windUnit: WindUnit,
    tempUnit: TempUnit,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        TextMMD(text = spot.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.size(2.dp))
        if (latestHour != null) {
            val w = latestHour.windSpeedMs?.let { UnitFormatters.windFromMs(it, windUnit) }
            val g = latestHour.windGustMs?.let { UnitFormatters.windFromMs(it, windUnit) }
            val arrow = latestHour.windDirDeg?.let { UnitFormatters.arrowGlyph(it) } ?: ""
            val card = latestHour.windDirDeg?.let { UnitFormatters.cardinal(it) } ?: "—"
            val t = latestHour.tempC?.let { UnitFormatters.tempFromC(it, tempUnit) }
            val tUnit = UnitFormatters.tempUnitLabel(tempUnit)
            val wUnit = UnitFormatters.windUnitLabel(windUnit)
            val windText = if (w != null) "%.0f".format(w) + if (g != null) "/${"%.0f".format(g)}" else "" else "—"
            TextMMD(
                text = "$arrow $card $windText $wUnit · ${t?.let { "%.0f".format(it) } ?: "—"}$tUnit",
                fontSize = 14.sp
            )
        } else {
            TextMMD(
                text = "%.2f, %.2f".format(spot.lat, spot.lon),
                fontSize = 14.sp
            )
        }
    }
}
