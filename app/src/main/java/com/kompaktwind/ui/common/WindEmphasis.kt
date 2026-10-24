package com.kompaktwind.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kompaktwind.data.WindColorMode
import com.kompaktwind.data.WindUnit
import com.mudita.mmd.components.text.TextMMD

object WindEmphasis {
    private const val LIGHT_MS = 4.9
    private const val LOW_MS = 7.7
    private const val HIGH_MS = 12.86
    private const val WARN_MS = 15.43
    private const val DANGER_MS = 18.0

    fun tier(ms: Double?): Int = when {
        ms == null -> 1
        ms >= HIGH_MS -> 3
        ms >= LOW_MS -> 2
        ms >= LIGHT_MS -> 1
        else -> 0
    }

    fun weight(tier: Int): FontWeight = when (tier) {
        0 -> FontWeight.Light
        1 -> FontWeight.Normal
        2 -> FontWeight.Bold
        else -> FontWeight.ExtraBold
    }

    fun isWarn(ms: Double?): Boolean = ms != null && ms >= WARN_MS && !isDanger(ms)

    fun isDanger(ms: Double?): Boolean = ms != null && ms >= DANGER_MS

    private const val FOREGROUND_BLACK_LUMINANCE = 0.18f

    private val COLOR_STOPS = listOf(
        0.0 to Color.White,
        4.58 to Color(0xFF67F7F1),
        6.94 to Color(0xFF00FF00),
        9.67 to Color(0xFFFFF000),
        12.71 to Color(0xFFFF322C),
        16.31 to Color(0xFFFF0AC8),
        19.55 to Color(0xFFFF00FF),
        23.15 to Color(0xFF9632FF),
        30.87 to Color(0xFF3C3C3C),
        36.0 to Color(0xFF0000FF),
    )

    fun colorBackground(ms: Double?): Color? {
        if (ms == null) return null
        val value = ms.coerceAtLeast(0.0)
        val upper = COLOR_STOPS.firstOrNull { value <= it.first } ?: return COLOR_STOPS.last().second
        val lowerIndex = (COLOR_STOPS.indexOf(upper) - 1).coerceAtLeast(0)
        val lower = COLOR_STOPS[lowerIndex]
        if (upper.first == lower.first) return upper.second
        return lerp(lower.second, upper.second, ((value - lower.first) / (upper.first - lower.first)).toFloat())
    }

    fun colorForeground(background: Color): Color =
        if (background.luminance() > FOREGROUND_BLACK_LUMINANCE) Color.Black else Color.White
}

@Composable
fun WindPair(
    avgMs: Double?,
    maxMs: Double?,
    unit: WindUnit,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    colorMode: WindColorMode = WindColorMode.MONO,
    showUnit: Boolean = false
) {
    val avg = avgMs?.let { UnitFormatters.windFromMs(it, unit) }
    val max = maxMs?.let { UnitFormatters.windFromMs(it, unit) }
    val avgTier = WindEmphasis.tier(avgMs)
    val maxTier = WindEmphasis.tier(maxMs)

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        WindValue(value = avg, ms = avgMs, fontSize = fontSize, colorMode = colorMode)
        if (max != null) {
            TextMMD(
                text = "-",
                fontSize = fontSize,
                lineHeight = fontSize,
                fontWeight = if (colorMode != WindColorMode.COLOR && avgTier == maxTier) WindEmphasis.weight(avgTier) else FontWeight.Normal,
                maxLines = 1,
                softWrap = false
            )
            WindValue(value = max, ms = maxMs, fontSize = fontSize, colorMode = colorMode)
        }
        if (showUnit) {
            TextMMD(
                text = " ${UnitFormatters.windUnitLabel(unit)}",
                fontSize = if (fontSize.value >= 24f) 16.sp else 14.sp,
                lineHeight = fontSize,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun WindValue(value: Double?, ms: Double?, fontSize: TextUnit, colorMode: WindColorMode) {
    val danger = WindEmphasis.isDanger(ms)
    val warn = WindEmphasis.isWarn(ms)
    val colorBackground = if (colorMode == WindColorMode.COLOR) WindEmphasis.colorBackground(ms) else null
    val background = colorBackground
        ?: if (danger) MaterialTheme.colorScheme.onBackground else Color.Transparent
    val foreground = when {
        colorBackground != null -> WindEmphasis.colorForeground(colorBackground)
        danger -> MaterialTheme.colorScheme.background
        else -> Color.Unspecified
    }
    TextMMD(
        text = UnitFormatters.windValueText(value),
        fontSize = fontSize,
        lineHeight = fontSize,
        fontWeight = if (colorMode == WindColorMode.COLOR) FontWeight.Normal else WindEmphasis.weight(WindEmphasis.tier(ms)),
        textDecoration = if (warn && colorMode == WindColorMode.MONO) TextDecoration.Underline else TextDecoration.None,
        color = foreground,
        maxLines = 1,
        softWrap = false,
        modifier = if (background != Color.Transparent) {
            Modifier.background(background).padding(horizontal = 2.dp)
        } else {
            Modifier
        }
    )
}
