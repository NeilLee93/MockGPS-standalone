// app/src/main/java/com/mockgps/standalone/service/WalkerState.kt
package com.mockgps.standalone.service

import com.mockgps.standalone.util.GeoMath
import kotlin.random.Random

class WalkerState(
    private val centerLat: Double,
    private val centerLon: Double,
    private val radiusM: Double,
    private val speedMs: Double
) {
    var currentLat: Double = centerLat
    var currentLon: Double = centerLon
    private var targetLat: Double
    private var targetLon: Double

    init {
        val t = GeoMath.randomPointInCircle(centerLat, centerLon, radiusM)
        targetLat = t.first; targetLon = t.second
    }

    fun step(): Pair<Double, Double> {
        val dist = GeoMath.distanceMeters(currentLat, currentLon, targetLat, targetLon)
        if (dist < 5.0) pickNewTarget()
        val bearing = GeoMath.bearing(currentLat, currentLon, targetLat, targetLon)
        val step = (speedMs + (Random.nextDouble() - 0.5)).coerceAtLeast(0.1)
        val next = GeoMath.destination(currentLat, currentLon, bearing, step)
        // Clamp to radius
        val distFromCenter = GeoMath.distanceMeters(centerLat, centerLon, next.first, next.second)
        val (clampedLat, clampedLon) = if (distFromCenter > radiusM) {
            val bearingToNext = GeoMath.bearing(centerLat, centerLon, next.first, next.second)
            GeoMath.destination(centerLat, centerLon, bearingToNext, radiusM)
        } else next
        currentLat = clampedLat
        currentLon = clampedLon
        return Pair(clampedLat, clampedLon)
    }

    private fun pickNewTarget() {
        val t = GeoMath.randomPointInCircle(centerLat, centerLon, radiusM)
        targetLat = t.first; targetLon = t.second
    }
}
