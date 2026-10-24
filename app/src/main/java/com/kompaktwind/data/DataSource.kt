package com.kompaktwind.data

object ProviderIds {
    const val OPEN_METEO = "open-meteo"
    const val AROME_FR = "arome-fr"
    const val AROME_FR_2_5 = "arome-fr-2-5"
    const val KNMI_HARMONIE = "knmi-harmonie"
    const val DMI_HARMONIE = "dmi-harmonie"
    const val UKMO_UKV = "ukmo-ukv"
    const val ICON_D2 = "icon-d2"
    const val ICON_EU = "icon-eu"
    const val ARPEGE_EUROPE = "arpege-europe"
    const val METEOCH_SWISS_CH2 = "meteoswiss-ch2"
    const val METEOCH_SWISS_CH1 = "meteoswiss-ch1"
    const val ECMWF_IFS = "ecmwf-ifs"
    const val GFS = "gfs"
    const val GEM = "gem"
    const val UKMO_GLOBAL = "ukmo-global"
    const val ARPEGE_WORLD = "arpege-world"
    const val ICON_GLOBAL = "icon-global"
}

data class GeoBounds(
    val south: Double,
    val west: Double,
    val north: Double,
    val east: Double
) {
    fun contains(lat: Double, lon: Double): Boolean =
        lat in south..north && lon in west..east
}

