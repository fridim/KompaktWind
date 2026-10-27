package com.kompaktwind.ui.forecast

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kompaktwind.R
import com.kompaktwind.data.HourPoint
import com.kompaktwind.data.TempUnit
import com.kompaktwind.data.WindColorMode
import com.kompaktwind.data.WindUnit
import com.kompaktwind.ui.common.UnitFormatters
import com.kompaktwind.ui.common.WindEmphasis
import com.mudita.mmd.components.text.TextMMD
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val ROW_FONT = 18.sp
private val CARD_FONT = 10.sp

private const val WAVE_LIGHT_M = 0.5
private const val WAVE_BOLD_M = 1.0
private const val WAVE_STRONG_M = 1.5
private const val WAVE_DANGER_M = 2.5
private const val WAVE_COL_WEIGHT = 1.5f
private const val TEMP_COL_WEIGHT = 0.9f
private const val SEA_COL_WEIGHT = 0.8f
private const val TIDE_COL_WEIGHT = 1.7f

private fun waveWeightTier(heightM: Double?): Int = when {
    heightM == null -> 1
    heightM >= WAVE_STRONG_M -> 3
    heightM >= WAVE_BOLD_M -> 2
    heightM >= WAVE_LIGHT_M -> 1
    else -> 0
}

@Composable
fun HourRow(
    hour: HourPoint,
    windUnit: WindUnit,
    tempUnit: TempUnit,
    showMarineCols: Boolean,
    showTideCol: Boolean = false,
    tide: TideInfo? = null,
    onToggleTides: (() -> Unit)? = null,
    timezone: String = ZoneId.systemDefault().id,
    colorMode: WindColorMode = WindColorMode.MONO
) {
    val locale = LocalConfiguration.current.locales[0]
    val hourFormatter = remember(timezone, locale) {
        DateTimeFormatter.ofPattern("HH", locale).withZone(ZoneId.of(timezone))
    }
    val hh = hourFormatter.format(Instant.ofEpochMilli(hour.timeEpochMs))
    val w = hour.windSpeedMs?.let { UnitFormatters.windFromMs(it, windUnit) }
    val g = hour.windGustMs?.let { UnitFormatters.windFromMs(it, windUnit) }
    val windRotation = hour.windDirDeg?.let { ((it + 180) % 360).toFloat() }
    val card = hour.windDirDeg?.let { UnitFormatters.cardinal(it) } ?: ""
    val speedTier = WindEmphasis.tier(hour.windSpeedMs)
    val gustTier = WindEmphasis.tier(hour.windGustMs)
    val hourTier = maxOf(speedTier, gustTier)
    val speedDanger = WindEmphasis.isDanger(hour.windSpeedMs)
    val gustDanger = WindEmphasis.isDanger(hour.windGustMs)
    val speedWarn = WindEmphasis.isWarn(hour.windSpeedMs)
    val gustWarn = WindEmphasis.isWarn(hour.windGustMs)
    val windSpeed = UnitFormatters.windValueText(w)
    val gustSpeed = g?.let { UnitFormatters.windValueText(it) }
    val speedWeight = if (colorMode == WindColorMode.COLOR) FontWeight.Normal else WindEmphasis.weight(speedTier)
    val gustWeight = if (colorMode == WindColorMode.COLOR) FontWeight.Normal else WindEmphasis.weight(gustTier)
    val separatorWeight = if (colorMode != WindColorMode.COLOR && gustSpeed != null && speedTier == gustTier) speedWeight else FontWeight.Normal
    val speedDecoration = if (speedWarn && colorMode == WindColorMode.MONO) TextDecoration.Underline else TextDecoration.None
    val gustDecoration = if (gustWarn && colorMode == WindColorMode.MONO) TextDecoration.Underline else TextDecoration.None
    val speedColorBg = if (colorMode == WindColorMode.COLOR) WindEmphasis.colorBackground(hour.windSpeedMs) else null
    val gustColorBg = if (colorMode == WindColorMode.COLOR) WindEmphasis.colorBackground(hour.windGustMs) else null
    val speedColor = when {
        speedColorBg != null -> WindEmphasis.colorForeground(speedColorBg)
        speedDanger -> MaterialTheme.colorScheme.background
        else -> Color.Unspecified
    }
    val gustColor = when {
        gustColorBg != null -> WindEmphasis.colorForeground(gustColorBg)
        gustDanger -> MaterialTheme.colorScheme.background
        else -> Color.Unspecified
    }
    val speedBgModifier = when {
        speedColorBg != null -> Modifier.background(speedColorBg).padding(horizontal = 2.dp)
        speedDanger -> Modifier.background(MaterialTheme.colorScheme.onBackground).padding(horizontal = 2.dp)
        else -> Modifier
    }
    val gustBgModifier = when {
        gustColorBg != null -> Modifier.background(gustColorBg).padding(horizontal = 2.dp)
        gustDanger -> Modifier.background(MaterialTheme.colorScheme.onBackground).padding(horizontal = 2.dp)
        else -> Modifier
    }

    val t = hour.tempC?.let { UnitFormatters.tempFromC(it, tempUnit) }
    val tStr = t?.let { "%.0f°".format(it) } ?: "—"

    val pStr = hour.precipMm?.let { "%.1f".format(it).removeSuffix(".0") } ?: "—"
    val waveHeight = hour.waveHeightM
    val waveHeightStr = if (showMarineCols && waveHeight != null) {
        "%.1f".format(waveHeight).removeSuffix(".0")
    } else " "
    val wavePeriodStr = if (showMarineCols && waveHeight != null && hour.wavePeriodS != null) {
        "%.0f".format(hour.wavePeriodS)
    } else null
    val waveWeight = WindEmphasis.weight(waveWeightTier(waveHeight))
    val waveDecoration = if (showMarineCols && waveHeight != null && waveHeight >= WAVE_STRONG_M) {
        TextDecoration.Underline
    } else {
        TextDecoration.None
    }
    val waveDanger = showMarineCols && waveHeight != null && waveHeight >= WAVE_DANGER_M
    val waveColor = if (waveDanger) MaterialTheme.colorScheme.background else Color.Unspecified
    val waveBgModifier = if (waveDanger) {
        Modifier.background(MaterialTheme.colorScheme.onBackground).padding(horizontal = 2.dp)
    } else {
        Modifier
    }
    val waterStr = if (showMarineCols && hour.waterTempC != null) {
        "%.0f°".format(hour.waterTempC)
    } else " "
    val tideStr = if (showTideCol) {
        TidePresentation.label(tide?.heightM, tide?.trend ?: TideTrend.UNKNOWN)
    } else {
        " "
    }
    val tideWeight = WindEmphasis.weight(tide?.weightTier ?: 1)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onToggleTides != null) {
                    Modifier.clickable(onClick = onToggleTides)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val hhWeight = if (hourTier >= 2) FontWeight.Bold else FontWeight.Normal
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
        Row(
            modifier = Modifier.weight(1.6f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextMMD(
                text = windSpeed,
                fontSize = ROW_FONT,
                lineHeight = ROW_FONT,
                fontWeight = speedWeight,
                textDecoration = speedDecoration,
                color = speedColor,
                maxLines = 1,
                softWrap = false,
                modifier = speedBgModifier
            )
            if (gustSpeed != null) {
                TextMMD(
                    text = "-",
                    fontSize = ROW_FONT,
                    lineHeight = ROW_FONT,
                    fontWeight = separatorWeight,
                    maxLines = 1,
                    softWrap = false
                )
                TextMMD(
                    text = gustSpeed,
                    fontSize = ROW_FONT,
                    lineHeight = ROW_FONT,
                    fontWeight = gustWeight,
                    textDecoration = gustDecoration,
                    color = gustColor,
                    maxLines = 1,
                    softWrap = false,
                    modifier = gustBgModifier
                )
            }
        }
        TextMMD(text = pStr, fontSize = ROW_FONT, modifier = Modifier.weight(0.8f))
        if (showMarineCols) {
            Row(
                modifier = Modifier.weight(WAVE_COL_WEIGHT),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextMMD(
                    text = waveHeightStr,
                    fontSize = ROW_FONT,
                    lineHeight = ROW_FONT,
                    fontWeight = waveWeight,
                    textDecoration = waveDecoration,
                    color = waveColor,
                    maxLines = 1,
                    softWrap = false,
                    modifier = waveBgModifier
                )
                if (wavePeriodStr != null) {
                    TextMMD(
                        text = "/$wavePeriodStr",
                        fontSize = 16.sp,
                        lineHeight = ROW_FONT,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(start = 1.dp)
                    )
                }
            }
            if (!showTideCol) {
                TextMMD(text = tStr, fontSize = ROW_FONT, modifier = Modifier.weight(TEMP_COL_WEIGHT))
            }
            if (showTideCol) {
                TextMMD(
                    text = tideStr,
                    fontSize = ROW_FONT,
                    lineHeight = ROW_FONT,
                    fontWeight = tideWeight,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.weight(TIDE_COL_WEIGHT)
                )
            } else {
                TextMMD(text = waterStr, fontSize = ROW_FONT, modifier = Modifier.weight(SEA_COL_WEIGHT))
            }
        } else {
            TextMMD(text = tStr, fontSize = ROW_FONT, modifier = Modifier.weight(TEMP_COL_WEIGHT))
        }
    }
}

