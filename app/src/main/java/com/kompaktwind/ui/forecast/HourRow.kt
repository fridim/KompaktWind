package com.kompaktwind.ui.forecast

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
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
import java.util.TimeZone

private val ROW_FONT = 18.sp
private val CARD_FONT = 10.sp

private const val WING_LIGHT_MS = 4.9  // ~9.5 knots — below displayed "10"
private const val WING_LOW_MS = 7.7    // ~15 knots
private const val WING_HIGH_MS = 12.86 // ~25 knots
private const val WING_WARN_MS = 15.43 // ~30 knots
private const val WING_DANGER_MS = 18.0 // ~35 knots

// Font weight tiers: 0=Light, 1=Normal, 2=Bold, 3=ExtraBold
private fun weightTier(ms: Double?): Int = when {
    ms == null -> 1
    ms >= WING_HIGH_MS -> 3
    ms >= WING_LOW_MS -> 2
    ms >= WING_LIGHT_MS -> 1
    else -> 0
}

private fun windWeightTier(speedMs: Double?, gustMs: Double?): Int {
    val base = weightTier(speedMs)
    val gustBump = if (gustMs != null && weightTier(gustMs) > base) 1 else 0
    return (base + gustBump).coerceAtMost(3)
}

private fun tierToFontWeight(tier: Int): FontWeight = when (tier) {
    0 -> FontWeight.Light
    1 -> FontWeight.Normal
    2 -> FontWeight.Bold
    else -> FontWeight.ExtraBold
}

private fun isDanger(speedMs: Double?, gustMs: Double?): Boolean =
    listOfNotNull(speedMs, gustMs).any { it >= WING_DANGER_MS }

private fun isWarn(speedMs: Double?, gustMs: Double?): Boolean =
    listOfNotNull(speedMs, gustMs).any { it >= WING_WARN_MS }

@Composable
fun HourRow(
    hour: HourPoint,
    windUnit: WindUnit,
    tempUnit: TempUnit,
    showMarineCols: Boolean,
    timezone: String = TimeZone.getDefault().id
) {
    val hh = SimpleDateFormat("HH", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone(timezone)
    }.format(Date(hour.timeEpochMs))
    val w = hour.windSpeedMs?.let { UnitFormatters.windFromMs(it, windUnit) }
    val g = hour.windGustMs?.let { UnitFormatters.windFromMs(it, windUnit) }
    val windRotation = hour.windDirDeg?.let { ((it + 180) % 360).toFloat() }
    val card = hour.windDirDeg?.let { UnitFormatters.cardinal(it) } ?: ""
    val tier = windWeightTier(hour.windSpeedMs, hour.windGustMs)
    val windWeight = tierToFontWeight(tier)
    val danger = isDanger(hour.windSpeedMs, hour.windGustMs)
    val warn = !danger && isWarn(hour.windSpeedMs, hour.windGustMs)
    val windSpeed = w?.let { "%.0f".format(it) } ?: "—"
    val gustPart = if (g != null) "-${"%.0f".format(g)}" else ""
    val windDecoration = if (warn) TextDecoration.Underline else TextDecoration.None
    val windColor = if (danger) Color.White else Color.Unspecified
    val windBgModifier = if (danger)
        Modifier.background(Color.Black).padding(horizontal = 4.dp)
    else Modifier

    val t = hour.tempC?.let { UnitFormatters.tempFromC(it, tempUnit) }
    val tStr = t?.let { "%.0f°".format(it) } ?: "—"

    val pStr = hour.precipMm?.let { "%.1f".format(it).removeSuffix(".0") } ?: "—"
    val waveStr = if (showMarineCols && hour.waveHeightM != null) {
        val h = "%.1f".format(hour.waveHeightM).removeSuffix(".0")
        val period = hour.wavePeriodS?.let { "/${"%.0f".format(it)}" } ?: ""
        "$h$period"
    } else " "
    val waterStr = if (showMarineCols && hour.waterTempC != null) {
        "%.0f°".format(hour.waterTempC)
    } else " "

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val hhWeight = if (tier >= 2) FontWeight.Bold else FontWeight.Normal
        TextMMD(text = hh, fontSize = ROW_FONT, fontWeight = hhWeight, modifier = Modifier.weight(0.8f))
        if (windRotation != null) {
            Icon(
                imageVector = Icons.Filled.ArrowUpward,
                contentDescription = card,
                modifier = Modifier.weight(0.5f).size(22.dp).rotate(windRotation)
            )
        } else {
            TextMMD(text = "—", fontSize = ROW_FONT, modifier = Modifier.weight(0.5f))
        }
        TextMMD(text = card, fontSize = CARD_FONT, modifier = Modifier.weight(0.5f))
        TextMMD(text = "$windSpeed$gustPart", fontSize = ROW_FONT, fontWeight = windWeight, textDecoration = windDecoration, color = windColor, modifier = Modifier.weight(1.3f).then(windBgModifier))
        TextMMD(text = tStr, fontSize = ROW_FONT, modifier = Modifier.weight(0.9f))
        TextMMD(text = pStr, fontSize = ROW_FONT, modifier = Modifier.weight(0.8f))
        if (showMarineCols) {
            TextMMD(text = waveStr, fontSize = ROW_FONT, modifier = Modifier.weight(1.2f))
            TextMMD(text = waterStr, fontSize = ROW_FONT, modifier = Modifier.weight(0.8f))
        }
    }
}

@Composable
fun ColumnHeader(windUnit: WindUnit, showMarineCols: Boolean) {
    val wLabel = UnitFormatters.windUnitLabel(windUnit)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextMMD(text = "H", fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.8f))
        Icon(imageVector = Icons.Filled.ArrowUpward, contentDescription = "Wind direction", modifier = Modifier.weight(0.5f).size(16.dp))
        TextMMD(text = "", fontSize = 14.sp, modifier = Modifier.weight(0.5f))
        TextMMD(text = wLabel, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.3f))
        TextMMD(text = "°", fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.9f))
        TextMMD(text = "mm", fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.8f))
        if (showMarineCols) {
            TextMMD(text = "Wave", fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f))
            TextMMD(text = "Sea", fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.8f))
        }
    }
}
