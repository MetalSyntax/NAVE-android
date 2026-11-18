package com.nave.moto.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.nave.moto.data.local.dao.FuelLogDao
import com.nave.moto.data.local.dao.MaintenanceDao
import com.nave.moto.data.local.dao.MotoManualDao
import com.nave.moto.data.local.dao.MotorcycleDao
import com.nave.moto.data.local.dao.RouteDao
import com.nave.moto.data.local.dao.ServiceScheduleDao
import com.nave.moto.data.local.entity.FuelLogEntity
import com.nave.moto.data.local.entity.MaintenanceRecordEntity
import com.nave.moto.data.local.entity.MotoManualEntity
import com.nave.moto.data.local.entity.MotorcycleEntity
import com.nave.moto.data.local.entity.RouteRecordEntity
import com.nave.moto.data.local.entity.ServiceScheduleEntity

@Database(
    entities = [
        MotorcycleEntity::class,
        FuelLogEntity::class,
        MaintenanceRecordEntity::class,
        ServiceScheduleEntity::class,
        RouteRecordEntity::class,
        MotoManualEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MotoDatabase : RoomDatabase() {
    abstract fun motorcycleDao(): MotorcycleDao
    abstract fun fuelLogDao(): FuelLogDao
    abstract fun maintenanceDao(): MaintenanceDao
    abstract fun serviceScheduleDao(): ServiceScheduleDao
    abstract fun routeDao(): RouteDao
    abstract fun motoManualDao(): MotoManualDao
}
