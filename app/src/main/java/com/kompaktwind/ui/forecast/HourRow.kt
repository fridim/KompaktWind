package com.kompaktwind.ui.forecast

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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

@Composable
fun HourRow(
    hour: HourPoint,
    windUnit: WindUnit,
    tempUnit: TempUnit,
    showMarineCols: Boolean
) {
    val hh = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(hour.timeEpochMs))
    val w = hour.windSpeedMs?.let { UnitFormatters.windFromMs(it, windUnit) }
    val g = hour.windGustMs?.let { UnitFormatters.windFromMs(it, windUnit) }
    val wUnit = UnitFormatters.windUnitLabel(windUnit)
    val arrow = hour.windDirDeg?.let { UnitFormatters.arrowGlyph(it) } ?: " "
    val card = hour.windDirDeg?.let { UnitFormatters.cardinal(it) } ?: "—"
    val wind = "$arrow $card ${w?.let { "%.0f".format(it) } ?: "—"}${if (g != null) "/${"%.0f".format(g)}" else ""}$wUnit"

    val t = hour.tempC?.let { UnitFormatters.tempFromC(it, tempUnit) }
    val tStr = t?.let { "%.0f".format(it) + UnitFormatters.tempUnitLabel(tempUnit) } ?: "—"

    val pStr = hour.precipMm?.let { "%.1fmm".format(it) } ?: "—"
    val waveStr = if (showMarineCols && hour.waveHeightM != null) {
        val h = "%.1fm".format(hour.waveHeightM)
        val period = hour.wavePeriodS?.let { "/${"%.0f".format(it)}s" } ?: ""
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
        TextMMD(text = hh, fontSize = 14.sp, modifier = Modifier.weight(1.2f))
        TextMMD(text = wind, fontSize = 14.sp, modifier = Modifier.weight(3.5f))
        TextMMD(text = tStr, fontSize = 14.sp, modifier = Modifier.weight(1.2f))
        TextMMD(text = pStr, fontSize = 14.sp, modifier = Modifier.weight(1.3f))
        TextMMD(text = waveStr, fontSize = 14.sp, modifier = Modifier.weight(1.5f))
        TextMMD(text = waterStr, fontSize = 14.sp, modifier = Modifier.weight(1.0f))
    }
}
