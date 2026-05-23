package com.kompaktwind.ui.forecast

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kompaktwind.data.HourPoint
import com.kompaktwind.data.TempUnit
import com.kompaktwind.data.WindUnit
import com.kompaktwind.ui.common.UnitFormatters
import com.mudita.mmd.components.text.TextMMD
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ROW_FONT = 22.sp

@Composable
fun HourRow(
    hour: HourPoint,
    windUnit: WindUnit,
    tempUnit: TempUnit,
    showMarineCols: Boolean
) {
    val hh = SimpleDateFormat("HH", Locale.getDefault()).format(Date(hour.timeEpochMs))
    val w = hour.windSpeedMs?.let { UnitFormatters.windFromMs(it, windUnit) }
    val g = hour.windGustMs?.let { UnitFormatters.windFromMs(it, windUnit) }
    val arrow = hour.windDirDeg?.let { UnitFormatters.arrowGlyph(it) } ?: "—"
    val card = hour.windDirDeg?.let { UnitFormatters.cardinal(it) } ?: ""
    val windSpeed = w?.let { "%.0f".format(it) } ?: "—"
    val gustPart = if (g != null) "/${"%.0f".format(g)}" else ""

    val t = hour.tempC?.let { UnitFormatters.tempFromC(it, tempUnit) }
    val tStr = t?.let { "%.0f°".format(it) } ?: "—"

    val pStr = hour.precipMm?.let { if (it == 0.0) "0" else "%.1f".format(it) } ?: "—"
    val waveStr = if (showMarineCols && hour.waveHeightM != null) {
        val h = if (hour.waveHeightM == 0.0) "0" else "%.1f".format(hour.waveHeightM)
        val period = hour.wavePeriodS?.let { "/${"%.0f".format(it)}" } ?: ""
        "$h$period"
    } else " "
    val waterStr = if (showMarineCols && hour.waterTempC != null) {
        "%.0f°".format(hour.waterTempC)
    } else " "

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        TextMMD(text = hh, fontSize = ROW_FONT, modifier = Modifier.weight(1f))
        Column(modifier = Modifier.weight(0.8f), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
            TextMMD(text = arrow, fontSize = 26.sp)
            TextMMD(text = card, fontSize = 10.sp)
        }
        TextMMD(text = "$windSpeed$gustPart", fontSize = ROW_FONT, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f))
        TextMMD(text = tStr, fontSize = ROW_FONT, modifier = Modifier.weight(1f))
        TextMMD(text = pStr, fontSize = ROW_FONT, modifier = Modifier.weight(1f))
        if (showMarineCols) {
            TextMMD(text = waveStr, fontSize = ROW_FONT, modifier = Modifier.weight(1.3f))
            TextMMD(text = waterStr, fontSize = ROW_FONT, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun ColumnHeader(windUnit: WindUnit, showMarineCols: Boolean) {
    val wLabel = UnitFormatters.windUnitLabel(windUnit)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        TextMMD(text = "Hour", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        TextMMD(text = "↑", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.7f))
        TextMMD(text = wLabel, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f))
        TextMMD(text = "°", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        TextMMD(text = "mm", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        if (showMarineCols) {
            TextMMD(text = "Wave", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.3f))
            TextMMD(text = "Sea", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        }
    }
}
