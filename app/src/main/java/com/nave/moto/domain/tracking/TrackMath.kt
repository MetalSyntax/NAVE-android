package com.nave.moto.domain.tracking

import kotlin.math.*

data class TrackPoint(val lat: Double, val lon: Double, val timeMs: Long, val accuracyM: Float = 0f)

/** Distancia de círculo máximo (haversine) en metros. */
fun haversineMeters(a: TrackPoint, b: TrackPoint): Double {
    val r = 6_371_000.0
    val dLat = Math.toRadians(b.lat - a.lat)
    val dLon = Math.toRadians(b.lon - a.lon)
    val h = sin(dLat / 2).pow(2) + cos(Math.toRadians(a.lat)) * cos(Math.toRadians(b.lat)) * sin(dLon / 2).pow(2)
    return 2 * r * asin(min(1.0, sqrt(h)))
}

/**
 * Acumula distancia filtrando ruido de GPS: descarta fixes imprecisos, saltos menores al
 * jitter (parado en un semáforo) y velocidades imposibles (salto de torre).
 */
class TrackAccumulator(
    private val maxAccuracyM: Float = 50f,
    private val minStepM: Double = 5.0,
    private val maxSpeedMps: Double = 70.0
) {
    var distanceM = 0.0; private set
    var accepted = 0; private set
    private var last: TrackPoint? = null

    /** @return true si el punto se aceptó (y se debe guardar). */
    fun add(p: TrackPoint): Boolean {
        if (p.accuracyM > maxAccuracyM) return false
        val prev = last
        if (prev == null) { last = p; accepted++; return true }
        val d = haversineMeters(prev, p)
        val dt = (p.timeMs - prev.timeMs) / 1000.0
        if (d < max(minStepM, p.accuracyM * 0.5)) return false
        if (dt > 0 && d / dt > maxSpeedMps) return false
        distanceM += d; last = p; accepted++
        return true
    }
}
