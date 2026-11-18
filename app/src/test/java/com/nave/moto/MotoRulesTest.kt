package com.nave.moto

import com.nave.moto.domain.model.*
import com.nave.moto.domain.rules.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

/** Port de web/src/domain/motoRules.test.ts */
class MotoRulesTest {
    private val moto = Motorcycle(
        id = 1, brand = "Bera", model = "SBR 150cc", year = 2024, category = "PASEO", cc = 150, color = "Negro / Rojo",
        plate = "AB123CD", vin = "8X9BR15024001", fuelLevel = 4, tankCapacityLiters = 13.0, efficiencyKmPerLiter = 40.0,
        currentOdometerKm = 14800, createdAt = "2026-01-10", updatedAt = "2026-11-15"
    )
    private val logs = listOf(FuelLog(1, 1, "2026-11-15", 14800, 3.5, 40.0, 140, "Estación central"))
    private val maint = listOf(MaintenanceRecord(1, 1, "Cambio de aceite", "2026-10-20", 12500, 18.0, "Taller NAVE", "20W50"))
    private val routes = listOf(RouteRecord(1, 1, "Ruta La Guaira", RouteStatus.COMPLETED, "2026-11-14", "2026-11-14", 14600, 14660, 60, 1.5))

    @Test fun normalizeFuelLevelAndRange() {
        assertEquals(1, normalizeFuelLevel(0.0))
        assertEquals(4, normalizeFuelLevel(80.0))
        assertEquals(5, normalizeFuelLevel(5.0))
        assertEquals(416, computeEstimatedRangeKm(13.0, 4, 40.0)) // 13 * 4/5 * 40
    }

    @Test fun serviceAlerts() {
        val s = listOf(
            ServiceSchedule(1, 1, "aceite", 3000, 11500, true),   // next 14500 -> -300 due
            ServiceSchedule(2, 1, "frenos", 12000, 3100, true),   // next 15100 -> +300 soon
            ServiceSchedule(3, 1, "llantas", 10000, 10000, true), // next 20000 -> +5200 ok
            ServiceSchedule(4, 1, "disabled", 1000, 0, false)
        )
        val a = computeServiceAlerts(s, 14800, 500)
        assertEquals(3, a.size)
        assertEquals(AlertLevel.DUE, a[0].level); assertEquals(-300, a[0].remainingKm)
        assertEquals(AlertLevel.SOON, a[1].level)
        assertEquals(AlertLevel.OK, a[2].level)
    }

    @Test fun telemetryStats() {
        val st = computeTelemetryStats(moto, logs, maint, routes, 3000, LocalDate.of(2026, 11, 16))
        assertEquals(14800, st.totalOdometerKm)
        assertEquals(200, st.distanceThisWeekKm) // 140 + 60
        assertEquals(700, st.oilRemainingKm)
        assertEquals(23, st.oilLifePercent)
        assertEquals(18.0, st.totalMaintenanceSpend, 0.0)
        assertEquals(416, st.estimatedRangeKm)
    }

    @Test fun telemetryWithoutMoto() {
        val st = computeTelemetryStats(null, emptyList(), emptyList(), emptyList(), 3000)
        assertEquals(0, st.totalOdometerKm); assertEquals(100, st.oilLifePercent)
    }

    @Test fun refillEfficiency() {
        val (d, e) = computeFuelRefillEfficiency(14800, 14950, 4.0)
        assertEquals(150, d); assertEquals(37.5, e, 0.0)
    }

    @Test fun validators() {
        assertNotNull(validateFuelLogInput(14800, 14800, 3.0))
        assertNotNull(validateFuelLogInput(14800, 14900, 0.0))
        assertNull(validateFuelLogInput(14800, 14920, 3.2))
        assertNotNull(validateMotorcycleInput("", "SBR", 2024, 100, 150))
        assertNull(validateMotorcycleInput("Bera", "SBR", 2024, 100, 150))
        assertNotNull(validateMaintenanceInput("", 100, 10.0))
        assertNull(validateMaintenanceInput("Aceite", 14800, 15.0))
    }
}
