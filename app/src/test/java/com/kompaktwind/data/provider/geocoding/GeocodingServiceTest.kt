package com.kompaktwind.data.provider.geocoding

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import org.junit.Test

class GeocodingServiceTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test fun `geocoding response parses with multiple results`() {
        val raw = checkNotNull(this::class.java.classLoader!!
            .getResourceAsStream("openmeteo/geocoding_calais.json"))
            .bufferedReader().readText()
        val dto = json.decodeFromString<GeocodingResponseDto>(raw)
        assertThat(dto.results).hasSize(2)
        assertThat(dto.results!![0].country).isEqualTo("France")
    }

    @Test fun `response with no results parses with empty list`() {
        val dto = json.decodeFromString<GeocodingResponseDto>("{}")
        assertThat(dto.results).isNull()
    }
}
