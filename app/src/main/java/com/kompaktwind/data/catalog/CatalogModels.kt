package com.kompaktwind.data.catalog

import kotlinx.serialization.Serializable

@Serializable
data class CatalogManifest(
    val version: Int = 1,
    val generatedAt: String = "",
    val spots: Int = 0
)

@Serializable
data class CatalogSource(
    val name: String = "",
    val url: String = ""
)

@Serializable
data class CatalogSpot(
    val id: String,
    val name: String,
    val shortName: String = "",
    val altNames: List<String> = emptyList(),
    val lat: Double,
    val lon: Double,
    val country: String? = null,
    val region: String? = null,
    val description: String = "",
    val windDirections: List<String> = emptyList(),
    val avoidWindDirections: List<String> = emptyList(),
    val activities: List<String> = emptyList(),
    val source: String = "",
    val sourceUrl: String = "",
    val sources: List<CatalogSource> = emptyList(),
    val tile: String = "",
    val updated: String = "",
    val local: Boolean = false,
    val hintName: String = "",
    val hintKind: String = "",
    val hintOsm: String = ""
)

val ALL_SOURCES = setOf(
    "openstreetmap.org",
)

fun CatalogSpot.hasEnabledSource(enabled: Set<String>): Boolean {
    if (local) return true
    val names = sources.map { it.name }.ifEmpty { listOf(source) }
    return names.any { it in enabled }
}

fun CatalogSpot.coordinateLabel(): String =
    String.format(java.util.Locale.US, "%.4f, %.4f", lat, lon)

fun CatalogSpot.displayName(): String =
    name.trim()
        .ifBlank { shortName.trim() }
        .ifBlank { hintName.trim() }
        .ifBlank { coordinateLabel() }

fun CatalogSpot.dedupeKey(): String =
    name.trim().lowercase(java.util.Locale.ROOT) + "|" +
        String.format(java.util.Locale.US, "%.5f,%.5f", lat, lon)

private val COASTAL_ACTIVITIES = setOf("kitesurfing", "windsurfing", "wing", "surf", "kite")

private val INLAND_WORDS = setOf(
    "lac", "lake", "étang", "etang", "lagune", "lagon",
    "rivière", "riviere", "fleuve", "gravière", "graviere"
)

private val NON_LETTER_RUN = Regex("[^\\p{L}]+")

fun CatalogSpot.likelyCoastal(): Boolean =
    activities.any { it.lowercase(java.util.Locale.ROOT) in COASTAL_ACTIVITIES } ||
        name.lowercase(java.util.Locale.ROOT)
            .split(NON_LETTER_RUN)
            .none { it in INLAND_WORDS }

sealed interface CatalogState {
    data object Loading : CatalogState
    data class Data(val spots: List<CatalogSpot>) : CatalogState
    data class Error(val message: String) : CatalogState
}
