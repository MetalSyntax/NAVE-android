package com.nave.moto.data.mapper

import com.nave.moto.data.local.entity.FuelLogEntity
import com.nave.moto.data.local.entity.MaintenanceRecordEntity
import com.nave.moto.data.local.entity.MotoManualEntity
import com.nave.moto.data.local.entity.MotorcycleEntity
import com.nave.moto.data.local.entity.RouteRecordEntity
import com.nave.moto.data.local.entity.ServiceScheduleEntity
import com.nave.moto.domain.model.FuelLog
import com.nave.moto.domain.model.MaintenanceRecord
import com.nave.moto.domain.model.MotoManual
import com.nave.moto.domain.model.Motorcycle
import com.nave.moto.domain.model.RouteRecord
import com.nave.moto.domain.model.RouteStatus
import com.nave.moto.domain.model.ServiceSchedule

fun MotorcycleEntity.toDomain() = Motorcycle(
    id = id,
    brand = brand,
    model = model,
    year = year,
    category = category,
    cc = cc,
    color = color,
    plate = plate,
    vin = vin,
    fuelLevel = fuelLevel,
    tankCapacityLiters = tankCapacityLiters,
    efficiencyKmPerLiter = efficiencyKmPerLiter,
    currentOdometerKm = currentOdometerKm,
    insurer = insurer,
    policyNumber = policyNumber,
    policyValidUntil = policyValidUntil,
    photoUri = photoUri,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Motorcycle.toEntity() = MotorcycleEntity(
    id = id,
    brand = brand,
    model = model,
    year = year,
    category = category,
    cc = cc,
    color = color,
    plate = plate,
    vin = vin,
    fuelLevel = fuelLevel,
    tankCapacityLiters = tankCapacityLiters,
    efficiencyKmPerLiter = efficiencyKmPerLiter,
    currentOdometerKm = currentOdometerKm,
    insurer = insurer,
    policyNumber = policyNumber,
    policyValidUntil = policyValidUntil,
    photoUri = photoUri,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun FuelLogEntity.toDomain() = FuelLog(
    id = id,
    motorcycleId = motorcycleId,
    date = date,
    odometerKm = odometerKm,
    liters = liters,
    efficiencyKmL = efficiencyKmL,
    distanceKm = distanceKm,
    notes = notes
)

fun FuelLog.toEntity() = FuelLogEntity(
    id = id,
    motorcycleId = motorcycleId,
    date = date,
    odometerKm = odometerKm,
    liters = liters,
    efficiencyKmL = efficiencyKmL,
    distanceKm = distanceKm,
    notes = notes
)

fun MaintenanceRecordEntity.toDomain() = MaintenanceRecord(
    id = id,
    motorcycleId = motorcycleId,
    type = type,
    date = date,
    odometerKm = odometerKm,
    cost = cost,
    workshop = workshop,
    notes = notes
)

fun MaintenanceRecord.toEntity() = MaintenanceRecordEntity(
    id = id,
    motorcycleId = motorcycleId,
    type = type,
    date = date,
    odometerKm = odometerKm,
    cost = cost,
    workshop = workshop,
    notes = notes
)

fun ServiceScheduleEntity.toDomain() = ServiceSchedule(
    id = id,
    motorcycleId = motorcycleId,
    type = type,
    intervalKm = intervalKm,
    lastServiceKm = lastServiceKm,
    enabled = enabled
)

fun ServiceSchedule.toEntity() = ServiceScheduleEntity(
    id = id,
    motorcycleId = motorcycleId,
    type = type,
    intervalKm = intervalKm,
    lastServiceKm = lastServiceKm,
    enabled = enabled
)

fun RouteRecordEntity.toDomain() = RouteRecord(
    id = id,
    motorcycleId = motorcycleId,
    name = name,
    status = runCatching { RouteStatus.valueOf(status) }.getOrDefault(RouteStatus.COMPLETED),
    startDate = startDate,
    endDate = endDate,
    odoStartKm = odoStartKm,
    odoEndKm = odoEndKm,
    distanceKm = distanceKm,
    estimatedFuelLiters = estimatedFuelLiters,
    notes = notes
)

fun RouteRecord.toEntity() = RouteRecordEntity(
    id = id,
    motorcycleId = motorcycleId,
    name = name,
    status = status.name,
    startDate = startDate,
    endDate = endDate,
    odoStartKm = odoStartKm,
    odoEndKm = odoEndKm,
    distanceKm = distanceKm,
    estimatedFuelLiters = estimatedFuelLiters,
    notes = notes
)

fun MotoManualEntity.toDomain() = MotoManual(
    id = id,
    motorcycleId = motorcycleId,
    title = title,
    fileName = fileName,
    mimeType = mimeType,
    sizeBytes = sizeBytes,
    localUri = localUri,
    addedAt = addedAt
)

fun MotoManual.toEntity() = MotoManualEntity(
    id = id,
    motorcycleId = motorcycleId,
    title = title,
    fileName = fileName,
    mimeType = mimeType,
    sizeBytes = sizeBytes,
    localUri = localUri,
    addedAt = addedAt
)
