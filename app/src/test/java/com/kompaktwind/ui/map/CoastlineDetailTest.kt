package com.kompaktwind.ui.map

import org.junit.Assert.assertEquals
import org.junit.Test

class CoastlineDetailTest {

    @Test
    fun `zoomed out uses coarse data`() {
        assertEquals(CoastlineDetail.LOW, detailFor(zoom = 2.0, lat = 47.0, lon = -4.0))
        assertEquals(CoastlineDetail.MEDIUM, detailFor(zoom = 5.0, lat = 47.0, lon = -4.0))
    }

    @Test
    fun `zooming in escalates detail in Europe`() {
        assertEquals(CoastlineDetail.HIGH, detailFor(zoom = 6.9, lat = 47.0, lon = -4.0))
        assertEquals(CoastlineDetail.ULTRA, detailFor(zoom = 7.0, lat = 47.0, lon = -4.0))
        assertEquals(CoastlineDetail.ULTRA, detailFor(zoom = 8.0, lat = 47.0, lon = -4.0))
        assertEquals(CoastlineDetail.ULTRA, detailFor(zoom = 12.0, lat = 47.0, lon = -4.0))
    }

    @Test
    fun `outside Europe stays at medium detail`() {
        assertEquals(CoastlineDetail.MEDIUM, detailFor(zoom = 8.0, lat = 20.0, lon = -70.0))
        assertEquals(CoastlineDetail.MEDIUM, detailFor(zoom = 12.0, lat = -20.0, lon = 120.0))
    }
}
