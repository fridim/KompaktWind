package com.kompaktwind.data

data class Spot(
    val id: String,
    val name: String,
    val lat: Double,
    val lon: Double,
    val isCoastal: Boolean,
    val createdAt: Long
)

data class Forecast(
    val spotId: String,
    val providerId: String,
    val fetchedAt: Long,
    val timezone: String,
    val hours: List<HourPoint>
)

data class HourPoint(
    val timeEpochMs: Long,
    val tempC: Double? = null,
    val windSpeedMs: Double? = null,
    val windGustMs: Double? = null,
    val windDirDeg: Int? = null,
    val precipMm: Double? = null,
    val precipProbability: Int? = null,
    val waveHeightM: Double? = null,
    val wavePeriodS: Double? = null,
    val waterTempC: Double? = null
)

enum class WindUnit { MS, KMH, KN, MPH }
enum class TempUnit { C, F }
enum class MarineDisplay { AUTO, ALWAYS, NEVER }
