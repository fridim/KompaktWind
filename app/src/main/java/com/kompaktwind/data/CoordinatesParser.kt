package com.kompaktwind.data

data class LatLon(val lat: Double, val lon: Double)

object CoordinatesParser {
    private val token = Regex("""^([NSEW])?([+-]?\d+(?:\.\d+)?)([NSEW])?$""", RegexOption.IGNORE_CASE)

    fun parse(input: String): LatLon? {
        val normalized = input
            .replace('°', ' ')
            .replace('º', ' ')
            .replace('′', ' ')
            .replace('″', ' ')
            .replace('\'', ' ')
            .replace('"', ' ')
            .replace(',', ' ')
            .replace(';', ' ')
            .trim()
        if (normalized.isEmpty()) return null

        val coords = mutableListOf<Coord>()
        var pendingHemisphere: Char? = null
        for (raw in normalized.split(Regex("\\s+")).filter { it.isNotBlank() }) {
            val match = token.matchEntire(raw)
            if (match == null) {
                if (raw.length == 1 && raw.first().uppercaseChar() in "NSEW") {
                    val hemisphere = raw.first().uppercaseChar()
                    val current = coords.lastOrNull()
                    if (current != null && current.hemisphere == null && current.numbers.isNotEmpty()) {
                        current.hemisphere = hemisphere
                    } else {
                        pendingHemisphere = hemisphere
                    }
                    continue
                }
                return null
            }
            val prefix = match.groupValues[1].firstOrNull()?.uppercaseChar()
            val number = match.groupValues[2].toDoubleOrNull() ?: return null
            val suffix = match.groupValues[3].firstOrNull()?.uppercaseChar()
            val hemisphere = suffix ?: prefix ?: pendingHemisphere
            pendingHemisphere = null

            val current = coords.lastOrNull()
            val startsNew = current == null ||
                (hemisphere != null && current.hemisphere != null) ||
                (number < 0 && current.numbers.isNotEmpty()) ||
                (current.numbers.size >= 3) ||
                (current.hemisphere == null && current.numbers.size == 1 &&
                    current.numbers[0].toString().contains('.') && raw.contains('.') && number > 0)

            if (startsNew) {
                coords += Coord(hemisphere = hemisphere, numbers = mutableListOf(number))
            } else {
                if (hemisphere != null && current!!.hemisphere == null) current.hemisphere = hemisphere
                current!!.numbers += number
            }
        }

        if (coords.size != 2) return null
        val lat = coords[0].value() ?: return null
        val lon = coords[1].value() ?: return null
        if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return null
        return LatLon(lat, lon)
    }

    private data class Coord(var hemisphere: Char?, val numbers: MutableList<Double>) {
        fun value(): Double? {
            if (numbers.isEmpty() || numbers.size > 3) return null
            val degrees = numbers[0]
            val minutes = numbers.getOrNull(1) ?: 0.0
            val seconds = numbers.getOrNull(2) ?: 0.0
            if (minutes !in 0.0..59.999 || seconds !in 0.0..59.999) return null
            val magnitude = kotlin.math.abs(degrees) + minutes / 60.0 + seconds / 3600.0
            val negative = hemisphere == 'S' || hemisphere == 'W' || degrees < 0
            return if (negative) -magnitude else magnitude
        }
    }
}
