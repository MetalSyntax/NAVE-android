package com.nave.moto.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.nave.moto.data.local.CatalogDao
import com.nave.moto.data.local.dao.FuelLogDao
import com.nave.moto.data.local.dao.MaintenanceDao
import com.nave.moto.data.local.dao.MotoManualDao
import com.nave.moto.data.local.dao.MotorcycleDao
import com.nave.moto.data.local.dao.RouteDao
import com.nave.moto.data.local.dao.ServiceScheduleDao
import com.nave.moto.data.mapper.toDomain
import com.nave.moto.data.mapper.toEntity
import com.nave.moto.domain.model.AppSettings
import com.nave.moto.domain.model.CatalogModel
import com.nave.moto.domain.model.CatalogSpec
import com.nave.moto.domain.rules.DEFAULT_SCHEDULES
import com.nave.moto.domain.rules.normalizeFontScale
import com.nave.moto.domain.model.FuelLog
import com.nave.moto.domain.model.MaintenanceRecord
import com.nave.moto.domain.model.MotoManual
import com.nave.moto.domain.model.Motorcycle
import com.nave.moto.domain.model.RouteRecord
import com.nave.moto.domain.model.ServiceSchedule
import com.nave.moto.domain.repository.MotoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MotoRepositoryImpl @Inject constructor(
    private val motorcycleDao: MotorcycleDao,
    private val fuelLogDao: FuelLogDao,
    private val maintenanceDao: MaintenanceDao,
    private val scheduleDao: ServiceScheduleDao,
    private val routeDao: RouteDao,
    private val manualDao: MotoManualDao,
    private val catalogDao: CatalogDao,
    private val dataStore: DataStore<Preferences>
) : MotoRepository {

    private object PrefKeys {
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val LANGUAGE = stringPreferencesKey("language")
        val OIL_INTERVAL = intPreferencesKey("oil_interval_km")
        val ACTIVE_MOTO_ID = longPreferencesKey("active_motorcycle_id")
        val RIDER_NAME = stringPreferencesKey("rider_name")
        val ONBOARDING = booleanPreferencesKey("onboarding_complete")
        val HIGH_CONTRAST = booleanPreferencesKey("high_contrast")
        val FONT_SCALE = floatPreferencesKey("font_scale")
        val HAPTICS = booleanPreferencesKey("haptics")
        val GPS_TRACKING = booleanPreferencesKey("gps_tracking")
        val GPS_INTERVAL = intPreferencesKey("gps_interval_sec")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on_route")
        val RIDER_ID = stringPreferencesKey("rider_id")
        val RIDER_PHONE = stringPreferencesKey("rider_phone")
        val BLOOD = stringPreferencesKey("rider_blood")
        val EMERG_NAME = stringPreferencesKey("emerg_name")
        val EMERG_PHONE = stringPreferencesKey("emerg_phone")
        val LIC_NUM = stringPreferencesKey("lic_num")
        val LIC_EXP = stringPreferencesKey("lic_exp")
        val RIDER_PHOTO = stringPreferencesKey("rider_photo")
    }

    override fun observeMotorcycles(): Flow<List<Motorcycle>> =
        motorcycleDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeFuelLogs(motorcycleId: Long): Flow<List<FuelLog>> =
        fuelLogDao.observeByMoto(motorcycleId).map { list -> list.map { it.toDomain() } }

    override fun observeMaintenanceRecords(motorcycleId: Long): Flow<List<MaintenanceRecord>> =
        maintenanceDao.observeByMoto(motorcycleId).map { list -> list.map { it.toDomain() } }

    override fun observeServiceSchedules(motorcycleId: Long): Flow<List<ServiceSchedule>> =
        scheduleDao.observeByMoto(motorcycleId).map { list -> list.map { it.toDomain() } }

    override fun observeRoutes(motorcycleId: Long): Flow<List<RouteRecord>> =
        routeDao.observeByMoto(motorcycleId).map { list -> list.map { it.toDomain() } }

    override fun observeManuals(): Flow<List<MotoManual>> =
        manualDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeSettings(): Flow<AppSettings> =
        dataStore.data.map { prefs ->
            AppSettings(
                darkTheme = prefs[PrefKeys.DARK_THEME] ?: true,
                language = prefs[PrefKeys.LANGUAGE] ?: "es",
                oilIntervalKm = prefs[PrefKeys.OIL_INTERVAL] ?: 3000,
                activeMotorcycleId = prefs[PrefKeys.ACTIVE_MOTO_ID],
                riderName = prefs[PrefKeys.RIDER_NAME] ?: "Piloto NAVE",
                onboardingComplete = prefs[PrefKeys.ONBOARDING] ?: true,
                highContrast = prefs[PrefKeys.HIGH_CONTRAST] ?: false,
                fontScale = normalizeFontScale(prefs[PrefKeys.FONT_SCALE]),
                haptics = prefs[PrefKeys.HAPTICS] ?: true,
                gpsTracking = prefs[PrefKeys.GPS_TRACKING] ?: true,
                gpsIntervalSec = prefs[PrefKeys.GPS_INTERVAL] ?: 3,
                keepScreenOnRoute = prefs[PrefKeys.KEEP_SCREEN_ON] ?: true,
                riderIdNumber = prefs[PrefKeys.RIDER_ID] ?: "", riderPhone = prefs[PrefKeys.RIDER_PHONE] ?: "",
                bloodType = prefs[PrefKeys.BLOOD] ?: "", emergencyName = prefs[PrefKeys.EMERG_NAME] ?: "",
                emergencyPhone = prefs[PrefKeys.EMERG_PHONE] ?: "", licenseNumber = prefs[PrefKeys.LIC_NUM] ?: "",
                licenseExpiry = prefs[PrefKeys.LIC_EXP] ?: "", riderPhoto = prefs[PrefKeys.RIDER_PHOTO]
            )
        }

    override suspend fun upsertMotorcycle(motorcycle: Motorcycle): Long =
        motorcycleDao.upsert(motorcycle.toEntity())

    override suspend fun deleteMotorcycle(motorcycleId: Long) =
        motorcycleDao.deleteById(motorcycleId)

    override suspend fun updateOdometer(motorcycleId: Long, newOdometerKm: Int) =
        motorcycleDao.updateOdometer(motorcycleId, newOdometerKm, LocalDate.now().toString())

    override suspend fun updateFuelLevel(motorcycleId: Long, level: Int) =
        motorcycleDao.updateFuelLevel(motorcycleId, level.coerceIn(1, 5), LocalDate.now().toString())

    override suspend fun insertFuelLog(log: FuelLog): Long =
        fuelLogDao.insert(log.toEntity())

    override suspend fun deleteFuelLog(logId: Long) =
        fuelLogDao.deleteById(logId)

    override suspend fun insertMaintenanceRecord(record: MaintenanceRecord): Long =
        maintenanceDao.insert(record.toEntity())

    override suspend fun deleteMaintenanceRecord(recordId: Long) =
        maintenanceDao.deleteById(recordId)

    override suspend fun upsertServiceSchedule(schedule: ServiceSchedule): Long =
        scheduleDao.upsert(schedule.toEntity())

    override suspend fun deleteServiceSchedule(scheduleId: Long) =
        scheduleDao.deleteById(scheduleId)

    override suspend fun upsertRoute(route: RouteRecord): Long =
        routeDao.upsert(route.toEntity())

    override suspend fun deleteRoute(routeId: Long) =
        routeDao.deleteById(routeId)

    override suspend fun insertManual(manual: MotoManual): Long =
        manualDao.insert(manual.toEntity())

    override suspend fun deleteManual(manualId: Long) =
        manualDao.deleteById(manualId)

    override suspend fun addMotorcycleWithDefaults(motorcycle: Motorcycle): Long {
        val id = motorcycleDao.upsert(motorcycle.toEntity())
        DEFAULT_SCHEDULES.forEach { (type, interval) ->
            scheduleDao.upsert(ServiceSchedule(motorcycleId = id, type = type, intervalKm = interval,
                lastServiceKm = motorcycle.currentOdometerKm).toEntity())
        }
        setActiveMotorcycle(id)
        return id
    }

    override suspend fun markScheduleDone(scheduleId: Long, km: Int) = scheduleDao.markDone(scheduleId, km)

    override suspend fun getMotorcycle(id: Long): Motorcycle? = motorcycleDao.getById(id)?.toDomain()

    private fun com.nave.moto.data.local.entity.CatalogModelEntity.toDomain() = CatalogModel(
        id, brand, name, category, cc, tankL, kmL, maxSpeedKmh, weightKg, colors, hasThumb == 1
    )

    override suspend fun catalogBrands() = catalogDao.brands()
    override suspend fun catalogModels(brand: String) = catalogDao.modelsOf(brand).map { it.toDomain() }
    override suspend fun catalogSearch(query: String) = catalogDao.search(query.trim()).map { it.toDomain() }
    override suspend fun catalogSpecs(modelId: Long) = catalogDao.specs(modelId).map { CatalogSpec(it.section, it.body) }
    override suspend fun catalogThumb(modelId: Long) = catalogDao.thumb(modelId)

    override suspend fun setActiveMotorcycle(motorcycleId: Long) {
        dataStore.edit { prefs -> prefs[PrefKeys.ACTIVE_MOTO_ID] = motorcycleId }
    }

    override suspend fun updateSettings(settings: AppSettings) {
        dataStore.edit { prefs ->
            prefs[PrefKeys.DARK_THEME] = settings.darkTheme
            prefs[PrefKeys.LANGUAGE] = settings.language
            prefs[PrefKeys.OIL_INTERVAL] = settings.oilIntervalKm
            settings.activeMotorcycleId?.let { prefs[PrefKeys.ACTIVE_MOTO_ID] = it }
            prefs[PrefKeys.RIDER_NAME] = settings.riderName
            prefs[PrefKeys.ONBOARDING] = settings.onboardingComplete
            prefs[PrefKeys.HIGH_CONTRAST] = settings.highContrast
            prefs[PrefKeys.FONT_SCALE] = settings.fontScale
            prefs[PrefKeys.HAPTICS] = settings.haptics
            prefs[PrefKeys.GPS_TRACKING] = settings.gpsTracking
            prefs[PrefKeys.GPS_INTERVAL] = settings.gpsIntervalSec
            prefs[PrefKeys.KEEP_SCREEN_ON] = settings.keepScreenOnRoute
            prefs[PrefKeys.RIDER_ID] = settings.riderIdNumber
            prefs[PrefKeys.RIDER_PHONE] = settings.riderPhone
            prefs[PrefKeys.BLOOD] = settings.bloodType
            prefs[PrefKeys.EMERG_NAME] = settings.emergencyName
            prefs[PrefKeys.EMERG_PHONE] = settings.emergencyPhone
            prefs[PrefKeys.LIC_NUM] = settings.licenseNumber
            prefs[PrefKeys.LIC_EXP] = settings.licenseExpiry
            settings.riderPhoto?.let { prefs[PrefKeys.RIDER_PHOTO] = it } ?: prefs.remove(PrefKeys.RIDER_PHOTO)
        }
    }
}
