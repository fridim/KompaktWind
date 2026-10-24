package com.kompaktwind.ui.common

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.kompaktwind.data.TempUnit
import com.kompaktwind.data.DistanceUnit
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

    fun distanceFromKm(km: Double, unit: DistanceUnit): Double = when (unit) {
        DistanceUnit.KM -> km
        DistanceUnit.MI -> km * 0.621371
    }

    fun distanceUnitLabel(unit: DistanceUnit): String = when (unit) {
        DistanceUnit.KM -> "km"
        DistanceUnit.MI -> "mi"
    }

    fun distanceLabel(km: Double, unit: DistanceUnit): String =
        "%.1f %s".format(distanceFromKm(km, unit), distanceUnitLabel(unit))

    fun windValueText(value: Double?): AnnotatedString {
        if (value == null) return AnnotatedString("—")
        val digits = "%.0f".format(value)
        if (digits.length >= 2) return AnnotatedString(digits)
        return buildAnnotatedString {
            withStyle(SpanStyle(color = Color.Transparent)) { append('0') }
            append(digits)
        }
    }
}
