package com.kompaktwind.data.provider

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ProviderRegistryTest {
    @Test fun `registry returns provider by id`() {
        val a = FakeWeatherProvider(id = "open-meteo")
        val b = FakeWeatherProvider(id = "windy")
        val registry = ProviderRegistry(listOf(a, b))
        assertThat(registry.get("windy")).isSameInstanceAs(b)
    }

    @Test fun `registry default is the first registered provider`() {
        val a = FakeWeatherProvider(id = "open-meteo")
        val b = FakeWeatherProvider(id = "windy")
        val registry = ProviderRegistry(listOf(a, b))
        assertThat(registry.default).isSameInstanceAs(a)
    }

    @Test fun `get unknown id returns null`() {
        val a = FakeWeatherProvider(id = "open-meteo")
        val registry = ProviderRegistry(listOf(a))
        assertThat(registry.get("nope")).isNull()
    }
}
