package com.kompaktwind.ui.common

import com.google.common.truth.Truth.assertThat
import com.kompaktwind.data.TempUnit
import com.kompaktwind.data.WindUnit
import org.junit.Test

class UnitFormattersTest {
    @Test fun `wind ms to knots`() {
        assertThat(UnitFormatters.windFromMs(10.0, WindUnit.KN)).isWithin(0.05).of(19.44)
    }
    @Test fun `wind ms to kmh`() {
        assertThat(UnitFormatters.windFromMs(10.0, WindUnit.KMH)).isWithin(0.001).of(36.0)
    }
    @Test fun `wind ms to mph`() {
        assertThat(UnitFormatters.windFromMs(10.0, WindUnit.MPH)).isWithin(0.05).of(22.37)
    }
    @Test fun `temp c to f`() {
        assertThat(UnitFormatters.tempFromC(0.0, TempUnit.F)).isWithin(0.01).of(32.0)
        assertThat(UnitFormatters.tempFromC(-40.0, TempUnit.F)).isWithin(0.01).of(-40.0)
        assertThat(UnitFormatters.tempFromC(100.0, TempUnit.F)).isWithin(0.01).of(212.0)
    }
    @Test fun `degrees to cardinal`() {
        assertThat(UnitFormatters.cardinal(0)).isEqualTo("N")
        assertThat(UnitFormatters.cardinal(45)).isEqualTo("NE")
        assertThat(UnitFormatters.cardinal(180)).isEqualTo("S")
        assertThat(UnitFormatters.cardinal(225)).isEqualTo("SW")
        assertThat(UnitFormatters.cardinal(359)).isEqualTo("N")
    }
    @Test fun `degree arrow glyph rotates by 45 deg buckets`() {
        assertThat(UnitFormatters.arrowGlyph(10)).isEqualTo(UnitFormatters.arrowGlyph(20))
        assertThat(UnitFormatters.arrowGlyph(0)).isEqualTo("↓")
    }
    @Test fun `unit suffix matches enum`() {
        assertThat(UnitFormatters.windUnitLabel(WindUnit.MS)).isEqualTo("m/s")
        assertThat(UnitFormatters.windUnitLabel(WindUnit.KN)).isEqualTo("kn")
    }
}
