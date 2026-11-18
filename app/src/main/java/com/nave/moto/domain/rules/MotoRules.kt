package com.nave.moto.domain.rules

import com.nave.moto.domain.model.AlertLevel
import com.nave.moto.domain.model.FuelLog
import com.nave.moto.domain.model.MaintenanceRecord
import com.nave.moto.domain.model.MotoTelemetryStats
import com.nave.moto.domain.model.Motorcycle
import com.nave.moto.domain.model.RouteRecord
import com.nave.moto.domain.model.RouteStatus
import com.nave.moto.domain.model.ServiceAlert
import com.nave.moto.domain.model.ServiceSchedule
import java.time.LocalDate
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Port 1:1 de web/src/domain/motoRules.ts. */

fun normalizeFuelLevel(v: Double): Int {
    if (!v.isFinite() || v <= 0) return 1
    if (v > 5) return max(1, min(5, (v / 20).roundToInt()))
    return min(5, max(1, v.roundToInt()))
}

fun computeEstimatedRangeKm(tankL: Double, fuelLevel: Int, kmPerL: Double): Int {
    val level = normalizeFuelLevel(fuelLevel.toDouble())
    val litersRemaining = tankL * (level / 5.0)
    return max(0, (litersRemaining * kmPerL).roundToInt())
}

fun computeServiceAlerts(
    schedules: List<ServiceSchedule>,
    currentOdometerKm: Int,
    soonThresholdKm: Int = 500
): List<ServiceAlert> =
    schedules.filter { it.enabled && it.intervalKm > 0 }
        .map { s ->
            val next = s.lastServiceKm + s.intervalKm
            val remaining = next - currentOdometerKm
            val level = when {
                remaining <= 0 -> AlertLevel.DUE
                remaining <= soonThresholdKm -> AlertLevel.SOON
                else -> AlertLevel.OK
            }
            ServiceAlert(s, next, remaining, level)
        }
        .sortedBy { it.remainingKm }

private fun round1(x: Double) = (x * 10).roundToInt() / 10.0

fun computeTelemetryStats(
    moto: Motorcycle?,
    fuelLogs: List<FuelLog>,
    maintenance: List<MaintenanceRecord>,
    routes: List<RouteRecord>,
    oilIntervalKm: Int,
    today: LocalDate = LocalDate.now(),
    /** Km del último cambio de aceite según el programa preventivo (si no hay registro de taller). */
    oilBaselineKm: Int = 0
): MotoTelemetryStats {
    if (moto == null) return MotoTelemetryStats(oilRemainingKm = oilIntervalKm, oilLifePercent = 100)
    val logs = fuelLogs.filter { it.motorcycleId == moto.id }
    val maint = maintenance.filter { it.motorcycleId == moto.id }
    val rts = routes.filter { it.motorcycleId == moto.id }

    val maxLogOdo = logs.maxOfOrNull { it.odometerKm } ?: 0
    val maxRouteOdo = rts.filter { it.status == RouteStatus.COMPLETED && (it.odoEndKm ?: 0) > 0 }
        .maxOfOrNull { it.odoEndKm ?: 0 } ?: 0
    val total = maxOf(moto.currentOdometerKm, maxLogOdo, maxRouteOdo)

    val cutoff = today.minusDays(7).toString()
    val logDist = logs.filter { it.date.take(10) >= cutoff }.sumOf { max(0, it.distanceKm) }
    val routeDist = rts.filter { it.status == RouteStatus.COMPLETED && (it.endDate ?: it.startDate).take(10) >= cutoff }
        .sumOf { max(0, it.distanceKm) }

    val avgEff = if (logs.isNotEmpty()) round1(logs.sumOf { it.efficiencyKmL } / logs.size) else moto.efficiencyKmPerLiter

    val lastOilKm = (maint.filter {
        val t = it.type.lowercase(); t.contains("aceite") || t.contains("oil")
    }.maxOfOrNull { it.odometerKm } ?: 0).let { max(it, oilBaselineKm) }
    val sinceOil = max(0, total - lastOilKm)
    val oilRemaining = max(0, oilIntervalKm - sinceOil)
    val oilLife = if (oilIntervalKm > 0) max(0, min(100, (100 - sinceOil.toDouble() / oilIntervalKm * 100).roundToInt())) else 100
    val spend = (maint.sumOf { max(0.0, it.cost) } * 100).roundToInt() / 100.0

    return MotoTelemetryStats(
        totalOdometerKm = total,
        distanceThisWeekKm = logDist + routeDist,
        averageEfficiencyKmL = avgEff,
        oilRemainingKm = oilRemaining,
        oilLifePercent = oilLife,
        estimatedRangeKm = computeEstimatedRangeKm(moto.tankCapacityLiters, moto.fuelLevel, moto.efficiencyKmPerLiter),
        totalMaintenanceSpend = spend
    )
}

fun computeFuelRefillEfficiency(prevOdo: Int, newOdo: Int, liters: Double): Pair<Int, Double> {
    val dist = max(0, newOdo - prevOdo)
    return dist to (if (liters > 0) round1(dist / liters) else 0.0)
}

fun validateFuelLogInput(currentOdo: Int, newOdo: Int?, liters: Double?): String? {
    if (newOdo == null || newOdo <= currentOdo) return "El odómetro debe ser mayor que $currentOdo km."
    if (liters == null || liters <= 0) return "Los litros cargados deben ser mayores a 0."
    return null
}

fun validateMotorcycleInput(brand: String, model: String, year: Int?, odo: Int?, cc: Int?): String? {
    if (brand.isBlank()) return "La marca es obligatoria."
    if (model.isBlank()) return "El modelo es obligatorio."
    if (year == null || year < 1970 || year > 2035) return "Ingresa un año válido."
    if (odo == null || odo < 0) return "El odómetro no puede ser negativo."
    if (cc == null || cc <= 0) return "La cilindrada (cc) debe ser mayor a 0."
    return null
}

fun validateMaintenanceInput(type: String, odo: Int?, cost: Double?): String? {
    if (type.isBlank()) return "El tipo de servicio es obligatorio."
    if (odo == null || odo < 0) return "El kilometraje del servicio no puede ser negativo."
    if (cost == null || cost < 0) return "El costo no puede ser negativo."
    return null
}

/** Programas preventivos por defecto (Apex-Velocity DEFAULT_SERVICE_SCHEDULES) + bujía y filtro. */
val DEFAULT_SCHEDULES: List<Pair<String, Int>> = listOf(
    "aceite" to 3000,
    "relacion" to 8000,
    "llantas" to 10000,
    "frenos" to 12000,
    "baston" to 15000
)

fun scheduleLabel(type: String, es: Boolean): String = when (type) {
    "aceite" -> if (es) "Cambio de aceite" else "Oil change"
    "relacion" -> if (es) "Cadena y piñones" else "Chain & sprockets"
    "llantas" -> if (es) "Cauchos / Llantas" else "Tires"
    "frenos" -> if (es) "Frenos" else "Brakes"
    "baston" -> if (es) "Suspensión" else "Suspension"
    else -> type
}
