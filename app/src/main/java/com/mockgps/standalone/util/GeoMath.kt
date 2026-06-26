package com.mockgps.standalone.util

import kotlin.math.*
import kotlin.random.Random

object GeoMath {
    private const val R = 6_371_000.0

    fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val φ1 = Math.toRadians(lat1); val φ2 = Math.toRadians(lat2)
        val dφ = Math.toRadians(lat2 - lat1)
        val dλ = Math.toRadians(lon2 - lon1)
        val a = sin(dφ / 2).pow(2) + cos(φ1) * cos(φ2) * sin(dλ / 2).pow(2)
        return 2 * R * asin(sqrt(a))
    }

    fun bearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val φ1 = Math.toRadians(lat1); val φ2 = Math.toRadians(lat2)
        val dλ = Math.toRadians(lon2 - lon1)
        val y = sin(dλ) * cos(φ2)
        val x = cos(φ1) * sin(φ2) - sin(φ1) * cos(φ2) * cos(dλ)
        return (Math.toDegrees(atan2(y, x)) + 360) % 360
    }

    fun destination(lat: Double, lon: Double, bearingDeg: Double, distanceM: Double): Pair<Double, Double> {
        val φ1 = Math.toRadians(lat); val λ1 = Math.toRadians(lon)
        val θ = Math.toRadians(bearingDeg); val δ = distanceM / R
        val φ2 = asin(sin(φ1) * cos(δ) + cos(φ1) * sin(δ) * cos(θ))
        val λ2 = λ1 + atan2(sin(θ) * sin(δ) * cos(φ1), cos(δ) - sin(φ1) * sin(φ2))
        return Pair(Math.toDegrees(φ2), Math.toDegrees(λ2))
    }

    fun randomPointInCircle(centerLat: Double, centerLon: Double, radiusM: Double): Pair<Double, Double> {
        val r = radiusM * sqrt(Random.nextDouble())
        val θ = Random.nextDouble() * 360.0
        return destination(centerLat, centerLon, θ, r)
    }
}
