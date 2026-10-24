package com.kompaktwind.data.live

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Test

class MetarRepositoryTest {
    private val metar = AwcMetarDto(
        icaoId = "LFPG",
        name = "Paris/De Gaulle",
        lat = 49.015,
        lon = 2.534,
        wdir = 180.0,
        wspd = 18.0,
        wgst = 36.0,
        obsTime = 1792735200L
    )

    private class FakeAwc(private val rows: List<AwcMetarDto>) : MetarApi {
        var calls = 0
        override suspend fun metar(bbox: String?, ids: String?, hours: Int?, format: String): List<AwcMetarDto> {
            calls++
            return rows
        }
    }

    private class EmptyIem : IemApi {
        override suspend fun currents(network: String): IemResponse = IemResponse()
    }

    @Test fun `nearby maps METAR and converts knots`() = runBlocking {
        val repo = MetarRepository(FakeAwc(listOf(metar)), EmptyIem())
        val result = repo.nearby(48.0, 2.0).getOrThrow()

        assertThat(result).hasSize(1)
        assertThat(result[0].id).isEqualTo("metar:LFPG")
        assertThat(result[0].source).isEqualTo(LiveSource.METAR)
        assertThat(result[0].windAvgMs!!).isWithin(0.01).of(9.26)
        assertThat(result[0].windMaxMs!!).isWithin(0.01).of(18.52)
        assertThat(result[0].windHeadingDeg).isEqualTo(180)
    }

    @Test fun `nearby is cached for 10 minutes`() = runBlocking {
        var now = 0L
        val awc = FakeAwc(listOf(metar))
        val repo = MetarRepository(awc, EmptyIem(), now = { now })

        repo.nearby(48.0, 2.0).getOrThrow()
        repo.nearby(48.0, 2.0).getOrThrow()
        assertThat(awc.calls).isEqualTo(1)

        now = 600_001L
        repo.nearby(48.0, 2.0).getOrThrow()
        assertThat(awc.calls).isEqualTo(2)
    }

    @Test fun `force refresh bypasses the cache`() = runBlocking {
        val awc = FakeAwc(listOf(metar))
        val repo = MetarRepository(awc, EmptyIem(), now = { 0L })

        repo.nearby(48.0, 2.0).getOrThrow()
        repo.nearby(48.0, 2.0, forceRefresh = true).getOrThrow()
        assertThat(awc.calls).isEqualTo(2)
    }

    @Test fun `history maps METAR rows`() = runBlocking {
        val repo = MetarRepository(FakeAwc(listOf(metar)), EmptyIem())
        val result = repo.history("LFPG").getOrThrow()

        assertThat(result).hasSize(1)
        assertThat(result[0].windAvgMs!!).isWithin(0.01).of(9.26)
        assertThat(result[0].windMaxMs!!).isWithin(0.01).of(18.52)
    }
}
