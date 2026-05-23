package com.kompaktwind.data

import com.kompaktwind.data.provider.ProviderRegistry

sealed interface ForecastUiState {
    data object Loading : ForecastUiState
    data class Data(
        val forecast: Forecast,
        val fetchedAt: Long,
        val isStale: Boolean
    ) : ForecastUiState
    data class Error(
        val kind: ErrorKind,
        val message: String,
        val cached: Data?
    ) : ForecastUiState

    enum class ErrorKind { NETWORK, RATE_LIMIT, PROVIDER_5XX, UNKNOWN }
}

class ForecastRepository(
    private val spotDao: SpotDao,
    private val cacheDao: ForecastCacheDao,
    private val providers: ProviderRegistry,
    private val now: () -> Long = { System.currentTimeMillis() },
    private val cacheTtlMs: () -> Long = { 30 * 60 * 1000L }
) {

    suspend fun getForecast(
        spot: Spot,
        providerId: String,
        forceRefresh: Boolean = false
    ): ForecastUiState {
        val provider = providers.get(providerId)
            ?: providers.default
        val cached = cacheDao.get(spot.id, provider.id)?.toData(spot)

        if (!forceRefresh && cached != null && (now() - cached.fetchedAt) < cacheTtlMs()) {
            return cached.copy(isStale = false)
        }

        val wantMarine = spot.isCoastal
        val result = provider.fetchForecast(spot.lat, spot.lon, wantMarine = wantMarine)
        return result.fold(
            onSuccess = { f ->
                val withSpot = f.copy(spotId = spot.id, providerId = provider.id)
                if (!provider.disallowCaching) {
                    cacheDao.upsert(withSpot.toEntity())
                }
                ForecastUiState.Data(withSpot, withSpot.fetchedAt, isStale = false)
            },
            onFailure = { t ->
                val kind = classify(t)
                cached?.let { ForecastUiState.Data(it.forecast, it.fetchedAt, isStale = true) }
                    ?: ForecastUiState.Error(kind, t.message ?: "Unknown error", cached = null)
            }
        )
    }

    @Suppress("UNUSED_PARAMETER")
    private fun ForecastCacheEntity.toData(spot: Spot) = ForecastUiState.Data(
        forecast = Forecast(
            spotId = spotId, providerId = providerId, fetchedAt = fetchedAt,
            timezone = timezone, hours = HourPointJson.decode(hoursJson)
        ),
        fetchedAt = fetchedAt,
        isStale = false
    )

    private fun Forecast.toEntity() = ForecastCacheEntity(
        spotId = spotId, providerId = providerId, fetchedAt = fetchedAt,
        timezone = timezone, hoursJson = HourPointJson.encode(hours)
    )

    private fun classify(t: Throwable): ForecastUiState.ErrorKind = when {
        t is java.net.UnknownHostException || t is java.net.SocketTimeoutException ->
            ForecastUiState.ErrorKind.NETWORK
        t.message?.contains("429") == true -> ForecastUiState.ErrorKind.RATE_LIMIT
        t.message?.contains("5") == true -> ForecastUiState.ErrorKind.PROVIDER_5XX
        else -> ForecastUiState.ErrorKind.UNKNOWN
    }
}
