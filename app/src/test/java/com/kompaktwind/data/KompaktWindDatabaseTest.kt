package com.kompaktwind.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class KompaktWindDatabaseTest {
    private lateinit var db: KompaktWindDatabase

    @Before fun setup() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, KompaktWindDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After fun teardown() { db.close() }

    @Test fun `insert spot and observe`() = runBlocking {
        val spot = SpotEntity("id1", "Calais", 50.95, 1.85, true, 0L)
        db.spotDao().upsert(spot)
        val all = db.spotDao().observeAll().first()
        assertThat(all).hasSize(1)
        assertThat(all[0].name).isEqualTo("Calais")
    }

    @Test fun `cache upsert replaces existing row`() = runBlocking {
        db.spotDao().upsert(SpotEntity("id1", "Calais", 50.95, 1.85, true, 0L))
        db.forecastCacheDao().upsert(ForecastCacheEntity("id1", "open-meteo", 1L, "Europe/Paris", "[]"))
        db.forecastCacheDao().upsert(ForecastCacheEntity("id1", "open-meteo", 2L, "Europe/Paris", "[]"))
        val row = db.forecastCacheDao().get("id1", "open-meteo")
        assertThat(row?.fetchedAt).isEqualTo(2L)
    }

    @Test fun `deleting spot cascades cache rows`() = runBlocking {
        db.spotDao().upsert(SpotEntity("id1", "Calais", 50.95, 1.85, true, 0L))
        db.forecastCacheDao().upsert(ForecastCacheEntity("id1", "open-meteo", 1L, "Europe/Paris", "[]"))
        db.spotDao().delete("id1")
        assertThat(db.forecastCacheDao().get("id1", "open-meteo")).isNull()
    }
}
