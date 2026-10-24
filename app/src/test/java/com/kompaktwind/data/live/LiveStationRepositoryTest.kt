package com.kompaktwind.data.live

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Test

class LiveStationRepositoryTest {
    private val stationOne = PiouPiouLiveDto(
        id = 1,
        meta = PiouPiouMeta("Near"),
        location = PiouPiouLocation(48.0, 2.0, success = true),
        measurements = PiouPiouMeasurements(
            date = "2026-10-22T07:00:00Z",
            windHeading = 180.0,
            windSpeedAvg = 36.0,
            windSpeedMax = 54.0,
            windSpeedMin = 12.0
        )
    )

    private val stationTwo = stationOne.copy(
        id = 2,
        meta = PiouPiouMeta("Far"),
        location = PiouPiouLocation(49.0, 2.0, success = true)
    )

    private val archive = PiouPiouArchiveResponse(
        legend = listOf(
            "time", "latitude", "longitude", "wind_speed_min",
            "wind_speed_avg", "wind_speed_max", "wind_heading", "pressure"
        ),
        units = listOf("utc", "degrees", "degrees", "km/h", "km/h", "km/h", "degrees", "deprecated"),
        data = listOf(
            listOf<JsonElement?>(
                JsonPrimitive("2026-10-22T07:00:00Z"),
                JsonPrimitive(48.0),
                JsonPrimitive(2.0),
                JsonPrimitive(12.0),
                JsonPrimitive(36.0),
                JsonPrimitive(54.0),
                JsonPrimitive(180.0),
                JsonNull
            )
        )
    )

    private class FakeApi(
        private val all: List<PiouPiouLiveDto>,
        private val archive: PiouPiouArchiveResponse
    ) : PiouPiouApi {
        var allCalls = 0
        override suspend fun liveAll(): PiouPiouLiveAllResponse {
            allCalls++
            return PiouPiouLiveAllResponse(all)
        }
        override suspend fun live(id: Int) = PiouPiouLiveSingleResponse(all.first { it.id == id })
        override suspend fun archive(id: Int, start: String, stop: String, format: String) = archive
    }

    @Test fun `nearby sorts by distance and converts kmh to ms`() = runBlocking {
        val repo = LiveStationRepository(FakeApi(listOf(stationTwo, stationOne), archive))
        val result = repo.nearby(48.0, 2.0).getOrThrow()

        assertThat(result.map { it.id }).containsExactly("pioupiou:1", "pioupiou:2").inOrder()
        assertThat(result[0].distanceKm).isWithin(0.001).of(0.0)
        assertThat(result[0].windAvgMs).isWithin(0.001).of(10.0)
        assertThat(result[0].windMaxMs).isWithin(0.001).of(15.0)
        assertThat(result[0].windHeadingDeg).isEqualTo(180)
        assertThat(result[0].source).isEqualTo(LiveSource.OPENWINDMAP)
    }

    @Test fun `nearby is cached for 5 minutes and force refresh bypasses it`() = runBlocking {
        var now = 0L
        val api = FakeApi(listOf(stationOne), archive)
        val repo = LiveStationRepository(api, now = { now })

        repo.nearby(48.0, 2.0).getOrThrow()
        repo.nearby(48.0, 2.0).getOrThrow()
        assertThat(api.allCalls).isEqualTo(1)

        now = 300_001L
        repo.nearby(48.0, 2.0).getOrThrow()
        assertThat(api.allCalls).isEqualTo(2)

        repo.nearby(48.0, 2.0, forceRefresh = true).getOrThrow()
        assertThat(api.allCalls).isEqualTo(3)
    }

    @Test fun `history parses archive rows and converts kmh to ms`() = runBlocking {
        val repo = LiveStationRepository(FakeApi(listOf(stationOne), archive))
        val result = repo.history(1).getOrThrow()

        assertThat(result).hasSize(1)
        assertThat(result[0].windAvgMs).isWithin(0.001).of(10.0)
        assertThat(result[0].windMaxMs).isWithin(0.001).of(15.0)
        assertThat(result[0].headingDeg).isEqualTo(180)
    }
}
