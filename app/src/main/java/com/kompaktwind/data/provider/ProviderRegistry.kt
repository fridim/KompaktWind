package com.kompaktwind.data.provider

class ProviderRegistry(private val providers: List<WeatherProvider>) {
    init {
        require(providers.isNotEmpty()) { "ProviderRegistry needs at least one provider" }
        require(providers.map { it.id }.toSet().size == providers.size) {
            "ProviderRegistry provider ids must be unique"
        }
    }

    val default: WeatherProvider get() = providers.first()
    val all: List<WeatherProvider> get() = providers

    fun get(id: String): WeatherProvider? = providers.firstOrNull { it.id == id }
}
