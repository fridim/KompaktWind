package com.kompaktwind.data.catalog

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.ByteArrayInputStream

@RunWith(RobolectricTestRunner::class)
class SpotCatalogRepositoryTest {
    @get:Rule val temp = TemporaryFolder()

    private fun repository(store: LocalSpotStore = LocalSpotStore(temp.root)) = SpotCatalogRepository(
        context = RuntimeEnvironment.getApplication(),
        localStore = store
    )

    @Test fun `loads bundled index without network`() = runBlocking {
        val index = repository().index().getOrThrow()

        assertThat(index).isNotEmpty()
        assertThat(index.map { it.id }).contains("osm-way-77221296")
        val unnamed = index.first { it.id == "osm-way-1571321477" }
        assertThat(unnamed.name).isEmpty()
        assertThat(unnamed.displayName()).isNotEmpty()
        assertThat(index.all { it.displayName().isNotBlank() }).isTrue()
    }

    @Test fun `loads bundled shard`() = runBlocking {
        val shard = repository().shard("59_10").getOrThrow()

        assertThat(shard).isNotEmpty()
    }

    @Test fun `merges local imports into the index`() = runBlocking {
        val store = LocalSpotStore(temp.root)
        store.import(
            "private.json",
            ByteArrayInputStream("""[{"name":"Private Spot","lat":43.1,"lon":3.2}]""".toByteArray())
        )

        val index = repository(store).index().getOrThrow()

        assertThat(index.map { it.name }).contains("Private Spot")
    }

    @Test fun `deduplicates local spots against the bundled catalog`() = runBlocking {
        val store = LocalSpotStore(temp.root)
        val bundled = repository(store).index().getOrThrow().first()
        store.import(
            "duplicate.json",
            ByteArrayInputStream(
                """[{"name":"${bundled.name}","lat":${bundled.lat},"lon":${bundled.lon}}]""".toByteArray()
            )
        )

        val index = repository(store).index().getOrThrow()

        assertThat(index.count { it.dedupeKey() == bundled.dedupeKey() }).isEqualTo(1)
    }

    @Test fun `includes local spots in map shards`() = runBlocking {
        val store = LocalSpotStore(temp.root)
        store.import(
            "private.json",
            ByteArrayInputStream("""[{"name":"Private Spot","lat":43.1,"lon":3.2}]""".toByteArray())
        )

        val shard = repository(store).shard("43_3").getOrThrow()

        assertThat(shard.map { it.name }).contains("Private Spot")
    }
}
