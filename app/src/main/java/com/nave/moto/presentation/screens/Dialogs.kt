package com.nave.moto.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nave.moto.domain.model.AlertLevel
import com.nave.moto.domain.model.RouteRecord
import com.nave.moto.domain.rules.scheduleLabel
import com.nave.moto.presentation.components.*
import com.nave.moto.presentation.theme.LocalEs
import com.nave.moto.presentation.theme.tr
import com.nave.moto.presentation.viewmodel.NaveUiState
import java.time.LocalDate

private fun String.dec() = replace(',', '.').toDoubleOrNull()
private fun badDate(d: String) = runCatching { LocalDate.parse(d) }.isFailure

@Composable
fun FuelDialog(ui: NaveUiState, onDismiss: () -> Unit, onSave: (Int?, Double?, String, String) -> String?) {
    var odo by remember { mutableStateOf((ui.stats.totalOdometerKm + 100).toString()) }
    var liters by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var notes by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val invalidDate = tr("Fecha inválida.", "Invalid date.")
    val preview = run {
        val o = odo.toIntOrNull(); val l = liters.dec()
        if (o != null && l != null && l > 0 && o > ui.stats.totalOdometerKm) com.nave.moto.domain.rules.computeFuelRefillEfficiency(ui.stats.totalOdometerKm, o, l) else null
    }
    NaveModal(tr("Cargar gasolina", "Log fuel"), onDismiss) {
        Text("${tr("Odómetro actual:", "Current odometer:")} ${fmt(ui.stats.totalOdometerKm)} km", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        val base = ui.stats.totalOdometerKm
        NumberStepper(tr("Odómetro (km)", "Odometer (km)"), odo, { odo = it }, 10.0,
            presets = listOf(50, 100, 150, 200, 300).map { "+$it" to (base + it).toDouble() })
        NumberStepper(tr("Litros", "Liters"), liters, { liters = it }, 0.5, decimal = true,
            presets = listOf(2.0, 4.0, 6.0, 8.0, 10.0).map { fmt1(it) + " L" to it })
        DateField(tr("Fecha de la recarga", "Refill date"), date, { date = it })
        NaveField(notes, { notes = it }, tr("Notas (opcional)", "Notes (optional)"))
        if (preview != null) Text("+${preview.first} km · ${fmt1(preview.second)} km/L", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        ErrorText(error)
        ModalActions(onDismiss, tr("Guardar", "Save")) {
            error = if (badDate(date)) invalidDate else onSave(odo.toIntOrNull(), liters.dec(), date, notes)
            if (error == null) onDismiss()
        }
    }
}

@Composable
fun OdoDialog(current: Int, onDismiss: () -> Unit, onSave: (Int?) -> String?) {
    var odo by remember { mutableStateOf(current.toString()) }
    var error by remember { mutableStateOf<String?>(null) }
    NaveModal(tr("Ajustar odómetro", "Edit odometer"), onDismiss) {
        NumberStepper("km", odo, { odo = it }, 10.0, presets = listOf(-100, -10, 10, 100).map { (if (it > 0) "+$it" else "$it") to (current + it).toDouble() })
        ErrorText(error)
        ModalActions(onDismiss, tr("Guardar", "Save")) { error = onSave(odo.toIntOrNull()); if (error == null) onDismiss() }
    }
}

@Composable
fun MaintenanceDialog(ui: NaveUiState, onDismiss: () -> Unit, onSave: (String, Int?, String, Double?, String, String, Long?) -> String?) {
    val es = LocalEs.current
    var type by remember { mutableStateOf("") }
    var scheduleId by remember { mutableStateOf<Long?>(null) }
    var odo by remember { mutableStateOf(ui.stats.totalOdometerKm.toString()) }
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var cost by remember { mutableStateOf("0") }
    var workshop by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val invalidDate = tr("Fecha inválida.", "Invalid date.")
    NaveModal(tr("Registrar servicio", "Log service"), onDismiss) {
        if (ui.schedules.isNotEmpty()) {
            Text(tr("Programa preventivo", "Preventive schedule"), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ui.schedules.forEach { s ->
                    NavePill(scheduleLabel(s.type, es), scheduleId == s.id, {
                        if (scheduleId == s.id) { scheduleId = null; type = "" } else { scheduleId = s.id; type = s.type }
                    })
                }
            }
        }
        NaveField(if (scheduleId != null) scheduleLabel(type, es) else type, { type = it; scheduleId = null }, tr("Tipo de servicio", "Service type"), readOnly = scheduleId != null)
        NumberStepper("km", odo, { odo = it }, 100.0)
        NumberStepper(tr("Costo ($)", "Cost ($)"), cost, { cost = it }, 5.0, decimal = true,
            presets = listOf(0.0, 10.0, 20.0, 50.0, 100.0).map { "$" + fmt1(it) to it })
        DateField(tr("Fecha del servicio", "Service date"), date, { date = it })
        NaveField(workshop, { workshop = it }, tr("Taller (opcional)", "Workshop (optional)"))
        NaveField(notes, { notes = it }, tr("Notas (opcional)", "Notes (optional)"))
        ErrorText(error)
        ModalActions(onDismiss, tr("Guardar", "Save")) {
            error = if (badDate(date)) invalidDate else onSave(type, odo.toIntOrNull(), date, cost.dec(), workshop, notes, scheduleId)
            if (error == null) onDismiss()
        }
    }
}

@Composable
fun FinishRouteDialog(route: RouteRecord, gpsKm: Double?, onDismiss: () -> Unit, onSave: (Int?, String) -> String?) {
    // Con GPS, el odómetro final se propone como inicio + km medidos
    val suggested = route.odoStartKm + (gpsKm?.let { Math.round(it).toInt() } ?: 0)
    var odo by remember { mutableStateOf(suggested.toString()) }
    var notes by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    NaveModal(tr("Finalizar ruta", "Finish route"), onDismiss) {
        Text("${route.name} · ${tr("inicio", "start")} ${fmt(route.odoStartKm)} km", style = MaterialTheme.typography.bodySmall)
        if (gpsKm != null) Text(tr("GPS midió ${fmt1(Math.round(gpsKm * 10) / 10.0)} km. Ajusta si tu odómetro marca otra cosa.", "GPS measured ${fmt1(Math.round(gpsKm * 10) / 10.0)} km. Adjust if your odometer differs."),
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        NumberStepper(tr("Odómetro final (km)", "End odometer (km)"), odo, { odo = it }, 1.0, min = route.odoStartKm.toDouble())
        NaveField(notes, { notes = it }, tr("Notas (opcional)", "Notes (optional)"))
        ErrorText(error)
        ModalActions(onDismiss, tr("Finalizar", "Finish")) { error = onSave(odo.toIntOrNull(), notes); if (error == null) onDismiss() }
    }
}

@Composable
fun AlertsDialog(ui: NaveUiState, onDismiss: () -> Unit) {
    val es = LocalEs.current
    val list = ui.alerts.filter { it.level != AlertLevel.OK }
    NaveModal(tr("Alertas de taller", "Service alerts"), onDismiss) {
        if (list.isEmpty()) Text(tr("Todo al día. Sin servicios vencidos ni próximos.", "All good. Nothing due or coming up."), color = MaterialTheme.colorScheme.onSurfaceVariant)
        list.forEach { a ->
            val (c, label) = alertColors(a.level)
            Column {
                Text(scheduleLabel(a.schedule.type, es), style = MaterialTheme.typography.titleSmall)
                Text("$label · " + if (a.remainingKm <= 0) "${tr("pasado por", "overdue by")} ${fmt(-a.remainingKm)} km" else "${tr("faltan", "in")} ${fmt(a.remainingKm)} km (${tr("a los", "at")} ${fmt(a.nextKm)} km)",
                    style = MaterialTheme.typography.bodySmall, color = c)
            }
        }
        ui.policyDaysLeft?.takeIf { it <= 30 }?.let { d ->
            Text(if (d < 0) tr("Póliza de seguro vencida.", "Insurance policy expired.") else tr("Póliza de seguro vence en $d días.", "Insurance policy expires in $d days."), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleSmall)
        }
    }
}
