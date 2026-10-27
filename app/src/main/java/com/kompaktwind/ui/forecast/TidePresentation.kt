package com.kompaktwind.ui.forecast

import com.kompaktwind.data.HourPoint
import java.util.Locale
import kotlin.math.abs

enum class TideTrend { RISING, FALLING, HIGH, LOW, UNKNOWN }

data class TideInfo(
    val heightM: Double?,
    val trend: TideTrend,
    val weightTier: Int
)

object TidePresentation {
    private const val HOUR_MS = 3_600_000.0

    fun from(hours: List<HourPoint>): Map<Long, TideInfo> {
        val heights = hours.map { it.seaLevelM?.takeIf(Double::isFinite) }

        fun nextDistinct(fromIndex: Int, value: Double): Double? {
            var j = fromIndex + 1
            while (j < heights.size && (heights[j] == null || heights[j] == value)) {
                j++
            }
            return heights.getOrNull(j)
        }

        fun prevDistinct(fromIndex: Int, value: Double): Double? {
            var j = fromIndex - 1
            while (j >= 0 && (heights[j] == null || heights[j] == value)) {
                j--
            }
            return heights.getOrNull(j)
        }

        fun rateBetween(i1: Int, h1: Double, i2: Int, h2: Double): Double? {
            val dtHours = (hours[i2].timeEpochMs - hours[i1].timeEpochMs) / HOUR_MS
            return if (dtHours > 0.0) abs(h2 - h1) / dtHours else null
        }

        return heights.mapIndexed { index, height ->
            val previous = heights.getOrNull(index - 1)
            val next = heights.getOrNull(index + 1)
            val afterPlateau = if (height != null) nextDistinct(index, height) else null
            val beforePlateau = if (height != null) prevDistinct(index, height) else null
            val firstOfPlateau = previous == null || previous != height
            val highByBothSides = beforePlateau != null && afterPlateau != null &&
                height != null && height > beforePlateau && height > afterPlateau
            val highAtStart = previous == null && next == height && afterPlateau != null &&
                height != null && height > afterPlateau
            val highAtEnd = afterPlateau == null && previous == height &&
                beforePlateau != null && height != null && height > beforePlateau
            val high = height != null &&
                ((firstOfPlateau && (highByBothSides || highAtStart)) || highAtEnd)
            val lowByBothSides = beforePlateau != null && afterPlateau != null &&
                height != null && height < beforePlateau && height < afterPlateau
            val lowAtStart = previous == null && next == height && afterPlateau != null &&
                height != null && height < afterPlateau
            val lowAtEnd = afterPlateau == null && previous == height &&
                beforePlateau != null && height != null && height < beforePlateau
            val low = height != null &&
                ((firstOfPlateau && (lowByBothSides || lowAtStart)) || lowAtEnd)
            val trend = when {
                height == null -> TideTrend.UNKNOWN
                high -> TideTrend.HIGH
                low -> TideTrend.LOW
                afterPlateau != null && afterPlateau > height -> TideTrend.RISING
                afterPlateau != null && afterPlateau < height -> TideTrend.FALLING
                beforePlateau != null && beforePlateau < height -> TideTrend.RISING
                beforePlateau != null && beforePlateau > height -> TideTrend.FALLING
                else -> TideTrend.UNKNOWN
            }
            val rateMh = when {
                height == null -> null
                previous != null && next != null -> rateBetween(index - 1, previous, index + 1, next)
                next != null -> rateBetween(index, height, index + 1, next)
                previous != null -> rateBetween(index - 1, previous, index, height)
                else -> 0.0
            }
            val rateTier = weightTier(rateMh)
            hours[index].timeEpochMs to TideInfo(
                heightM = height,
                trend = trend,
                weightTier = if (high || low) rateTier.coerceAtLeast(1) else rateTier
            )
        }.toMap()
    }

    fun available(hours: List<HourPoint>): Boolean =
        hours.any { it.seaLevelM?.isFinite() == true }

    fun label(
        heightM: Double?,
        trend: TideTrend,
        locale: Locale = Locale.getDefault()
    ): String {
        if (heightM == null || !heightM.isFinite()) return "—"
        val normalized = if (abs(heightM) < 0.05) 0.0 else heightM
        val value = String.format(locale, "%+.1f", normalized)
        val symbol = when (trend) {
            TideTrend.HIGH -> "▲"
            TideTrend.LOW -> "▼"
            else -> ""
        }
        return "$value$symbol"
    }

    internal fun weightTier(rateMh: Double?): Int = when {
        rateMh == null -> 1
        rateMh >= 0.45 -> 3
        rateMh >= 0.25 -> 2
        rateMh >= 0.10 -> 1
        else -> 0
    }
}
