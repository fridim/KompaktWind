package com.kompaktwind.ui.forecast

import com.google.common.truth.Truth.assertThat
import com.kompaktwind.data.HourPoint
import java.util.Locale
import org.junit.Test

class TidePresentationTest {

    private val hourMs = 3_600_000L

    private fun hours(vararg heights: Double?): List<HourPoint> =
        heights.mapIndexed { index, height ->
            HourPoint(timeEpochMs = index * hourMs, seaLevelM = height)
        }

    @Test fun `detects high low and rising falling`() {
        val result = TidePresentation.from(
            hours(0.5, 1.0, 1.5, 1.2, 0.8, 0.4, 0.6)
        )

        assertThat(result[0L * hourMs]!!.trend).isEqualTo(TideTrend.RISING)
        assertThat(result[1L * hourMs]!!.trend).isEqualTo(TideTrend.RISING)
        assertThat(result[2L * hourMs]!!.trend).isEqualTo(TideTrend.HIGH)
        assertThat(result[3L * hourMs]!!.trend).isEqualTo(TideTrend.FALLING)
        assertThat(result[4L * hourMs]!!.trend).isEqualTo(TideTrend.FALLING)
        assertThat(result[5L * hourMs]!!.trend).isEqualTo(TideTrend.LOW)
        assertThat(result[6L * hourMs]!!.trend).isEqualTo(TideTrend.RISING)
    }

    @Test fun `boldness follows how fast the tide is moving`() {
        val result = TidePresentation.from(
            hours(0.0, 0.6, 1.2, 1.3, 1.5)
        )

        assertThat(result[0L]!!.weightTier).isEqualTo(3)
        assertThat(result[3L * hourMs]!!.weightTier).isEqualTo(1)
        assertThat(result[4L * hourMs]!!.weightTier).isNotEqualTo(3)
    }

    @Test fun `unknown heights do not break the series`() {
        val result = TidePresentation.from(hours(null, 1.0, 1.2))

        assertThat(result[0L]!!.heightM).isNull()
        assertThat(result[0L]!!.trend).isEqualTo(TideTrend.UNKNOWN)
        assertThat(result[hourMs]!!.trend).isEqualTo(TideTrend.RISING)
    }

    @Test fun `formats compact tide labels`() {
        assertThat(TidePresentation.label(1.2, TideTrend.RISING, Locale.ROOT)).isEqualTo("+1.2")
        assertThat(TidePresentation.label(0.4, TideTrend.FALLING, Locale.ROOT)).isEqualTo("+0.4")
        assertThat(TidePresentation.label(2.3, TideTrend.HIGH, Locale.ROOT)).isEqualTo("+2.3▲")
        assertThat(TidePresentation.label(-0.4, TideTrend.LOW, Locale.ROOT)).isEqualTo("-0.4▼")
        assertThat(TidePresentation.label(-0.0, TideTrend.RISING, Locale.ROOT)).isEqualTo("+0.0")
        assertThat(TidePresentation.label(-0.04, TideTrend.FALLING, Locale.ROOT)).isEqualTo("+0.0")
        assertThat(TidePresentation.label(Double.NaN, TideTrend.UNKNOWN, Locale.ROOT)).isEqualTo("—")
        assertThat(TidePresentation.label(Double.POSITIVE_INFINITY, TideTrend.HIGH, Locale.ROOT)).isEqualTo("—")
        assertThat(TidePresentation.label(null, TideTrend.UNKNOWN, Locale.ROOT)).isEqualTo("—")
        assertThat(TidePresentation.label(1.2, TideTrend.RISING, Locale.FRANCE)).isEqualTo("+1,2")
    }

    @Test fun `available reports whether any finite tide height exists`() {
        assertThat(TidePresentation.available(hours(null, null))).isFalse()
        assertThat(TidePresentation.available(hours(0.8))).isTrue()
        assertThat(TidePresentation.available(hours(Double.NaN, 0.8))).isTrue()
    }

