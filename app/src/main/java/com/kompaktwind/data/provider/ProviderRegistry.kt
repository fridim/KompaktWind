package com.kompaktwind.data.provider

class ProviderRegistry(private val providers: List<WeatherProvider>) {
    init { require(providers.isNotEmpty()) { "ProviderRegistry needs at least one provider" } }

    val default: WeatherProvider get() = providers.first()
    val all: List<WeatherProvider> get() = providers

    fun get(id: String): WeatherProvider? = providers.firstOrNull { it.id == id }
}