// Entries are deliberately ordered by relevance for a France-focused wind app.
// `availableAt` preserves this order, so the nearest/most useful models appear
// first in the selector.
enum class DataSource(
    val providerId: String,
    val modelId: String?,
    val label: String,
    val shortLabel: String,
    val agency: String,
    val resolution: String,
    val horizon: String,
    val maxDays: Int,
    val attribution: String,
    val bounds: GeoBounds
) {
    OPEN_METEO(
        providerId = ProviderIds.OPEN_METEO,
        modelId = null,
        label = "OpenMeteo",
        shortLabel = "Best",
        agency = "Open-Meteo",
        resolution = "auto",
        horizon = "up to 7 d",
        maxDays = 7,
        attribution = "Data: Open-Meteo",
        bounds = GeoBounds(-90.0, -180.0, 90.0, 180.0)
    ),
    AROME_FR(
        providerId = ProviderIds.AROME_FR,
        modelId = "meteofrance_arome_france_hd",
        label = "AROME FR HD",
        shortLabel = "AROME 1.5",
        agency = "Météo-France",
        resolution = "1.5 km",
        horizon = "~2 d",
        maxDays = 3,
        attribution = "Data: Météo-France AROME via Open-Meteo",
        bounds = GeoBounds(37.5, -12.0, 55.4, 16.0)
    ),
    AROME_FR_2_5(
        providerId = ProviderIds.AROME_FR_2_5,
        modelId = "meteofrance_arome_france0025",
        label = "AROME FR 2.5",
        shortLabel = "AROME 2.5",
        agency = "Météo-France",
        resolution = "2.5 km",
        horizon = "~2 d",
        maxDays = 3,
        attribution = "Data: Météo-France AROME via Open-Meteo",
        bounds = GeoBounds(37.5, -12.0, 55.4, 16.0)
    ),
    KNMI_HARMONIE(
        providerId = ProviderIds.KNMI_HARMONIE,
        modelId = "knmi_harmonie_arome_europe",
        label = "KNMI HARMONIE",
        shortLabel = "KNMI",
        agency = "KNMI",
        resolution = "2 km",
        horizon = "~2.5 d",
        maxDays = 3,
        attribution = "Data: KNMI HARMONIE via Open-Meteo",
        bounds = GeoBounds(39.74063, -25.162262, 62.619324, 38.75702)
    ),
    DMI_HARMONIE(
        providerId = ProviderIds.DMI_HARMONIE,
        modelId = "dmi_harmonie_arome_europe",
        label = "DMI HARMONIE",
        shortLabel = "DMI",
        agency = "DMI",
        resolution = "2 km",
        horizon = "~2.3 d",
        maxDays = 3,
        attribution = "Data: DMI HARMONIE via Open-Meteo",
        bounds = GeoBounds(39.67, -25.42, 62.67, 40.07)
    ),
    UKMO_UKV(
        providerId = ProviderIds.UKMO_UKV,
        modelId = "ukmo_uk_deterministic_2km",
        label = "UKMO UKV",
        shortLabel = "UKV",
        agency = "Met Office",
        resolution = "2 km",
        horizon = "~2 d",
        maxDays = 3,
        attribution = "Data: UK Met Office via Open-Meteo",
        bounds = GeoBounds(44.51, -17.15, 61.93, 15.35)
    ),
    ICON_D2(
        providerId = ProviderIds.ICON_D2,
        modelId = "dwd_icon_d2",
        label = "ICON D2",
        shortLabel = "ICON-D2",
        agency = "DWD",
        resolution = "2.2 km",
        horizon = "~2 d",
        maxDays = 3,
        attribution = "Data: DWD ICON via Open-Meteo",
        bounds = GeoBounds(43.18, -3.94, 58.08, 20.34)
    ),
    ICON_EU(
        providerId = ProviderIds.ICON_EU,
        modelId = "dwd_icon_eu",
        label = "ICON EU",
        shortLabel = "ICON-EU",
        agency = "DWD",
        resolution = "7 km",
        horizon = "~5 d",
        maxDays = 5,
        attribution = "Data: DWD ICON via Open-Meteo",
        bounds = GeoBounds(29.5, -23.5, 70.5, 62.5)
    ),
    ARPEGE_EUROPE(
        providerId = ProviderIds.ARPEGE_EUROPE,
        modelId = "meteofrance_arpege_europe",
        label = "ARPEGE Europe",
        shortLabel = "ARPEGE EU",
        agency = "Météo-France",
        resolution = "11 km",
        horizon = "~4 d",
        maxDays = 4,
        attribution = "Data: Météo-France ARPEGE via Open-Meteo",
        bounds = GeoBounds(20.0, -32.0, 72.0, 42.0)
    ),
    METEOCH_SWISS_CH2(
        providerId = ProviderIds.METEOCH_SWISS_CH2,
        modelId = "meteoswiss_icon_ch2",
        label = "MeteoSwiss CH2",
        shortLabel = "CH2",
        agency = "MeteoSwiss",
        resolution = "2 km",
        horizon = "~5 d",
        maxDays = 5,
        attribution = "Data: MeteoSwiss ICON via Open-Meteo",
        bounds = GeoBounds(42.58, 1.23, 49.79, 16.85)
    ),
    METEOCH_SWISS_CH1(
        providerId = ProviderIds.METEOCH_SWISS_CH1,
        modelId = "meteoswiss_icon_ch1",
        label = "MeteoSwiss CH1",
        shortLabel = "CH1",
        agency = "MeteoSwiss",
        resolution = "1 km",
        horizon = "~1.2 d",
        maxDays = 2,
        attribution = "Data: MeteoSwiss ICON via Open-Meteo",
        bounds = GeoBounds(42.58, 1.23, 49.79, 16.85)
    ),
    ECMWF_IFS(
        providerId = ProviderIds.ECMWF_IFS,
        modelId = "ecmwf_ifs",
        label = "ECMWF IFS",
        shortLabel = "ECMWF",
        agency = "ECMWF",
        resolution = "9 km",
        horizon = "up to 7 d",
        maxDays = 7,
        attribution = "Data: ECMWF IFS via Open-Meteo",
        bounds = GeoBounds(-90.0, -180.0, 90.0, 180.0)
    ),
    GFS(
        providerId = ProviderIds.GFS,
        modelId = "ncep_gfs_global",
        label = "GFS",
        shortLabel = "GFS",
        agency = "NOAA",
        resolution = "13 km",
        horizon = "up to 7 d",
        maxDays = 7,
        attribution = "Data: NOAA GFS via Open-Meteo",
        bounds = GeoBounds(-90.0, -180.0, 90.0, 180.0)
    ),
    GEM(
        providerId = ProviderIds.GEM,
        modelId = "gem_global",
        label = "GEM",
        shortLabel = "GEM",
        agency = "ECCC",
        resolution = "~15 km",
        horizon = "up to 7 d",
        maxDays = 7,
        attribution = "Data: ECCC GEM via Open-Meteo",
        bounds = GeoBounds(-90.0, -180.0, 90.0, 180.0)
    ),
    UKMO_GLOBAL(
        providerId = ProviderIds.UKMO_GLOBAL,
        modelId = "ukmo_global_deterministic_10km",
        label = "UKMO Global",
        shortLabel = "UKMO",
        agency = "Met Office",
        resolution = "10 km",
        horizon = "up to 7 d",
        maxDays = 7,
        attribution = "Data: UK Met Office via Open-Meteo",
        bounds = GeoBounds(-90.0, -180.0, 90.0, 180.0)
    ),
    ARPEGE_WORLD(
        providerId = ProviderIds.ARPEGE_WORLD,
        modelId = "meteofrance_arpege_world025",
        label = "ARPEGE World",
        shortLabel = "ARPEGE W",
        agency = "Météo-France",
        resolution = "25 km",
        horizon = "~4 d",
        maxDays = 4,
        attribution = "Data: Météo-France ARPEGE via Open-Meteo",
        bounds = GeoBounds(-90.0, -180.0, 90.0, 180.0)
    ),
    ICON_GLOBAL(
        providerId = ProviderIds.ICON_GLOBAL,
        modelId = "dwd_icon",
        label = "ICON Global",
        shortLabel = "ICON",
        agency = "DWD",
        resolution = "11 km",
        horizon = "~5 d",
        maxDays = 5,
        attribution = "Data: DWD ICON via Open-Meteo",
        bounds = GeoBounds(-90.0, -180.0, 90.0, 180.0)
    );

    val description: String
        get() = "$agency · $resolution · $horizon"

    val country: String
        get() = when (agency) {
            "Météo-France" -> "France"
            "KNMI" -> "Netherlands"
            "DMI" -> "Denmark"
            "Met Office" -> "United Kingdom"
            "DWD" -> "Germany"
            "MeteoSwiss" -> "Switzerland"
            "ECMWF" -> "Europe"
            "NOAA" -> "USA"
            "ECCC" -> "Canada"
            "Open-Meteo" -> "Global"
            else -> "—"
        }

    val details: String
        get() = "$agency · $country · $resolution · $horizon"

    fun isAvailableAt(lat: Double, lon: Double): Boolean = bounds.contains(lat, lon)

    companion object {
        fun fromId(id: String): DataSource =
            entries.firstOrNull { it.providerId == id } ?: OPEN_METEO

        fun fromLabel(label: String): DataSource =
            entries.firstOrNull { it.label == label } ?: OPEN_METEO

        fun availableAt(lat: Double, lon: Double): List<DataSource> =
            entries.filter { it.isAvailableAt(lat, lon) }

        fun effectiveFor(providerId: String, lat: Double, lon: Double): DataSource {
            val saved = fromId(providerId)
            return if (saved.isAvailableAt(lat, lon)) saved else OPEN_METEO
        }
    }
}
