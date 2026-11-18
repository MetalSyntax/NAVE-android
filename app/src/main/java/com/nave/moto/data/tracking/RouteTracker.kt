package com.nave.moto.data.tracking

import android.content.Context
import com.nave.moto.domain.tracking.TrackAccumulator
import com.nave.moto.domain.tracking.TrackPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

data class TrackState(
    val routeId: Long? = null,
    val running: Boolean = false,
    val distanceM: Double = 0.0,
    val points: Int = 0,
    val accuracyM: Float? = null,
    val speedKmh: Float? = null,
    val gpsEnabled: Boolean = true
)

/** Estado compartido entre el servicio de GPS y la UI + persistencia CSV por ruta. */
object RouteTracker {
    private val _state = MutableStateFlow(TrackState())
    val state: StateFlow<TrackState> = _state

    fun trackPath(routeId: Long) = "tracks/route-$routeId.csv"
    private fun file(ctx: Context, routeId: Long) = File(ctx.filesDir, trackPath(routeId))

    fun update(f: (TrackState) -> TrackState) { _state.value = f(_state.value) }
    fun reset() { _state.value = TrackState() }

    /** Reconstruye la distancia desde el CSV (reanudar tras cerrar la app). */
    fun restore(ctx: Context, routeId: Long): TrackAccumulator {
        val acc = TrackAccumulator()
        runCatching {
            file(ctx, routeId).takeIf { it.exists() }?.forEachLine { l ->
                val c = l.split(',')
                if (c.size >= 4) acc.add(TrackPoint(c[0].toDouble(), c[1].toDouble(), c[2].toLong(), c[3].toFloat()))
            }
        }
        return acc
    }

    fun append(ctx: Context, routeId: Long, p: TrackPoint) {
        runCatching {
            val f = file(ctx, routeId); f.parentFile?.mkdirs()
            f.appendText("${p.lat},${p.lon},${p.timeMs},${p.accuracyM}\n")
        }
    }

    /** Metros guardados de una ruta, aunque el servicio ya no corra. */
    fun savedDistanceM(ctx: Context, routeId: Long): Double = restore(ctx, routeId).distanceM
}
