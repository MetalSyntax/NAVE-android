package com.nave.moto.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "motorcycles")
data class MotorcycleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val brand: String,
    val model: String,
    val year: Int,
    val category: String,
    val cc: Int,
    val color: String,
    val plate: String,
    val vin: String,
    val fuelLevel: Int,
    val tankCapacityLiters: Double,
    val efficiencyKmPerLiter: Double,
    val currentOdometerKm: Int,
    val insurer: String,
    val policyNumber: String,
    val policyValidUntil: String,
    val photoUri: String?,
    val createdAt: String,
    val updatedAt: String
)

@Entity(
    tableName = "fuel_logs",
    foreignKeys = [
        ForeignKey(
            entity = MotorcycleEntity::class,
            parentColumns = ["id"],
            childColumns = ["motorcycleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("motorcycleId")]
)
data class FuelLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val motorcycleId: Long,
    val date: String,
    val odometerKm: Int,
    val liters: Double,
    val efficiencyKmL: Double,
    val distanceKm: Int,
    val notes: String
)

@Entity(
    tableName = "maintenance_records",
    foreignKeys = [
        ForeignKey(
            entity = MotorcycleEntity::class,
            parentColumns = ["id"],
            childColumns = ["motorcycleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("motorcycleId")]
)
data class MaintenanceRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val motorcycleId: Long,
    val type: String,
    val date: String,
    val odometerKm: Int,
    val cost: Double,
    val workshop: String,
    val notes: String
)

@Entity(
    tableName = "service_schedules",
    foreignKeys = [
        ForeignKey(
            entity = MotorcycleEntity::class,
            parentColumns = ["id"],
            childColumns = ["motorcycleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("motorcycleId")]
)
data class ServiceScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val motorcycleId: Long,
    val type: String,
    val intervalKm: Int,
    val lastServiceKm: Int,
    val enabled: Boolean
)

@Entity(
    tableName = "route_records",
    foreignKeys = [
        ForeignKey(
            entity = MotorcycleEntity::class,
            parentColumns = ["id"],
            childColumns = ["motorcycleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("motorcycleId")]
)
data class RouteRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val motorcycleId: Long,
    val name: String,
    val status: String,
    val startDate: String,
    val endDate: String?,
    val odoStartKm: Int,
    val odoEndKm: Int?,
    val distanceKm: Int,
    val estimatedFuelLiters: Double,
    val notes: String
)

@Entity(tableName = "moto_manuals")
data class MotoManualEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val motorcycleId: Long?,
    val title: String,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val localUri: String,
    val addedAt: String
)