@Composable
fun ColumnHeader(
    windUnit: WindUnit,
    showMarineCols: Boolean,
    showTideCol: Boolean = false,
    onOpenHelp: () -> Unit = {}
) {
    val wLabel = UnitFormatters.windUnitLabel(windUnit)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenHelp)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextMMD(text = stringResource(R.string.header_hour), fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.8f))
        Icon(imageVector = Icons.Filled.ArrowUpward, contentDescription = stringResource(R.string.wind_direction), modifier = Modifier.weight(0.5f).size(16.dp))
        TextMMD(text = "", fontSize = 14.sp, modifier = Modifier.weight(0.5f))
        TextMMD(text = wLabel, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.6f))
        TextMMD(text = stringResource(R.string.header_precip), fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.8f))
        if (showMarineCols) {
            TextMMD(
                text = stringResource(R.string.header_wave),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(WAVE_COL_WEIGHT)
            )
            if (!showTideCol) {
                TextMMD(
                    text = stringResource(R.string.header_temp),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(TEMP_COL_WEIGHT)
                )
            }
            TextMMD(
                text = stringResource(if (showTideCol) R.string.header_tide else R.string.header_sea),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(if (showTideCol) TIDE_COL_WEIGHT else SEA_COL_WEIGHT)
            )
        } else {
            TextMMD(
                text = stringResource(R.string.header_temp),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(TEMP_COL_WEIGHT)
            )
        }
    }
}
