package com.nave.moto.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nave.moto.data.backup.FileStorage
import com.nave.moto.domain.model.*
import com.nave.moto.domain.repository.BackupService
import com.nave.moto.domain.repository.MotoRepository
import com.nave.moto.domain.rules.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDate
import javax.inject.Inject
import kotlin.math.roundToInt

data class NaveUiState(
    val loaded: Boolean = false,
    val settings: AppSettings = AppSettings(),
    val motorcycles: List<Motorcycle> = emptyList(),
    val activeMoto: Motorcycle? = null,
    val fuelLogs: List<FuelLog> = emptyList(),
    val maintenance: List<MaintenanceRecord> = emptyList(),
    val schedules: List<ServiceSchedule> = emptyList(),
    val routes: List<RouteRecord> = emptyList(),
    val manuals: List<MotoManual> = emptyList(),
    val stats: MotoTelemetryStats = MotoTelemetryStats(),
    val alerts: List<ServiceAlert> = emptyList(),
    /** Días hasta vencer la póliza (negativo = vencida); null si no hay fecha válida. */
    val policyDaysLeft: Long? = null
)

private data class MotoData(
    val logs: List<FuelLog>, val maint: List<MaintenanceRecord>,
    val sched: List<ServiceSchedule>, val routes: List<RouteRecord>
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NaveMotoViewModel @Inject constructor(
    private val repo: MotoRepository,
    private val backup: BackupService,
    private val files: FileStorage
) : ViewModel() {

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages

    private fun msg(s: String) { _messages.tryEmit(s) }

    private val activeFlow = combine(repo.observeMotorcycles(), repo.observeSettings()) { motos, st ->
        Triple(motos, st, motos.firstOrNull { it.id == st.activeMotorcycleId } ?: motos.firstOrNull())
    }

    private val perMoto = activeFlow.map { it.third?.id }.distinctUntilChanged().flatMapLatest { id ->
        if (id == null) flowOf(MotoData(emptyList(), emptyList(), emptyList(), emptyList()))
        else combine(
            repo.observeFuelLogs(id), repo.observeMaintenanceRecords(id),
            repo.observeServiceSchedules(id), repo.observeRoutes(id)
        ) { a, b, c, d -> MotoData(a, b, c, d) }
    }

    val uiState: StateFlow<NaveUiState> =
        combine(activeFlow, perMoto, repo.observeManuals()) { (motos, st, moto), data, manuals ->
            val stats = computeTelemetryStats(moto, data.logs, data.maint, data.routes, st.oilIntervalKm,
                oilBaselineKm = data.sched.firstOrNull { it.type == "aceite" }?.lastServiceKm ?: 0)
            val policy = runCatching { java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(moto?.policyValidUntil)) }.getOrNull()
            NaveUiState(
                loaded = true, settings = st, motorcycles = motos, activeMoto = moto,
                fuelLogs = data.logs, maintenance = data.maint, schedules = data.sched, routes = data.routes,
                manuals = manuals, stats = stats,
                alerts = computeServiceAlerts(data.sched, stats.totalOdometerKm),
                policyDaysLeft = policy
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NaveUiState())

    private val st get() = uiState.value
    private val today get() = LocalDate.now().toString()

    // ---- Ajustes ----
    private fun updateSettings(f: (AppSettings) -> AppSettings) = viewModelScope.launch {
        repo.updateSettings(f(st.settings))
    }
    fun toggleTheme() { updateSettings { it.copy(darkTheme = !it.darkTheme) } }
    fun setLanguage(l: String) { updateSettings { it.copy(language = l) } }
    fun setOilInterval(km: Int) { updateSettings { it.copy(oilIntervalKm = km) } }
    fun setRiderName(n: String) { updateSettings { it.copy(riderName = n) } }
    fun setHighContrast(v: Boolean) { updateSettings { it.copy(highContrast = v) } }
    fun setFontScale(v: Float) { updateSettings { it.copy(fontScale = v) } }
    fun setHaptics(v: Boolean) { updateSettings { it.copy(haptics = v) } }
    fun setGpsTracking(v: Boolean) { updateSettings { it.copy(gpsTracking = v) } }
    fun setGpsInterval(s: Int) { updateSettings { it.copy(gpsIntervalSec = s) } }
    fun setKeepScreenOn(v: Boolean) { updateSettings { it.copy(keepScreenOnRoute = v) } }
    /** Guarda el perfil completo del motorizado (modo Editar → Guardar). */
    fun saveProfile(f: (AppSettings) -> AppSettings) { updateSettings(f); msg("Perfil guardado") }
    fun setRiderPhoto(uri: Uri?) {
        viewModelScope.launch {
            val old = st.settings.riderPhoto
            val rel = if (uri == null) null else withContext(Dispatchers.IO) { files.savePortraitFromUri(uri) }
            if (uri != null && rel == null) { msg("No se pudo guardar la foto"); return@launch }
            repo.updateSettings(repo.observeSettings().first().copy(riderPhoto = rel))
            files.delete(old)
        }
    }
    fun restoreDemo() { viewModelScope.launch { runCatching { backup.restoreDemo() }.onSuccess { msg("Garaje de ejemplo restaurado") }.onFailure { msg("Error: ${it.message}") } } }
    fun manualPreview(rel: String): String? = files.previewText(rel)
    fun selectMoto(id: Long) { viewModelScope.launch { repo.setActiveMotorcycle(id) } }

    // ---- Moto ----
    fun addMoto(m: Motorcycle, photoUri: Uri?, thumbCatalogId: Long?): String? {
        validateMotorcycleInput(m.brand, m.model, m.year, m.currentOdometerKm, m.cc)?.let { return it }
        viewModelScope.launch {
            val rel = when {
                photoUri != null -> withContext(Dispatchers.IO) { files.savePhotoFromUri(photoUri) }
                thumbCatalogId != null -> repo.catalogThumb(thumbCatalogId)?.let { files.savePhotoBytes(it) }
                else -> null
            }
            repo.addMotorcycleWithDefaults(m.copy(photoUri = rel, createdAt = today, updatedAt = today))
            msg("${m.brand} ${m.model} agregada")
        }
        return null
    }

    fun updateMoto(m: Motorcycle, newPhoto: Uri?): String? {
        validateMotorcycleInput(m.brand, m.model, m.year, m.currentOdometerKm, m.cc)?.let { return it }
        viewModelScope.launch {
            var photo = m.photoUri
            if (newPhoto != null) {
                withContext(Dispatchers.IO) { files.savePhotoFromUri(newPhoto) }?.let { files.delete(photo); photo = it }
            }
            repo.upsertMotorcycle(m.copy(photoUri = photo, updatedAt = today))
            msg("Moto actualizada")
        }
        return null
    }

    fun deleteMoto(m: Motorcycle) = viewModelScope.launch {
        files.delete(m.photoUri)
        repo.deleteMotorcycle(m.id)
        if (st.settings.activeMotorcycleId == m.id) {
            st.motorcycles.firstOrNull { it.id != m.id }?.let { repo.setActiveMotorcycle(it.id) }
        }
    }

    fun setFuelLevel(level: Int) { st.activeMoto?.let { m -> viewModelScope.launch { repo.updateFuelLevel(m.id, level) } } }

    fun setOdometer(km: Int?): String? {
        val m = st.activeMoto ?: return "Sin moto activa"
        if (km == null || km < 0) return "Odómetro inválido"
        viewModelScope.launch { repo.updateOdometer(m.id, km) }
        return null
    }

    // ---- Combustible ----
    fun logFuel(odo: Int?, liters: Double?, date: String, notes: String): String? {
        val m = st.activeMoto ?: return "Agrega una moto primero"
        val current = st.stats.totalOdometerKm
        validateFuelLogInput(current, odo, liters)?.let { return it }
        val (dist, eff) = computeFuelRefillEfficiency(current, odo!!, liters!!)
        viewModelScope.launch {
            repo.insertFuelLog(FuelLog(motorcycleId = m.id, date = date, odometerKm = odo, liters = liters,
                efficiencyKmL = eff, distanceKm = dist, notes = notes.trim()))
            repo.updateOdometer(m.id, odo)
            msg("Recarga registrada: $eff km/L")
        }
        return null
    }
    fun deleteFuelLog(id: Long) { viewModelScope.launch { repo.deleteFuelLog(id) } }

    // ---- Taller ----
    fun addMaintenance(type: String, odo: Int?, date: String, cost: Double?, workshop: String, notes: String, scheduleId: Long?): String? {
        val m = st.activeMoto ?: return "Agrega una moto primero"
        validateMaintenanceInput(type, odo, cost)?.let { return it }
        viewModelScope.launch {
            repo.insertMaintenanceRecord(MaintenanceRecord(motorcycleId = m.id, type = type.trim(), date = date,
                odometerKm = odo!!, cost = cost!!, workshop = workshop.trim(), notes = notes.trim()))
            if (scheduleId != null) repo.markScheduleDone(scheduleId, odo)
            if (odo > m.currentOdometerKm) repo.updateOdometer(m.id, odo)
            msg("Servicio registrado")
        }
        return null
    }
    fun deleteMaintenance(id: Long) { viewModelScope.launch { repo.deleteMaintenanceRecord(id) } }

    fun markServiced(s: ServiceSchedule) {
        val m = st.activeMoto ?: return
        val km = st.stats.totalOdometerKm
        viewModelScope.launch {
            repo.insertMaintenanceRecord(MaintenanceRecord(motorcycleId = m.id, type = s.type, date = today,
                odometerKm = km, workshop = "Revisión preventiva", notes = "Programa marcado al día"))
            repo.markScheduleDone(s.id, km)
            msg("Programa al día")
        }
    }
    fun addSchedule(type: String, interval: Int?): String? {
        val m = st.activeMoto ?: return "Agrega una moto primero"
        if (type.isBlank()) return "Escribe el nombre del servicio."
        if (interval == null || interval <= 0) return "El intervalo debe ser mayor a 0 km."
        viewModelScope.launch {
            repo.upsertServiceSchedule(ServiceSchedule(motorcycleId = m.id, type = type.trim(), intervalKm = interval,
                lastServiceKm = st.stats.totalOdometerKm))
        }
        return null
    }
    fun toggleSchedule(s: ServiceSchedule) { viewModelScope.launch { repo.upsertServiceSchedule(s.copy(enabled = !s.enabled)) } }
    fun deleteSchedule(id: Long) { viewModelScope.launch { repo.deleteServiceSchedule(id) } }

    // ---- Rutas ----
    /** [onCreated] recibe el id de la ruta nueva (para arrancar el GPS). */
    fun startRoute(name: String, onCreated: (Long) -> Unit = {}) {
        val m = st.activeMoto ?: return
        viewModelScope.launch {
            val id = repo.upsertRoute(RouteRecord(motorcycleId = m.id, name = name.ifBlank { "Ruta urbana" }, status = RouteStatus.ACTIVE,
                startDate = today, odoStartKm = st.stats.totalOdometerKm))
            onCreated(id)
        }
    }
    fun finishRoute(r: RouteRecord, endOdo: Int?, notes: String): String? {
        val m = st.activeMoto ?: return "Sin moto activa"
        if (endOdo == null || endOdo < r.odoStartKm) return "El odómetro final debe ser ≥ ${r.odoStartKm} km."
        val dist = endOdo - r.odoStartKm
        val fuel = if (m.efficiencyKmPerLiter > 0) (dist / m.efficiencyKmPerLiter * 100).roundToInt() / 100.0 else 0.0
        viewModelScope.launch {
            repo.upsertRoute(r.copy(status = RouteStatus.COMPLETED, endDate = today, odoEndKm = endOdo, distanceKm = dist,
                estimatedFuelLiters = fuel, notes = notes.trim()))
            if (endOdo > m.currentOdometerKm) repo.updateOdometer(m.id, endOdo)
            msg("Ruta completada: +$dist km")
        }
        return null
    }
    fun deleteRoute(id: Long) { viewModelScope.launch { repo.deleteRoute(id); files.delete(com.nave.moto.data.tracking.RouteTracker.trackPath(id)) } }

    // ---- Manuales ----
    fun addManual(uri: Uri) {
        viewModelScope.launch {
            val saved = withContext(Dispatchers.IO) { files.saveManual(uri) }
            if (saved == null) { msg("No se pudo guardar el archivo"); return@launch }
            repo.insertManual(MotoManual(motorcycleId = st.activeMoto?.id, title = saved.displayName.substringBeforeLast('.'),
                fileName = saved.displayName, mimeType = saved.mime, sizeBytes = saved.size, localUri = saved.rel, addedAt = today))
            msg("Manual agregado")
        }
    }
    fun deleteManual(m: MotoManual) { viewModelScope.launch { files.delete(m.localUri); repo.deleteManual(m.id) } }

    // ---- Catálogo ----
    suspend fun brands() = repo.catalogBrands()
    suspend fun models(brand: String) = repo.catalogModels(brand)
    suspend fun search(q: String) = repo.catalogSearch(q)
    suspend fun specs(id: Long) = repo.catalogSpecs(id)
    suspend fun thumb(id: Long) = repo.catalogThumb(id)

    // ---- Respaldo ----
    fun exportBackup(out: OutputStream) {
        viewModelScope.launch {
            runCatching { backup.export(out) }.onSuccess { msg("Respaldo exportado") }
                .onFailure { msg("Error al exportar: ${it.message}") }
        }
    }
    fun importBackup(input: InputStream) {
        viewModelScope.launch {
            runCatching { backup.import(input) }.onSuccess { msg("Importado: $it") }
                .onFailure { msg("Error al importar: ${it.message}") }
        }
    }
}
