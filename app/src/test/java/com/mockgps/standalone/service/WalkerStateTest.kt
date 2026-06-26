// app/src/test/java/com/mockgps/standalone/service/WalkerStateTest.kt
package com.mockgps.standalone.service

import com.mockgps.standalone.util.GeoMath
import org.junit.Assert.*
import org.junit.Test

class WalkerStateTest {

    @Test
    fun `step changes position`() {
        val state = WalkerState(25.0, 121.0, 300.0, 3.0)
        val (lat, lon) = state.step()
        assertFalse("position must change", lat == 25.0 && lon == 121.0)
    }

    @Test
    fun `position is tracked in state fields`() {
        val state = WalkerState(25.0, 121.0, 300.0, 3.0)
        val (lat, lon) = state.step()
        assertEquals(lat, state.currentLat, 0.0)
        assertEquals(lon, state.currentLon, 0.0)
    }

    @Test
    fun `after 300 steps stays within radius plus buffer`() {
        val state = WalkerState(25.0330, 121.5654, 300.0, 5.0)
        repeat(300) { state.step() }
        val dist = GeoMath.distanceMeters(25.0330, 121.5654, state.currentLat, state.currentLon)
        assertTrue("dist=$dist, expected ≤ 350", dist <= 350.0)
    }
}
