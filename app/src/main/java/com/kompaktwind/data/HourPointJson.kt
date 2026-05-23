package com.kompaktwind.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

@Serializable
private data class HourPointSerializable(
    @SerialName("t") val t: Long,
    @SerialName("tC") val tC: Double? = null,
    @SerialName("wMs") val wMs: Double? = null,
    @SerialName("gMs") val gMs: Double? = null,
    @SerialName("wD") val wD: Int? = null,
    @SerialName("pMm") val pMm: Double? = null,
    @SerialName("pP") val pP: Int? = null,
    @SerialName("waveH") val waveH: Double? = null,
    @SerialName("waveT") val waveT: Double? = null,
    @SerialName("waterC") val waterC: Double? = null
) {
    fun toDomain() = HourPoint(t, tC, wMs, gMs, wD, pMm, pP, waveH, waveT, waterC)
    companion object {
        fun from(h: HourPoint) = HourPointSerializable(
            h.timeEpochMs, h.tempC, h.windSpeedMs, h.windGustMs, h.windDirDeg,
            h.precipMm, h.precipProbability, h.waveHeightM, h.wavePeriodS, h.waterTempC
        )
    }
}

object HourPointJson {
    private val json = Json { ignoreUnknownKeys = true }
    fun encode(hours: List<HourPoint>): String =
        json.encodeToString(ListSerializer(HourPointSerializable.serializer()),
            hours.map { HourPointSerializable.from(it) })
    fun decode(s: String): List<HourPoint> =
        json.decodeFromString(ListSerializer(HourPointSerializable.serializer()), s)
            .map { it.toDomain() }
}
