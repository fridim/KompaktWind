package com.kompaktwind.ui.map

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MapAssetTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test fun `binary map assets load`() {
        val low = loadMapData(context, CoastlineDetail.LOW)
        val medium = loadMapData(context, CoastlineDetail.MEDIUM)
        val high = loadMapData(context, CoastlineDetail.HIGH)
        val ultra = loadMapData(context, CoastlineDetail.ULTRA)

        assertThat(low.land.size).isGreaterThan(100)
        assertThat(medium.land.size).isGreaterThan(1_000)
        assertThat(high.land.size).isGreaterThan(700)
        assertThat(ultra.land.size).isGreaterThan(30_000)

        assertThat(high.land.flatMap { it.rings }.flatten()).isNotEmpty()
        expectNoLeaks(ultra)
    }

    private fun expectNoLeaks(data: MapData) {
        val points = data.land.sumOf { polygon -> polygon.rings.sumOf { it.size } }
        assertThat(points).isGreaterThan(250_000)
    }
}
