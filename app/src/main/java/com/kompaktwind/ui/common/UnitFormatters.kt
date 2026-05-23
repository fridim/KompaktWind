package com.kompaktwind.ui.common

import com.kompaktwind.data.TempUnit
import com.kompaktwind.data.WindUnit

object UnitFormatters {

    fun windFromMs(ms: Double, target: WindUnit): Double = when (target) {
        WindUnit.MS -> ms
        WindUnit.KMH -> ms * 3.6
        WindUnit.KN -> ms * 1.94384
        WindUnit.MPH -> ms * 2.23694
    }

    fun tempFromC(c: Double, target: TempUnit): Double = when (target) {
        TempUnit.C -> c
        TempUnit.F -> c * 9.0 / 5.0 + 32.0
    }

    fun windUnitLabel(u: WindUnit): String = when (u) {
        WindUnit.MS -> "m/s"
        WindUnit.KMH -> "km/h"
        WindUnit.KN -> "kn"
        WindUnit.MPH -> "mph"
    }

    fun tempUnitLabel(u: TempUnit): String = when (u) {
        TempUnit.C -> "°C"
        TempUnit.F -> "°F"
    }

    private val cardinals = listOf(
        "N", "NE", "E", "SE", "S", "SW", "W", "NW"
    )

    fun cardinal(deg: Int): String {
        val norm = ((deg % 360) + 360) % 360
        val idx = ((norm + 22) / 45) % 8
        return cardinals[idx]
    }

    fun arrowGlyph(degFrom: Int): String {
        val norm = ((degFrom + 180) % 360 + 360) % 360
        val idx = ((norm + 22) / 45) % 8
        return listOf("↑", "↗", "→", "↘", "↓", "↙", "←", "↖")[idx]
    }
}
