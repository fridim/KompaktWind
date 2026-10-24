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

    @Test fun `catalog link and short name round trip`() = runBlocking {
        db.spotDao().upsert(
            SpotEntity("id2", "Praia de Santa Maria", 16.6, -22.9, true, 0L, catalogId = "osm-way-77221296", shortName = "Praia de Santa")
        )
        val row = db.spotDao().get("id2")
        assertThat(row?.catalogId).isEqualTo("osm-way-77221296")
        assertThat(row?.shortName).isEqualTo("Praia de Santa")
    }

    @Test fun `cache upsert replaces existing row`() = runBlocking {
        db.spotDao().upsert(SpotEntity("id1", "Calais", 50.95, 1.85, true, 0L))
        db.forecastCacheDao().upsert(ForecastCacheEntity("id1", "open-meteo", 1L, "Europe/Paris", "[]"))
        db.forecastCacheDao().upsert(ForecastCacheEntity("id1", "open-meteo", 2L, "Europe/Paris", "[]"))
        val row = db.forecastCacheDao().get("id1", "open-meteo")
        assertThat(row?.fetchedAt).isEqualTo(2L)
    }

    @Test fun `spot source defaults to open meteo and can be updated`() = runBlocking {
        db.spotDao().upsert(SpotEntity("id2", "Brest", 48.39, -4.49, true, 0L))
        assertThat(db.spotDao().get("id2")?.providerId).isEqualTo(ProviderIds.OPEN_METEO)

        db.spotDao().updateProvider("id2", ProviderIds.AROME_FR)
        assertThat(db.spotDao().get("id2")?.providerId).isEqualTo(ProviderIds.AROME_FR)
    }

    @Test fun `moving a spot swaps its position and persists`() = runBlocking {
        db.spotDao().upsert(SpotEntity("a", "A", 1.0, 1.0, true, 1L, sortOrder = 1L))
        db.spotDao().upsert(SpotEntity("b", "B", 2.0, 2.0, true, 2L, sortOrder = 2L))
        db.spotDao().upsert(SpotEntity("c", "C", 3.0, 3.0, true, 3L, sortOrder = 3L))

        assertThat(db.spotDao().move("b", -1)).isTrue()
        assertThat(db.spotDao().observeAll().first().map { it.id })
            .containsExactly("b", "a", "c").inOrder()

        assertThat(db.spotDao().move("b", 1)).isTrue()
        assertThat(db.spotDao().observeAll().first().map { it.id })
            .containsExactly("a", "b", "c").inOrder()

        assertThat(db.spotDao().move("a", -1)).isFalse()
        assertThat(db.spotDao().move("c", 1)).isFalse()
    }

    @Test fun `deleting spot cascades cache rows`() = runBlocking {
        db.spotDao().upsert(SpotEntity("id1", "Calais", 50.95, 1.85, true, 0L))
        db.forecastCacheDao().upsert(ForecastCacheEntity("id1", "open-meteo", 1L, "Europe/Paris", "[]"))
        db.spotDao().delete("id1")
        assertThat(db.forecastCacheDao().get("id1", "open-meteo")).isNull()
    }
}
