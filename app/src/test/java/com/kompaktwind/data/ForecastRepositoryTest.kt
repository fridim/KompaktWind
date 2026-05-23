package com.kompaktwind.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.kompaktwind.data.provider.FakeWeatherProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ForecastRepositoryTest {
    private lateinit var db: KompaktWindDatabase
    private lateinit var provider: FakeWeatherProvider
    private lateinit var repo: ForecastRepository
    private val spot = Spot("spot1", "Calais", 50.95, 1.85, true, 0L)

    @Before fun setup() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, KompaktWindDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        provider = FakeWeatherProvider(id = "fake")
        repo = ForecastRepository(
            spotDao = db.spotDao(),
            cacheDao = db.forecastCacheDao(),
            providers = com.kompaktwind.data.provider.ProviderRegistry(listOf(provider)),
            now = { 10_000L },
            cacheTtlMs = { 5_000L }
        )
        runBlocking { db.spotDao().upsert(SpotEntity(spot.id, spot.name, spot.lat, spot.lon, spot.isCoastal, spot.createdAt)) }
    }

    @After fun teardown() = db.close()

    @Test fun `absent cache + success returns Data`() = runBlocking {
        provider.setNext(Result.success(forecast(fetchedAt = 10_000L)))
        val state = repo.getForecast(spot, "fake")
        assertThat(state).isInstanceOf(ForecastUiState.Data::class.java)
        assertThat((state as ForecastUiState.Data).isStale).isFalse()
    }

    @Test fun `fresh cache hit does not call provider`() = runBlocking {
        db.forecastCacheDao().upsert(cacheEntity(fetchedAt = 8_000L))
        val state = repo.getForecast(spot, "fake")
        assertThat(provider.calls).isEqualTo(0)
        assertThat(state).isInstanceOf(ForecastUiState.Data::class.java)
    }

    @Test fun `stale cache + network success returns fresh`() = runBlocking {
        db.forecastCacheDao().upsert(cacheEntity(fetchedAt = 1_000L))
        provider.setNext(Result.success(forecast(fetchedAt = 10_000L)))
        val state = repo.getForecast(spot, "fake") as ForecastUiState.Data
        assertThat(state.fetchedAt).isEqualTo(10_000L)
        assertThat(state.isStale).isFalse()
    }

    @Test fun `stale cache + failure returns stale data`() = runBlocking {
        db.forecastCacheDao().upsert(cacheEntity(fetchedAt = 1_000L))
        provider.setNext(Result.failure(RuntimeException("boom")))
        val state = repo.getForecast(spot, "fake") as ForecastUiState.Data
        assertThat(state.isStale).isTrue()
        assertThat(state.fetchedAt).isEqualTo(1_000L)
    }

    @Test fun `absent cache + failure returns Error`() = runBlocking {
        provider.setNext(Result.failure(RuntimeException("no net")))
        val state = repo.getForecast(spot, "fake")
        assertThat(state).isInstanceOf(ForecastUiState.Error::class.java)
        assertThat((state as ForecastUiState.Error).cached).isNull()
    }

    @Test fun `disallow caching provider skips cache write`() = runBlocking {
        val nocache = FakeWeatherProvider(id = "windy", disallowCaching = true)
        nocache.setNext(Result.success(forecast(fetchedAt = 10_000L).copy(providerId = "windy")))
        val r2 = ForecastRepository(
            spotDao = db.spotDao(),
            cacheDao = db.forecastCacheDao(),
            providers = com.kompaktwind.data.provider.ProviderRegistry(listOf(nocache)),
            now = { 10_000L },
            cacheTtlMs = { 5_000L }
        )
        r2.getForecast(spot, "windy")
        assertThat(db.forecastCacheDao().get(spot.id, "windy")).isNull()
    }

    @Test fun `forceRefresh ignores fresh cache`() = runBlocking {
        db.forecastCacheDao().upsert(cacheEntity(fetchedAt = 8_000L))
        provider.setNext(Result.success(forecast(fetchedAt = 10_000L)))
        repo.getForecast(spot, "fake", forceRefresh = true)
        assertThat(provider.calls).isEqualTo(1)
    }

    private fun forecast(fetchedAt: Long) = Forecast(
        spotId = "", providerId = "fake", fetchedAt = fetchedAt,
        timezone = "UTC", hours = listOf(HourPoint(timeEpochMs = fetchedAt))
    )

    private fun cacheEntity(fetchedAt: Long) = ForecastCacheEntity(
        spotId = spot.id, providerId = "fake", fetchedAt = fetchedAt,
        timezone = "UTC", hoursJson = HourPointJson.encode(listOf(HourPoint(timeEpochMs = fetchedAt)))
    )
}
