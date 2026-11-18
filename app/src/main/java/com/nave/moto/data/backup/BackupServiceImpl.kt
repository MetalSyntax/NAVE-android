package com.nave.moto.data.backup

import androidx.room.withTransaction
import com.nave.moto.data.local.MotoDatabase
import com.nave.moto.data.mapper.toDomain
import com.nave.moto.data.mapper.toEntity
import com.nave.moto.domain.model.*
import com.nave.moto.domain.repository.BackupService
import com.nave.moto.domain.repository.MotoRepository
import com.nave.moto.domain.rules.backupRelPaths
import com.nave.moto.domain.rules.isRestorableEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDateTime
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupServiceImpl @Inject constructor(
    private val db: MotoDatabase,
    private val repo: MotoRepository,
    private val files: FileStorage
) : BackupService {

    private fun JSONObject.optStr(k: String): String? = if (isNull(k) || !has(k)) null else getString(k)

    override suspend fun export(out: OutputStream) = withContext(Dispatchers.IO) {
        val motos = db.motorcycleDao().getAll().map { it.toDomain() }
        val fuel = db.fuelLogDao().getAll().map { it.toDomain() }
        val maint = db.maintenanceDao().getAll().map { it.toDomain() }
        val sched = db.serviceScheduleDao().getAll().map { it.toDomain() }
        val routes = db.routeDao().getAll().map { it.toDomain() }
        val manuals = db.motoManualDao().getAll().map { it.toDomain() }
        val st = repo.observeSettings().first()

        val root = JSONObject().put("app", "NAVE").put("version", 1)
            .put("exportedAt", LocalDateTime.now().toString())
            .put("settings", JSONObject().put("darkTheme", st.darkTheme).put("language", st.language)
                .put("oilIntervalKm", st.oilIntervalKm).put("riderName", st.riderName)
                .put("activeMotorcycleId", st.activeMotorcycleId ?: JSONObject.NULL))
            .put("profile", JSONObject().put("idNumber", st.riderIdNumber).put("phone", st.riderPhone)
                .put("bloodType", st.bloodType).put("emergencyName", st.emergencyName).put("emergencyPhone", st.emergencyPhone)
                .put("licenseNumber", st.licenseNumber).put("licenseExpiry", st.licenseExpiry)
                .put("photo", st.riderPhoto ?: JSONObject.NULL))
            .put("motorcycles", JSONArray(motos.map { m -> JSONObject()
                .put("id", m.id).put("brand", m.brand).put("model", m.model).put("year", m.year)
                .put("category", m.category).put("cc", m.cc).put("color", m.color).put("plate", m.plate)
                .put("vin", m.vin).put("fuelLevel", m.fuelLevel).put("tankCapacityLiters", m.tankCapacityLiters)
                .put("efficiencyKmPerLiter", m.efficiencyKmPerLiter).put("currentOdometerKm", m.currentOdometerKm)
                .put("insurer", m.insurer).put("policyNumber", m.policyNumber).put("policyValidUntil", m.policyValidUntil)
                .put("photoUri", m.photoUri ?: JSONObject.NULL).put("createdAt", m.createdAt).put("updatedAt", m.updatedAt) }))
            .put("fuelLogs", JSONArray(fuel.map { l -> JSONObject()
                .put("id", l.id).put("motorcycleId", l.motorcycleId).put("date", l.date).put("odometerKm", l.odometerKm)
                .put("liters", l.liters).put("efficiencyKmL", l.efficiencyKmL).put("distanceKm", l.distanceKm).put("notes", l.notes) }))
            .put("maintenance", JSONArray(maint.map { r -> JSONObject()
                .put("id", r.id).put("motorcycleId", r.motorcycleId).put("type", r.type).put("date", r.date)
                .put("odometerKm", r.odometerKm).put("cost", r.cost).put("workshop", r.workshop).put("notes", r.notes) }))
            .put("schedules", JSONArray(sched.map { s -> JSONObject()
                .put("id", s.id).put("motorcycleId", s.motorcycleId).put("type", s.type)
                .put("intervalKm", s.intervalKm).put("lastServiceKm", s.lastServiceKm).put("enabled", s.enabled) }))
            .put("routes", JSONArray(routes.map { r -> JSONObject()
                .put("id", r.id).put("motorcycleId", r.motorcycleId).put("name", r.name).put("status", r.status.name)
                .put("startDate", r.startDate).put("endDate", r.endDate ?: JSONObject.NULL)
                .put("odoStartKm", r.odoStartKm).put("odoEndKm", r.odoEndKm ?: JSONObject.NULL)
                .put("distanceKm", r.distanceKm).put("estimatedFuelLiters", r.estimatedFuelLiters).put("notes", r.notes) }))
            .put("manuals", JSONArray(manuals.map { m -> JSONObject()
                .put("id", m.id).put("motorcycleId", m.motorcycleId ?: JSONObject.NULL).put("title", m.title)
                .put("fileName", m.fileName).put("mimeType", m.mimeType).put("sizeBytes", m.sizeBytes)
                .put("localUri", m.localUri).put("addedAt", m.addedAt) }))

        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry("data.json")); zip.write(root.toString(2).toByteArray()); zip.closeEntry()
            // Incluye el CSV de GPS de cada ruta (tracks/route-<id>.csv) si existe
            backupRelPaths(motos.map { it.photoUri }, manuals.map { it.localUri }, routes.map { it.id }, st.riderPhoto).forEach { rel ->
                val f = files.file(rel)
                if (f.exists()) { zip.putNextEntry(ZipEntry("files/$rel")); f.inputStream().use { it.copyTo(zip) }; zip.closeEntry() }
            }
        }
    }

    override suspend fun import(input: InputStream): String = withContext(Dispatchers.IO) {
        var json: String? = null
        val extracted = mutableListOf<String>()
        ZipInputStream(input).use { zip ->
            var e = zip.nextEntry
            while (e != null) {
                if (e.name == "data.json") json = zip.readBytes().toString(Charsets.UTF_8)
                else if (isRestorableEntry(e.name)) {
                    val rel = e.name.removePrefix("files/")
                    val dest = files.file(rel)
                    // Evita path traversal ("../")
                    if (dest.canonicalPath.startsWith(files.root.canonicalPath + "/")) {
                        dest.parentFile?.mkdirs(); dest.outputStream().use { zip.copyTo(it) }; extracted += rel
                    }
                }
                e = zip.nextEntry
            }
        }
        val root = JSONObject(json ?: error("El archivo no contiene data.json (¿es un respaldo NAVE?)"))
        require(root.optString("app") == "NAVE") { "No es un respaldo de NAVE." }

        fun JSONArray.objs() = (0 until length()).map { getJSONObject(it) }
        val motos = root.getJSONArray("motorcycles").objs().map { o -> Motorcycle(
            id = o.getLong("id"), brand = o.getString("brand"), model = o.getString("model"), year = o.getInt("year"),
            category = o.getString("category"), cc = o.getInt("cc"), color = o.getString("color"), plate = o.getString("plate"),
            vin = o.optString("vin"), fuelLevel = o.getInt("fuelLevel"), tankCapacityLiters = o.getDouble("tankCapacityLiters"),
            efficiencyKmPerLiter = o.getDouble("efficiencyKmPerLiter"), currentOdometerKm = o.getInt("currentOdometerKm"),
            insurer = o.optString("insurer"), policyNumber = o.optString("policyNumber"),
            policyValidUntil = o.optString("policyValidUntil"), photoUri = o.optStr("photoUri"),
            createdAt = o.getString("createdAt"), updatedAt = o.getString("updatedAt")) }
        val fuel = root.getJSONArray("fuelLogs").objs().map { o -> FuelLog(o.getLong("id"), o.getLong("motorcycleId"),
            o.getString("date"), o.getInt("odometerKm"), o.getDouble("liters"), o.getDouble("efficiencyKmL"),
            o.getInt("distanceKm"), o.optString("notes")) }
        val maint = root.getJSONArray("maintenance").objs().map { o -> MaintenanceRecord(o.getLong("id"),
            o.getLong("motorcycleId"), o.getString("type"), o.getString("date"), o.getInt("odometerKm"),
            o.getDouble("cost"), o.optString("workshop"), o.optString("notes")) }
        val sched = root.getJSONArray("schedules").objs().map { o -> ServiceSchedule(o.getLong("id"),
            o.getLong("motorcycleId"), o.getString("type"), o.getInt("intervalKm"), o.getInt("lastServiceKm"),
            o.getBoolean("enabled")) }
        val routes = root.getJSONArray("routes").objs().map { o -> RouteRecord(o.getLong("id"), o.getLong("motorcycleId"),
            o.getString("name"), RouteStatus.valueOf(o.getString("status")), o.getString("startDate"), o.optStr("endDate"),
            o.getInt("odoStartKm"), if (o.isNull("odoEndKm")) null else o.getInt("odoEndKm"),
            o.getInt("distanceKm"), o.getDouble("estimatedFuelLiters"), o.optString("notes")) }
        val manuals = root.getJSONArray("manuals").objs().map { o -> MotoManual(o.getLong("id"),
            if (o.isNull("motorcycleId")) null else o.getLong("motorcycleId"), o.getString("title"),
            o.getString("fileName"), o.getString("mimeType"), o.getLong("sizeBytes"), o.getString("localUri"),
            o.getString("addedAt")) }

        db.withTransaction {
            db.motorcycleDao().deleteAll() // CASCADE limpia logs/servicios/rutas
            db.motoManualDao().deleteAll()
            motos.forEach { db.motorcycleDao().upsert(it.toEntity()) }
            fuel.forEach { db.fuelLogDao().insert(it.toEntity()) }
            maint.forEach { db.maintenanceDao().insert(it.toEntity()) }
            sched.forEach { db.serviceScheduleDao().upsert(it.toEntity()) }
            routes.forEach { db.routeDao().upsert(it.toEntity()) }
            manuals.forEach { db.motoManualDao().insert(it.toEntity()) }
        }
        val s = root.optJSONObject("settings")
        if (s != null) {
            repo.updateSettings(repo.observeSettings().first().copy(
                darkTheme = s.optBoolean("darkTheme", true), language = s.optString("language", "es"),
                oilIntervalKm = s.optInt("oilIntervalKm", 3000), riderName = s.optString("riderName", "Piloto NAVE"),
                activeMotorcycleId = if (s.isNull("activeMotorcycleId")) motos.firstOrNull()?.id else s.getLong("activeMotorcycleId")))
        }
        root.optJSONObject("profile")?.let { p ->
            repo.updateSettings(repo.observeSettings().first().copy(
                riderIdNumber = p.optString("idNumber"), riderPhone = p.optString("phone"), bloodType = p.optString("bloodType"),
                emergencyName = p.optString("emergencyName"), emergencyPhone = p.optString("emergencyPhone"),
                licenseNumber = p.optString("licenseNumber"), licenseExpiry = p.optString("licenseExpiry"),
                riderPhoto = p.optStr("photo")))
        }
        "${motos.size} motos, ${fuel.size} recargas, ${maint.size} servicios, ${sched.size} recordatorios, ${routes.size} rutas, ${manuals.size} manuales"
    }

    /** Garaje de ejemplo (equivale al «Restaurar ejemplo» de la web). Borra motos, registros y manuales; conserva ajustes y perfil. */
    override suspend fun restoreDemo() = withContext(Dispatchers.IO) {
        val today = java.time.LocalDate.now()
        fun d(ago: Long) = today.minusDays(ago).toString()
        db.withTransaction {
            db.motorcycleDao().deleteAll()
            db.motoManualDao().deleteAll()
        }
        java.io.File(files.root, "tracks").deleteRecursively()
        val bera = repo.addMotorcycleWithDefaults(Motorcycle(brand = "Bera", model = "SBR 150cc", year = 2024, category = "PASEO", cc = 150,
            color = "Negro Mate / Carmesí", plate = "AF482KM", vin = "8X9BR15024R009142", fuelLevel = 4, tankCapacityLiters = 13.0,
            efficiencyKmPerLiter = 42.5, currentOdometerKm = 14820, insurer = "Seguros Horizonte", policyNumber = "RCV-2026-4418",
            policyValidUntil = today.plusMonths(6).toString(), createdAt = d(200), updatedAt = d(2)))
        listOf(Triple(2L, 14820, 4.1), Triple(7L, 14644, 3.9), Triple(13L, 14478, 4.3)).forEach { (ago, odo, l) ->
            repo.insertFuelLog(FuelLog(motorcycleId = bera, date = d(ago), odometerKm = odo, liters = l,
                efficiencyKmL = 42.0, distanceKm = 170, notes = "Recarga completa 91 octanos"))
        }
        repo.insertMaintenanceRecord(MaintenanceRecord(motorcycleId = bera, type = "Cambio de aceite", date = d(40), odometerKm = 13900,
            cost = 18.0, workshop = "Taller Central", notes = "Aceite 20W-50"))
        repo.insertMaintenanceRecord(MaintenanceRecord(motorcycleId = bera, type = "Cadena y relación", date = d(90), odometerKm = 12800,
            cost = 25.0, workshop = "Taller Central"))
        repo.upsertRoute(RouteRecord(motorcycleId = bera, name = "Reparto zona norte", status = RouteStatus.COMPLETED, startDate = d(5),
            endDate = d(5), odoStartKm = 14644, odoEndKm = 14820, distanceKm = 176, estimatedFuelLiters = 4.14))
        repo.addMotorcycleWithDefaults(Motorcycle(brand = "Empire Keeway", model = "RKV 200", year = 2023, category = "NAKED / STREET", cc = 200,
            color = "Grafito / Dorado", plate = "AG910TV", fuelLevel = 3, tankCapacityLiters = 16.0, efficiencyKmPerLiter = 35.0,
            currentOdometerKm = 21450, insurer = "Seguros Caracas", policyNumber = "RCV-2026-7730",
            policyValidUntil = today.plusMonths(3).toString(), createdAt = d(300), updatedAt = d(5)))
        repo.setActiveMotorcycle(bera)
    }
}
