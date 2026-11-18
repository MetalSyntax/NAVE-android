package com.nave.moto.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nave.moto.data.local.entity.FuelLogEntity
import com.nave.moto.data.local.entity.MaintenanceRecordEntity
import com.nave.moto.data.local.entity.MotoManualEntity
import com.nave.moto.data.local.entity.MotorcycleEntity
import com.nave.moto.data.local.entity.RouteRecordEntity
import com.nave.moto.data.local.entity.ServiceScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MotorcycleDao {
    @Query("SELECT * FROM motorcycles")
    suspend fun getAll(): List<MotorcycleEntity>

    @Query("DELETE FROM motorcycles")
    suspend fun deleteAll()

    @Query("SELECT * FROM motorcycles ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<MotorcycleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(motorcycle: MotorcycleEntity): Long

    @Query("UPDATE motorcycles SET currentOdometerKm = :odoKm, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateOdometer(id: Long, odoKm: Int, updatedAt: String)

    @Query("UPDATE motorcycles SET fuelLevel = :level, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateFuelLevel(id: Long, level: Int, updatedAt: String)

    @Query("SELECT * FROM motorcycles WHERE id = :id")
    suspend fun getById(id: Long): MotorcycleEntity?

    @Query("DELETE FROM motorcycles WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface FuelLogDao {
    @Query("SELECT * FROM fuel_logs")
    suspend fun getAll(): List<FuelLogEntity>

    @Query("DELETE FROM fuel_logs")
    suspend fun deleteAll()

    @Query("SELECT * FROM fuel_logs WHERE motorcycleId = :motoId ORDER BY odometerKm DESC, id DESC")
    fun observeByMoto(motoId: Long): Flow<List<FuelLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: FuelLogEntity): Long

    @Query("DELETE FROM fuel_logs WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface MaintenanceDao {
    @Query("SELECT * FROM maintenance_records")
    suspend fun getAll(): List<MaintenanceRecordEntity>

    @Query("DELETE FROM maintenance_records")
    suspend fun deleteAll()

    @Query("SELECT * FROM maintenance_records WHERE motorcycleId = :motoId ORDER BY odometerKm DESC, id DESC")
    fun observeByMoto(motoId: Long): Flow<List<MaintenanceRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: MaintenanceRecordEntity): Long

    @Query("DELETE FROM maintenance_records WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface ServiceScheduleDao {
    @Query("UPDATE service_schedules SET lastServiceKm = :km WHERE id = :id")
    suspend fun markDone(id: Long, km: Int)

    @Query("SELECT * FROM service_schedules")
    suspend fun getAll(): List<ServiceScheduleEntity>

    @Query("DELETE FROM service_schedules")
    suspend fun deleteAll()

    @Query("SELECT * FROM service_schedules WHERE motorcycleId = :motoId ORDER BY id ASC")
    fun observeByMoto(motoId: Long): Flow<List<ServiceScheduleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(schedule: ServiceScheduleEntity): Long

    @Query("DELETE FROM service_schedules WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface RouteDao {
    @Query("SELECT * FROM route_records")
    suspend fun getAll(): List<RouteRecordEntity>

    @Query("DELETE FROM route_records")
    suspend fun deleteAll()

    @Query("SELECT * FROM route_records WHERE motorcycleId = :motoId ORDER BY id DESC")
    fun observeByMoto(motoId: Long): Flow<List<RouteRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(route: RouteRecordEntity): Long

    @Query("DELETE FROM route_records WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface MotoManualDao {
    @Query("SELECT * FROM moto_manuals")
    suspend fun getAll(): List<MotoManualEntity>

    @Query("DELETE FROM moto_manuals")
    suspend fun deleteAll()

    @Query("SELECT * FROM moto_manuals ORDER BY id DESC")
    fun observeAll(): Flow<List<MotoManualEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(manual: MotoManualEntity): Long

    @Query("DELETE FROM moto_manuals WHERE id = :id")
    suspend fun deleteById(id: Long)
}
