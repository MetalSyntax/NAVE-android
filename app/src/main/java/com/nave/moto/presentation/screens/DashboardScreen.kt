package com.nave.moto.presentation.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nave.moto.domain.model.AlertLevel
import com.nave.moto.presentation.components.*
import com.nave.moto.presentation.theme.Outfit
import com.nave.moto.presentation.theme.tr
import com.nave.moto.presentation.viewmodel.NaveUiState

@Composable
fun DashboardScreen(
    ui: NaveUiState,
    onSelectMoto: (Long) -> Unit,
    onSetFuelLevel: (Int) -> Unit,
    onOpenFuel: () -> Unit,
    onOpenOdo: () -> Unit,
    onOpenAlerts: () -> Unit,
    onToggleTheme: () -> Unit,
    onGoMaintenance: () -> Unit,
    onGoLogs: () -> Unit,
    onGoGarage: () -> Unit,
    onDeleteFuelLog: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    val moto = ui.activeMoto
    val attention = ui.alerts.count { it.level != AlertLevel.OK }
    Column(modifier.verticalScrollColumn(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        // Selector multi-moto + alertas + tema
        if (ui.motorcycles.isNotEmpty()) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ui.motorcycles.forEach { m -> NavePill("${m.brand} ${m.model}", m.id == moto?.id, { onSelectMoto(m.id) }) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChipAction(Icons.Default.Notifications, "${tr("Alertas taller", "Service alerts")} ($attention)", cs.secondary, onOpenAlerts)
            ChipAction(
                if (ui.settings.darkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                if (ui.settings.darkTheme) tr("Claro", "Light") else tr("Oscuro", "Dark"),
                if (ui.settings.darkTheme) cs.secondary else cs.primary, onToggleTheme
            )
        }

        if (moto == null) {
            NaveCard(Modifier, cs.surfaceContainer, MaterialTheme.shapes.large, 28.dp) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.TwoWheeler, null, Modifier.size(40.dp), tint = cs.primary)
                    Text(tr("Tu garaje está vacío", "Your garage is empty"), style = MaterialTheme.typography.headlineSmall)
                    Text(tr("Elige tu moto del catálogo o créala a mano.", "Pick your bike from the catalog or create it manually."),
                        color = cs.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    PrimaryButton(tr("Agregar motocicleta", "Add motorcycle"), onGoGarage)
                }
            }
        } else {
            // Tarjeta focal de telemetría
            NaveCard(padding = 20.dp) {
                MotoPhoto(moto.photoUri, "${moto.brand} ${moto.model}", Modifier.fillMaxWidth().aspectRatio(4f / 3f))
                Spacer(Modifier.height(14.dp))
                Eyebrow("${moto.category} · ${moto.cc} cc · ${moto.year} · ${moto.plate}")
                Text("${moto.brand} ${moto.model}", style = MaterialTheme.typography.displaySmall, color = cs.onSurface)
                Text("${moto.color} · ${tr("Tanque", "Tank")} ${fmt1(moto.tankCapacityLiters)} L" + if (moto.insurer.isNotBlank()) " · ${moto.insurer}" else "",
                    style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                Spacer(Modifier.height(14.dp))
                val level = moto.fuelLevel.coerceIn(1, 5)
                val labels = listOf(tr("Crítico", "Critical"), tr("Bajo", "Low"), tr("Medio", "Medium"), tr("Alto", "High"), tr("Lleno", "Full"))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(tr("Nivel de gasolina", "Fuel level"), style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
                    Text("${labels[level - 1]} ($level/5 · ~${ui.stats.estimatedRangeKm} km)", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                }
                Spacer(Modifier.height(8.dp))
                val tone = when { level <= 1 -> cs.error to cs.onError; level <= 3 -> cs.secondary to cs.onSecondary; else -> cs.primary to cs.onPrimary }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (1..5).forEach { l ->
                        val filled = l <= level
                        val desc = tr("Nivel $l de 5, ${labels[l - 1]}", "Level $l of 5, ${labels[l - 1]}")
                        Box(
                            Modifier.weight(1f).height(56.dp).clip(MaterialTheme.shapes.small)
                                .background(if (filled) tone.first else cs.surfaceContainerHigh)
                                .selectable(l == level, role = androidx.compose.ui.semantics.Role.RadioButton, onClick = { onSetFuelLevel(l) })
                                .semantics { contentDescription = desc },
                            contentAlignment = Alignment.Center
                        ) { Text("$l", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = if (filled) tone.second else cs.onSurfaceVariant) }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TonalButton(tr("Ajustar km", "Edit odometer"), onOpenOdo)
                    OutlineButton(tr("Ver taller", "Maintenance"), onGoMaintenance)
                }
                HorizontalDivider(Modifier.padding(vertical = 18.dp), color = cs.outlineVariant)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(tr("Odómetro acumulado", "Total odometer"), style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(fmt(ui.stats.totalOdometerKm), style = MaterialTheme.typography.displaySmall)
                            Text(" km", Modifier.padding(bottom = 4.dp), style = MaterialTheme.typography.titleSmall, color = cs.onSurfaceVariant)
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${ui.stats.oilLifePercent}%", style = MaterialTheme.typography.headlineMedium, color = cs.primary)
                        Text("${tr("Aceite · faltan", "Oil ·")} ${fmt(ui.stats.oilRemainingKm)} km", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(10.dp))
                val p = ui.stats.oilLifePercent
                LinearProgressIndicator(
                    progress = { p / 100f }, Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(50)),
                    color = if (p >= 50) cs.primary else if (p >= 20) cs.secondary else cs.error,
                    trackColor = cs.surfaceContainerHigh, strokeCap = androidx.compose.ui.graphics.StrokeCap.Round, gapSize = 0.dp, drawStopIndicator = {}
                )
            }

            ui.policyDaysLeft?.let { d ->
                if (d <= 30) NaveCard(container = cs.errorContainer.copy(alpha = 0.35f), padding = 14.dp) {
                    Text(
                        if (d < 0) tr("La póliza de ${moto.insurer.ifBlank { "seguro" }} está vencida.", "Insurance policy expired.")
                        else tr("La póliza vence en $d días (${moto.policyValidUntil}).", "Insurance policy expires in $d days (${moto.policyValidUntil})."),
                        style = MaterialTheme.typography.labelLarge, color = cs.error
                    )
                }
            }

            // Franja de métricas
            StatTile(tr("Recorrido semanal", "Weekly distance"), "${ui.stats.distanceThisWeekKm}", "km", Icons.Default.Speed, "${ui.fuelLogs.size} ${tr("recargas registradas", "refill logs")}")
            StatTile(tr("Rendimiento real", "Fuel efficiency"), fmt1(ui.stats.averageEfficiencyKmL), "km/L", Icons.Default.LocalGasStation, "${tr("Autonomía estimada:", "Est. range:")} ~${ui.stats.estimatedRangeKm} km")
            StatTile(tr("Gasto en taller", "Maintenance spend"), "$" + fmt1(ui.stats.totalMaintenanceSpend), "", Icons.Default.Build, "$attention ${tr("alertas activas de servicio", "active service alerts")}", iconTint = cs.primary)

            // Bitácora reciente
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(tr("Bitácora reciente de gasolina", "Recent fuel log"), Modifier.weight(1f).semantics { heading() }, style = MaterialTheme.typography.titleLarge)
                TextButton(onGoLogs, Modifier.heightIn(min = 56.dp)) { Text(tr("Ver todo", "View all"), color = cs.primary, fontWeight = FontWeight.Bold) }
            }
            if (ui.fuelLogs.isEmpty()) {
                EmptyCard(Icons.Default.LocalGasStation, tr("Sin recargas para esta motocicleta.", "No fuel logs for this motorcycle."), tr("Registrar primera recarga", "Log first refill"), onOpenFuel)
            } else ui.fuelLogs.take(4).forEach { FuelLogRow(it, onDeleteFuelLog, cs.surfaceContainerLow) }
        }
    }
}

@Composable
private fun ChipAction(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, tint: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Row(
        Modifier.heightIn(min = 56.dp).clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onClick).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, null, Modifier.size(22.dp), tint = tint)
        Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

@Composable
private fun StatTile(label: String, value: String, unit: String, icon: androidx.compose.ui.graphics.vector.ImageVector, caption: String, iconTint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    NaveCard(Modifier.semantics(mergeDescendants = true) {}, container = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.medium, padding = 18.dp) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Eyebrow(label)
            Icon(icon, null, Modifier.size(16.dp), tint = iconTint)
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = MaterialTheme.typography.displaySmall)
            if (unit.isNotEmpty()) Text(" $unit", Modifier.padding(bottom = 4.dp), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(caption, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun FuelLogRow(log: com.nave.moto.domain.model.FuelLog, onDelete: (Long) -> Unit, container: androidx.compose.ui.graphics.Color) {
    val cs = MaterialTheme.colorScheme
    var confirm by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(container).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text("${fmt(log.odometerKm)} km · ${fmt1(log.liters)} L", style = MaterialTheme.typography.titleSmall)
            Text("${log.date} · +${log.distanceKm} km" + if (log.notes.isNotBlank()) " · ${log.notes}" else "", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
        }
        Text("${fmt1(log.efficiencyKmL)} km/L", style = MaterialTheme.typography.titleMedium.copy(fontFamily = Outfit, fontWeight = FontWeight.Black), color = cs.primary)
        SquareIconButton(Icons.Default.Delete, tr("Eliminar", "Delete"), { confirm = true })
    }
    if (confirm) ConfirmDialog(tr("¿Eliminar la recarga del ${log.date}?", "Delete the refill from ${log.date}?"), tr("Eliminar", "Delete"), { onDelete(log.id) }, { confirm = false })
}
