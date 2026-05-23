package com.kompaktwind

import android.content.Context
import com.kompaktwind.data.ForecastRepository
import com.kompaktwind.data.KompaktWindDatabase
import com.kompaktwind.data.SettingsManager
import com.kompaktwind.data.provider.ProviderRegistry
import com.kompaktwind.data.provider.geocoding.GeocodingService
import com.kompaktwind.data.provider.openmeteo.OpenMeteoProvider
import com.kompaktwind.util.NetworkMonitor

class AppContainer(context: Context) {
    val db: KompaktWindDatabase = KompaktWindDatabase.getDatabase(context)
    val settings: SettingsManager = SettingsManager(context)
    val networkMonitor: NetworkMonitor = NetworkMonitor(context)
    val geocoding: GeocodingService = GeocodingService.create()
    val providers: ProviderRegistry = ProviderRegistry(listOf(OpenMeteoProvider.create()))
    val forecastRepository: ForecastRepository = ForecastRepository(
        spotDao = db.spotDao(),
        cacheDao = db.forecastCacheDao(),
        providers = providers,
        cacheTtlMs = { 30 * 60 * 1000L }
    )
}
