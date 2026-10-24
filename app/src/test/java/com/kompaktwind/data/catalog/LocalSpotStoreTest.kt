package com.kompaktwind.data.catalog

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream

class LocalSpotStoreTest {
    @get:Rule val temp = TemporaryFolder()

    private fun store(maxBytes: Int = 5 * 1024 * 1024, maxSpots: Int = 10_000) =
        LocalSpotStore(temp.root, maxBytes = maxBytes, maxSpots = maxSpots)

    private fun importJson(store: LocalSpotStore, name: String, json: String) =
        runBlocking { store.import(name, ByteArrayInputStream(json.toByteArray())) }

    @Test fun `imports valid spots with local ids and tiles`() = runBlocking {
        val outcome = importJson(
            store(),
            "my-spots.json",
            """[{"id":"a","name":"Spot A","lat":43.1,"lon":3.2,"source":"example.com"}]"""
        )

        assertThat(outcome).isInstanceOf(ImportOutcome.Success::class.java)
        val info = (outcome as ImportOutcome.Success).info
        assertThat(info.fileName).isEqualTo("my-spots.json")
        assertThat(info.spots).isEqualTo(1)
        assertThat(info.skipped).isEqualTo(0)

        val spots = store().spots()
        assertThat(spots).hasSize(1)
        assertThat(spots[0].name).isEqualTo("Spot A")
        assertThat(spots[0].local).isTrue()
        assertThat(spots[0].id).startsWith("local-")
        assertThat(spots[0].tile).isEqualTo("43_3")
        assertThat(spots[0].source).isEqualTo("example.com")
    }

    @Test fun `skips invalid rows`() = runBlocking {
        val outcome = importJson(
            store(),
            "mixed.json",
            """[
                {"name":"Good","lat":43.1,"lon":3.2},
                {"name":"Bad lat","lat":100.0,"lon":3.2},
                {"name":"","lat":43.1,"lon":3.2},
                {"name":123,"lat":43.1,"lon":3.2}
            ]"""
        )

        val info = (outcome as ImportOutcome.Success).info
        assertThat(info.spots).isEqualTo(1)
        assertThat(info.skipped).isEqualTo(3)
        assertThat(store().spots().map { it.name }).containsExactly("Good")
        Unit
    }

    @Test fun `rejects invalid json`() {
        val outcome = importJson(store(), "bad.json", "not json")
        assertThat(outcome).isEqualTo(ImportOutcome.Failure(ImportFailure.INVALID_JSON))
    }

    @Test fun `rejects oversized files`() {
        val outcome = importJson(store(maxBytes = 10), "big.json", """[{"name":"A","lat":1.0,"lon":2.0}]""")
        assertThat(outcome).isEqualTo(ImportOutcome.Failure(ImportFailure.TOO_LARGE))
    }

    @Test fun `rejects too many spots`() {
        val outcome = importJson(
            store(maxSpots = 1),
            "many.json",
            """[{"name":"A","lat":1.0,"lon":2.0},{"name":"B","lat":1.0,"lon":2.0}]"""
        )
        assertThat(outcome).isEqualTo(ImportOutcome.Failure(ImportFailure.TOO_MANY))
    }

    @Test fun `rejects files without valid spots`() {
        val outcome = importJson(store(), "empty.json", """[{"name":"","lat":1.0,"lon":2.0}]""")
        assertThat(outcome).isEqualTo(ImportOutcome.Failure(ImportFailure.NO_VALID_SPOTS))
    }

    @Test fun `deletes an imported file`() = runBlocking {
        val info = (importJson(store(), "one.json", """[{"name":"A","lat":1.0,"lon":2.0}]""")
            as ImportOutcome.Success).info
        assertThat(store().list()).hasSize(1)

        store().delete(info.id)

        assertThat(store().list()).isEmpty()
        assertThat(store().spots()).isEmpty()
    }

    @Test fun `imports persist across instances`() = runBlocking {
        importJson(store(), "one.json", """[{"name":"A","lat":1.0,"lon":2.0}]""")
        assertThat(store().list()).hasSize(1)
        assertThat(store().spots()).hasSize(1)
    }
}