    @Test fun `weight tiers use the documented thresholds`() {
        assertThat(TidePresentation.weightTier(null)).isEqualTo(1)
        assertThat(TidePresentation.weightTier(0.09)).isEqualTo(0)
        assertThat(TidePresentation.weightTier(0.10)).isEqualTo(1)
        assertThat(TidePresentation.weightTier(0.25)).isEqualTo(2)
        assertThat(TidePresentation.weightTier(0.45)).isEqualTo(3)
    }

    @Test fun `empty single point and null gaps stay safe`() {
        assertThat(TidePresentation.from(emptyList())).isEmpty()

        val single = TidePresentation.from(hours(0.8)).values.single()
        assertThat(single.trend).isEqualTo(TideTrend.UNKNOWN)
        assertThat(single.weightTier).isEqualTo(0)

        val gap = TidePresentation.from(hours(1.0, null, 1.4))
        assertThat(gap[0L]!!.trend).isEqualTo(TideTrend.RISING)
        assertThat(gap[hourMs]!!.trend).isEqualTo(TideTrend.UNKNOWN)
        assertThat(gap[2L * hourMs]!!.trend).isEqualTo(TideTrend.RISING)
    }

    @Test fun `boundary plateaus mark one high or low`() {
        val highStart = TidePresentation.from(hours(2.0, 2.0, 2.0, 1.0))
        assertThat(highStart[0L]!!.trend).isEqualTo(TideTrend.HIGH)
        assertThat(highStart[hourMs]!!.trend).isEqualTo(TideTrend.FALLING)

        val lowStart = TidePresentation.from(hours(1.0, 1.0, 1.0, 2.0))
        assertThat(lowStart[0L]!!.trend).isEqualTo(TideTrend.LOW)
        assertThat(lowStart[hourMs]!!.trend).isEqualTo(TideTrend.RISING)

        val highEnd = TidePresentation.from(hours(1.0, 2.0, 2.0))
        assertThat(highEnd[2L * hourMs]!!.trend).isEqualTo(TideTrend.HIGH)
    }

    @Test fun `plateaus during rise or fall are not false extrema and flat peaks mark once`() {
        val risingStep = TidePresentation.from(hours(1.0, 2.0, 2.0, 3.0))
        assertThat(risingStep[1L * hourMs]!!.trend).isEqualTo(TideTrend.RISING)
        assertThat(risingStep[2L * hourMs]!!.trend).isEqualTo(TideTrend.RISING)

        val fallingStep = TidePresentation.from(hours(3.0, 2.0, 2.0, 1.0))
        assertThat(fallingStep[1L * hourMs]!!.trend).isEqualTo(TideTrend.FALLING)
        assertThat(fallingStep[2L * hourMs]!!.trend).isEqualTo(TideTrend.FALLING)

        val flatPeak = TidePresentation.from(hours(1.0, 2.0, 2.0, 1.0))
        assertThat(flatPeak[1L * hourMs]!!.trend).isEqualTo(TideTrend.HIGH)
        assertThat(flatPeak[2L * hourMs]!!.trend).isEqualTo(TideTrend.FALLING)

        val flatTrough = TidePresentation.from(hours(2.0, 1.0, 1.0, 2.0))
        assertThat(flatTrough[1L * hourMs]!!.trend).isEqualTo(TideTrend.LOW)
        assertThat(flatTrough[2L * hourMs]!!.trend).isEqualTo(TideTrend.RISING)
    }

    @Test fun `rate calculation uses actual elapsed hours between timestamps`() {
        val gapped = listOf(
            HourPoint(timeEpochMs = 0L, seaLevelM = 0.0),
            HourPoint(timeEpochMs = 2L * hourMs, seaLevelM = 0.4)
        )
        val result = TidePresentation.from(gapped)
        // 0.4m over 2 hours = 0.20 m/h -> tier 1 (not 0.40 m/h -> tier 2)
        assertThat(result[0L]!!.weightTier).isEqualTo(1)
        assertThat(result[2L * hourMs]!!.weightTier).isEqualTo(1)
    }
}
