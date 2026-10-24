package com.kompaktwind.data

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DataSourceTest {
    @Test fun `ids and labels map to the expected source`() {
        assertThat(DataSource.fromId(ProviderIds.OPEN_METEO)).isEqualTo(DataSource.OPEN_METEO)
        assertThat(DataSource.fromId(ProviderIds.AROME_FR)).isEqualTo(DataSource.AROME_FR)
        assertThat(DataSource.fromLabel("AROME FR HD")).isEqualTo(DataSource.AROME_FR)
        assertThat(DataSource.fromId("unknown")).isEqualTo(DataSource.OPEN_METEO)
        assertThat(DataSource.entries.first()).isEqualTo(DataSource.OPEN_METEO)
    }

    @Test fun `regional models are gated by their domains`() {
        assertThat(DataSource.AROME_FR.isAvailableAt(48.39, -4.49)).isTrue()
        assertThat(DataSource.AROME_FR.isAvailableAt(4.9, -52.3)).isFalse()
        assertThat(DataSource.availableAt(48.39, -4.49)).containsAtLeast(
            DataSource.OPEN_METEO,
            DataSource.AROME_FR,
            DataSource.AROME_FR_2_5,
            DataSource.KNMI_HARMONIE,
            DataSource.DMI_HARMONIE,
            DataSource.UKMO_UKV,
            DataSource.ICON_EU,
            DataSource.ARPEGE_EUROPE,
            DataSource.ECMWF_IFS
        )
        assertThat(DataSource.availableAt(4.9, -52.3)).containsExactly(
            DataSource.OPEN_METEO,
            DataSource.ECMWF_IFS,
            DataSource.GFS,
            DataSource.GEM,
            DataSource.UKMO_GLOBAL,
            DataSource.ARPEGE_WORLD,
            DataSource.ICON_GLOBAL
        )
    }

    @Test fun `description includes agency resolution and horizon`() {
        assertThat(DataSource.ICON_EU.description).isEqualTo("DWD · 7 km · ~5 d")
        assertThat(DataSource.ICON_EU.shortLabel).isEqualTo("ICON-EU")
        assertThat(DataSource.AROME_FR.description).contains("1.5 km")
        assertThat(DataSource.AROME_FR.description).contains("~2 d")
        assertThat(DataSource.AROME_FR.country).isEqualTo("France")
        assertThat(DataSource.ICON_EU.details).contains("Germany")
        assertThat(DataSource.ECMWF_IFS.country).isEqualTo("Europe")
    }

    @Test fun `unavailable saved source falls back to open meteo`() {
        assertThat(DataSource.effectiveFor(ProviderIds.AROME_FR, 4.9, -52.3))
            .isEqualTo(DataSource.OPEN_METEO)
        assertThat(DataSource.effectiveFor(ProviderIds.AROME_FR, 48.39, -4.49))
            .isEqualTo(DataSource.AROME_FR)
    }
}
