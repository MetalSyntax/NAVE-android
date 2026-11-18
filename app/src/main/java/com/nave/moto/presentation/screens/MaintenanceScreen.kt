package com.nave.moto.presentation.screens

import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nave.moto.domain.model.AlertLevel
import com.nave.moto.domain.model.MaintenanceRecord
import com.nave.moto.domain.model.ServiceAlert
import com.nave.moto.domain.rules.scheduleLabel
import com.nave.moto.presentation.components.*
import com.nave.moto.presentation.theme.LocalEs
import com.nave.moto.presentation.theme.Outfit
import com.nave.moto.presentation.theme.tr
import com.nave.moto.presentation.viewmodel.NaveUiState

@Composable
fun alertColors(level: AlertLevel): Pair<androidx.compose.ui.graphics.Color, String> {
    val cs = MaterialTheme.colorScheme
    return when (level) {
        AlertLevel.DUE -> cs.error to tr("Vencido", "Overdue")
        AlertLevel.SOON -> cs.secondary to tr("Pronto", "Soon")
        AlertLevel.OK -> cs.onSurfaceVariant to tr("Al día", "OK")
    }
}

@Composable
fun MaintenanceScreen(
    ui: NaveUiState,
    onAddMaintenance: () -> Unit,
    onMarkServiced: (com.nave.moto.domain.model.ServiceSchedule) -> Unit,
    onToggleSchedule: (com.nave.moto.domain.model.ServiceSchedule) -> Unit,
    onAddSchedule: (String, Int?) -> String?,
    onDeleteSchedule: (Long) -> Unit,
    onDeleteMaintenance: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    val es = LocalEs.current
    var newType by rememberSaveable { mutableStateOf("") }
    var newInterval by rememberSaveable { mutableStateOf("4000") }
    var error by remember { mutableStateOf<String?>(null) }
    var delSched by remember { mutableStateOf<ServiceAlert?>(null) }
    var delRec by remember { mutableStateOf<MaintenanceRecord?>(null) }
    val moto = ui.activeMoto

    Column(modifier.verticalScrollColumn(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ScreenTitle(tr("Taller y mantenimiento preventivo", "Workshop & preventive service"),
            (moto?.let { "${it.brand} ${it.model}" } ?: "NAVE") + " · ${fmt(ui.stats.totalOdometerKm)} km")
        if (moto == null) {
            EmptyCard(Icons.Default.TwoWheeler, tr("Agrega una moto en el Garaje para empezar.", "Add a motorcycle in the Garage to start."))
        } else {
        Text(tr("Programas preventivos", "Preventive schedules"), Modifier.semantics { heading() }, style = MaterialTheme.typography.titleLarge)
        if (ui.schedules.isEmpty()) EmptyCard(Icons.Default.Build, tr("No hay programas activos para esta moto.", "No active schedules for this motorcycle."))
        val alertsById = ui.alerts.associateBy { it.schedule.id }
        ui.schedules.forEach { s ->
            val a = alertsById[s.id]
            NaveCard(container = cs.surfaceContainer, shape = MaterialTheme.shapes.medium, padding = 16.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(scheduleLabel(s.type, es), style = MaterialTheme.typography.titleMedium)
                        if (a != null) {
                            val (c, label) = alertColors(a.level)
                            val rem = if (a.remainingKm <= 0) "${tr("pasado por", "overdue by")} ${fmt(-a.remainingKm)} km" else "${tr("faltan", "in")} ${fmt(a.remainingKm)} km"
                            Text("$label · $rem · ${tr("cada", "every")} ${fmt(s.intervalKm)} km", style = MaterialTheme.typography.labelMedium, color = c)
                        } else Text(tr("Desactivado", "Disabled"), style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
                    }
                    Switch(s.enabled, { onToggleSchedule(s) }, Modifier.semantics { contentDescription = scheduleLabel(s.type, es) })
                }
                Text("${tr("Último:", "Last:")} ${fmt(s.lastServiceKm)} km · ${tr("Próximo:", "Next:")} ${fmt(s.lastServiceKm + s.intervalKm)} km",
                    style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                if (a != null && a.level != AlertLevel.OK) {
                    val p = ((1f - (a.remainingKm.coerceAtLeast(0).toFloat() / s.intervalKm)).coerceIn(0f, 1f))
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator({ p }, Modifier.fillMaxWidth().height(6.dp).clip(MaterialTheme.shapes.extraSmall),
                        color = alertColors(a.level).first, trackColor = cs.surfaceContainerHigh)
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TonalButton(tr("Marcar hecho", "Mark done"), { onMarkServiced(s) }, icon = Icons.Default.Check)
                    SquareIconButton(Icons.Default.Delete, tr("Eliminar", "Delete"), { delSched = a ?: ServiceAlert(s, 0, 0, AlertLevel.OK) })
                }
            }
        }

        NaveCard(container = cs.surfaceContainerLow, padding = 16.dp) {
            Text(tr("Nuevo recordatorio", "New schedule"), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            NaveField(newType, { newType = it }, tr("Componente o servicio", "Component or service"), placeholder = tr("Ej. Bujía / Filtro de aire", "e.g. Spark plug"))
            Spacer(Modifier.height(8.dp))
            NumberStepper(tr("Intervalo (km)", "Interval (km)"), newInterval, { newInterval = it }, 500.0, min = 500.0,
                presets = listOf(2000, 4000, 6000, 8000, 12000).map { fmt(it) to it.toDouble() })
            ErrorText(error)
            Spacer(Modifier.height(10.dp))
            PrimaryButton(tr("Agregar programa", "Add schedule"), {
                error = onAddSchedule(newType, newInterval.toIntOrNull())
                if (error == null) { newType = ""; newInterval = "4000" }
            }, Modifier.fillMaxWidth())
        }

        Text(tr("Historial de taller (${ui.maintenance.size})", "Service history (${ui.maintenance.size})"), style = MaterialTheme.typography.titleLarge)
        if (ui.maintenance.isEmpty()) EmptyCard(Icons.Default.Build, tr("Sin servicios registrados aún.", "No service records yet."))
        ui.maintenance.forEach { r ->
            Row(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(cs.surfaceContainerLow).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(scheduleLabel(r.type, es), style = MaterialTheme.typography.titleSmall)
                    Text("${r.date} · ${r.workshop.ifBlank { tr("Taller personal", "Personal garage") }} · $${fmt1(r.cost)}", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                    if (r.notes.isNotBlank()) Text(r.notes, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                }
                Text("${fmt(r.odometerKm)} km", style = MaterialTheme.typography.titleSmall.copy(fontFamily = Outfit, fontWeight = FontWeight.Black), color = cs.secondary)
                SquareIconButton(Icons.Default.Delete, tr("Eliminar", "Delete"), { delRec = r })
            }
        }
        }
    }
    delSched?.let { a -> ConfirmDialog(tr("¿Eliminar este recordatorio?", "Delete this schedule?"), tr("Eliminar", "Delete"), { onDeleteSchedule(a.schedule.id) }, { delSched = null }) }
    delRec?.let { r -> ConfirmDialog(tr("¿Eliminar este servicio del historial?", "Delete this service record?"), tr("Eliminar", "Delete"), { onDeleteMaintenance(r.id) }, { delRec = null }) }
}
