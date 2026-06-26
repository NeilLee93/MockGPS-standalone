package com.mockgps.standalone.util

import org.junit.Assert.*
import org.junit.Test

class GeoMathTest {

    @Test
    fun `distanceMeters same point is zero`() {
        assertEquals(0.0, GeoMath.distanceMeters(25.0, 121.0, 25.0, 121.0), 0.001)
    }

    @Test
    fun `distanceMeters Taipei to Kaohsiung approx 307km`() {
        val d = GeoMath.distanceMeters(25.0330, 121.5654, 22.6273, 120.3014)
        assertTrue("expected ~307000 but was $d", d in 290_000.0..320_000.0)
    }

    @Test
    fun `bearing due north is 0 degrees`() {
        assertEquals(0.0, GeoMath.bearing(0.0, 0.0, 1.0, 0.0), 1.0)
    }

    @Test
    fun `bearing due east is 90 degrees`() {
        assertEquals(90.0, GeoMath.bearing(0.0, 0.0, 0.0, 1.0), 1.0)
    }

    @Test
    fun `bearing due south is 180 degrees`() {
        assertEquals(180.0, GeoMath.bearing(1.0, 0.0, 0.0, 0.0), 1.0)
    }

    @Test
    fun `destination 1000m north increases latitude`() {
        val (lat2, lon2) = GeoMath.destination(25.0, 121.0, 0.0, 1000.0)
        assertTrue(lat2 > 25.0)
        assertEquals(121.0, lon2, 0.001)
    }

    @Test
    fun `destination round-trip is close to origin`() {
        val (lat2, lon2) = GeoMath.destination(25.0330, 121.5654, 45.0, 500.0)
        val (lat3, lon3) = GeoMath.destination(lat2, lon2, 225.0, 500.0)
        assertEquals(25.0330, lat3, 0.0001)
        assertEquals(121.5654, lon3, 0.0001)
    }

    @Test
    fun `randomPointInCircle stays within radius`() {
        repeat(200) {
            val (lat, lon) = GeoMath.randomPointInCircle(25.0330, 121.5654, 300.0)
            val d = GeoMath.distanceMeters(25.0330, 121.5654, lat, lon)
            assertTrue("point $d m from center, expected ≤ 300", d <= 301.0)
        }
    }
}
