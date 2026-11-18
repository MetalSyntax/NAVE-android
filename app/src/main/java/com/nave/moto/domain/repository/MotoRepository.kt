package com.nave.moto.domain.repository

import com.nave.moto.domain.model.AppSettings
import com.nave.moto.domain.model.FuelLog
import com.nave.moto.domain.model.MaintenanceRecord
import com.nave.moto.domain.model.MotoManual
import com.nave.moto.domain.model.Motorcycle
import com.nave.moto.domain.model.RouteRecord
import com.nave.moto.domain.model.ServiceSchedule
import com.nave.moto.domain.model.CatalogModel
import com.nave.moto.domain.model.CatalogSpec
import kotlinx.coroutines.flow.Flow

interface MotoRepository {
    fun observeMotorcycles(): Flow<List<Motorcycle>>
    fun observeFuelLogs(motorcycleId: Long): Flow<List<FuelLog>>
    fun observeMaintenanceRecords(motorcycleId: Long): Flow<List<MaintenanceRecord>>
    fun observeServiceSchedules(motorcycleId: Long): Flow<List<ServiceSchedule>>
    fun observeRoutes(motorcycleId: Long): Flow<List<RouteRecord>>
    fun observeManuals(): Flow<List<MotoManual>>
    fun observeSettings(): Flow<AppSettings>

    suspend fun upsertMotorcycle(motorcycle: Motorcycle): Long
    suspend fun deleteMotorcycle(motorcycleId: Long)
    suspend fun updateOdometer(motorcycleId: Long, newOdometerKm: Int)
    suspend fun updateFuelLevel(motorcycleId: Long, level: Int)

    suspend fun insertFuelLog(log: FuelLog): Long
    suspend fun deleteFuelLog(logId: Long)

    suspend fun insertMaintenanceRecord(record: MaintenanceRecord): Long
    suspend fun deleteMaintenanceRecord(recordId: Long)

    suspend fun upsertServiceSchedule(schedule: ServiceSchedule): Long
    suspend fun deleteServiceSchedule(scheduleId: Long)

    suspend fun upsertRoute(route: RouteRecord): Long
    suspend fun deleteRoute(routeId: Long)

    suspend fun insertManual(manual: MotoManual): Long
    suspend fun deleteManual(manualId: Long)

    /** Inserta la moto, crea los programas preventivos por defecto y la deja activa. */
    suspend fun addMotorcycleWithDefaults(motorcycle: Motorcycle): Long
    suspend fun markScheduleDone(scheduleId: Long, km: Int)
    suspend fun getMotorcycle(id: Long): Motorcycle?

    suspend fun catalogBrands(): List<String>
    suspend fun catalogModels(brand: String): List<CatalogModel>
    suspend fun catalogSearch(query: String): List<CatalogModel>
    suspend fun catalogSpecs(modelId: Long): List<CatalogSpec>
    suspend fun catalogThumb(modelId: Long): ByteArray?

    suspend fun setActiveMotorcycle(motorcycleId: Long)
    suspend fun updateSettings(settings: AppSettings)
}

/** Exportación/importación completa (zip: data.json + files/). */
interface BackupService {
    suspend fun export(out: java.io.OutputStream)
    /** Reemplaza todos los datos locales. Devuelve un resumen legible. */
    suspend fun import(input: java.io.InputStream): String
    /** Sustituye el garaje por datos de ejemplo. */
    suspend fun restoreDemo()
}
