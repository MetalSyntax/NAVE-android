package com.nave.moto.domain.model

enum class AlertLevel { DUE, SOON, OK }

enum class RouteStatus { ACTIVE, COMPLETED }

data class Motorcycle(
    val id: Long = 0,
    val brand: String,
    val model: String,
    val year: Int,
    val category: String,
    val cc: Int,
    val color: String,
    val plate: String,
    val vin: String = "",
    val fuelLevel: Int = 4, // 1..5
    val tankCapacityLiters: Double = 13.0,
    val efficiencyKmPerLiter: Double = 38.0,
    val currentOdometerKm: Int,
    val insurer: String = "",
    val policyNumber: String = "",
    val policyValidUntil: String = "",
    /** Ruta relativa dentro de filesDir (p. ej. "photos/abc.webp") o null. */
    val photoUri: String? = null,
    val createdAt: String,
    val updatedAt: String
)

data class FuelLog(
    val id: Long = 0,
    val motorcycleId: Long,
    val date: String,
    val odometerKm: Int,
    val liters: Double,
    val efficiencyKmL: Double,
    val distanceKm: Int = 0,
    val notes: String = ""
)

data class MaintenanceRecord(
    val id: Long = 0,
    val motorcycleId: Long,
    val type: String,
    val date: String,
    val odometerKm: Int,
    val cost: Double = 0.0,
    val workshop: String = "",
    val notes: String = ""
)

data class ServiceSchedule(
    val id: Long = 0,
    val motorcycleId: Long,
    val type: String, // "aceite" | "relacion" | "llantas" | "frenos" | "baston" | texto libre
    val intervalKm: Int,
    val lastServiceKm: Int,
    val enabled: Boolean = true
)

data class ServiceAlert(
    val schedule: ServiceSchedule,
    val nextKm: Int,
    val remainingKm: Int,
    val level: AlertLevel
)

data class RouteRecord(
    val id: Long = 0,
    val motorcycleId: Long,
    val name: String,
    val status: RouteStatus,
    val startDate: String,
    val endDate: String? = null,
    val odoStartKm: Int,
    val odoEndKm: Int? = null,
    val distanceKm: Int = 0,
    val estimatedFuelLiters: Double = 0.0,
    val notes: String = ""
)

data class MotoManual(
    val id: Long = 0,
    val motorcycleId: Long?,
    val title: String,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    /** Ruta relativa dentro de filesDir (p. ej. "manuals/abc.pdf"). */
    val localUri: String,
    val addedAt: String
)

data class AppSettings(
    val darkTheme: Boolean = true,
    val language: String = "es",
    val oilIntervalKm: Int = 3000,
    val activeMotorcycleId: Long? = null,
    val riderName: String = "Piloto NAVE",
    val onboardingComplete: Boolean = true,
    /** Uso en la calle: alto contraste, tamaño de texto, háptica y GPS en ruta. */
    val highContrast: Boolean = false,
    val fontScale: Float = 1f,
    val haptics: Boolean = true,
    val gpsTracking: Boolean = true,
    val gpsIntervalSec: Int = 3,
    val keepScreenOnRoute: Boolean = true,
    /** Perfil del motorizado (todo local). */
    val riderIdNumber: String = "",
    val riderPhone: String = "",
    val bloodType: String = "",
    val emergencyName: String = "",
    val emergencyPhone: String = "",
    val licenseNumber: String = "",
    val licenseExpiry: String = "",
    /** Ruta relativa en filesDir (photos/...) o null. */
    val riderPhoto: String? = null
)

data class MotoTelemetryStats(
    val totalOdometerKm: Int = 0,
    val distanceThisWeekKm: Int = 0,
    val averageEfficiencyKmL: Double = 0.0,
    val oilRemainingKm: Int = 3000,
    val oilLifePercent: Int = 100,
    val estimatedRangeKm: Int = 0,
    val totalMaintenanceSpend: Double = 0.0
)

/** Fila del catálogo empaquetado (solo lectura). */
data class CatalogModel(
    val id: Long,
    val brand: String,
    val name: String,
    val category: String,
    val cc: Int,
    val tankL: Double,
    val kmL: Double,
    val maxSpeedKmh: Int,
    val weightKg: Int,
    val colors: String,
    val hasThumb: Boolean
)

data class CatalogSpec(val section: String, val body: String)
