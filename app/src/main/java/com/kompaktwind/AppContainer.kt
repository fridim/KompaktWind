package com.kompaktwind

import android.content.Context
import com.kompaktwind.data.DataSource
import com.kompaktwind.data.ForecastRepository
import com.kompaktwind.data.KompaktWindDatabase
import com.kompaktwind.data.LanguageManager
import com.kompaktwind.data.SettingsManager
import com.kompaktwind.data.catalog.SpotCatalogRepository
import com.kompaktwind.data.live.LiveStationRepository
import com.kompaktwind.data.live.MetarRepository
import com.kompaktwind.data.provider.ProviderRegistry
import com.kompaktwind.data.provider.geocoding.GeocodingService
import com.kompaktwind.data.provider.openmeteo.OpenMeteoProvider
import com.kompaktwind.util.NetworkMonitor
import kotlinx.coroutines.flow.first

class AppContainer(context: Context) {
    val contentResolver = context.contentResolver
    val db: KompaktWindDatabase = KompaktWindDatabase.getDatabase(context)
    val settings: SettingsManager = SettingsManager(context)
    val language: LanguageManager = LanguageManager(context)
    val networkMonitor: NetworkMonitor = NetworkMonitor(context)
    val geocoding: GeocodingService = GeocodingService.create()
    val liveStations: LiveStationRepository = LiveStationRepository.create()
    val metar: MetarRepository = MetarRepository.create()
    val catalog: SpotCatalogRepository = SpotCatalogRepository(context)
    val providers: ProviderRegistry = ProviderRegistry(
        DataSource.entries.map { source ->
            OpenMeteoProvider.create(
                id = source.providerId,
                displayName = source.label,
                attributionText = source.attribution.removePrefix("Data: "),
                models = source.modelId,
                maxDays = source.maxDays
            )
        }
    )
    val forecastRepository: ForecastRepository = ForecastRepository(
        spotDao = db.spotDao(),
        cacheDao = db.forecastCacheDao(),
        providers = providers,
        cacheTtlMs = {
            settings.flow.first().cacheTtlMinutes.coerceAtLeast(1) * 60_000L
        }
    )
}
