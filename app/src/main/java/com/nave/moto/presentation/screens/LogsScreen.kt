package com.nave.moto.presentation.screens

import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nave.moto.domain.model.RouteRecord
import com.nave.moto.domain.model.RouteStatus
import com.nave.moto.presentation.components.*
import com.nave.moto.presentation.theme.Outfit
import com.nave.moto.presentation.theme.tr
import com.nave.moto.presentation.viewmodel.NaveUiState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nave.moto.data.tracking.RouteTracker
import com.nave.moto.data.tracking.RouteTrackingService

@Composable
fun LogsScreen(
    ui: NaveUiState,
    onOpenFuel: () -> Unit,
    onDeleteFuelLog: (Long) -> Unit,
    onStartRoute: (String, (Long) -> Unit) -> Unit,
    onFinishRoute: (RouteRecord) -> Unit,
    onDeleteRoute: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    var segment by rememberSaveable { mutableStateOf(0) }
    var routeName by rememberSaveable { mutableStateOf("") }
    var deleteRoute by remember { mutableStateOf<RouteRecord?>(null) }
    val moto = ui.activeMoto
    val active = ui.routes.firstOrNull { it.status == RouteStatus.ACTIVE }
    val ctx = LocalContext.current
    val track by RouteTracker.state.collectAsStateWithLifecycle()
    val interval = ui.settings.gpsIntervalSec
    val startGate = rememberLocationGate(
        onReady = { val n = routeName.trim(); routeName = ""; onStartRoute(n) { id -> RouteTrackingService.start(ctx, id, interval) } },
        onWithoutGps = { val n = routeName.trim(); routeName = ""; onStartRoute(n) {} }
    )
    val resumeGate = rememberLocationGate(
        onReady = { active?.let { RouteTrackingService.start(ctx, it.id, interval) } },
        onWithoutGps = {}
    )
    val tracking = track.running && active != null && track.routeId == active.id
    val gpsM = when {
        active == null -> 0.0
        track.routeId == active.id -> track.distanceM
        else -> remember(active.id) { RouteTracker.savedDistanceM(ctx, active.id) }
    }

    Column(modifier) {
    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ScreenTitle(tr("Bitácora de combustible y rutas", "Fuel & route logs"),
            (moto?.let { "${it.brand} ${it.model}" } ?: "NAVE") + " · ${fmt(ui.stats.totalOdometerKm)} km")
        Row(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(cs.surfaceContainerLow).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            NavePill(tr("Recargas (${ui.fuelLogs.size})", "Refills (${ui.fuelLogs.size})"), segment == 0, { segment = 0 }, Modifier.weight(1f))
            NavePill(tr("Rutas (${ui.routes.size})", "Routes (${ui.routes.size})"), segment == 1, { segment = 1 }, Modifier.weight(1f))
        }
        if (moto == null) {
            EmptyCard(Icons.Default.TwoWheeler, tr("Agrega una moto en el Garaje para empezar.", "Add a motorcycle in the Garage to start."))
        } else if (segment == 0) {
            if (ui.fuelLogs.isEmpty()) EmptyCard(Icons.Default.LocalGasStation, tr("No hay recargas registradas para esta moto.", "No fuel logs recorded yet."))
            else ui.fuelLogs.forEach { FuelLogRow(it, onDeleteFuelLog, cs.surfaceContainer) }
        } else {
            NaveCard(padding = 18.dp) {
                if (active != null) {
                    Text("${tr("Ruta en curso", "Active route")} · ${active.startDate}", style = MaterialTheme.typography.labelLarge, color = cs.primary)
                    Text(active.name, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    if (ui.settings.gpsTracking) {
                        Row(Modifier.semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.Bottom) {
                            Text(fmt1(Math.round(gpsM / 100.0) / 10.0), style = MaterialTheme.typography.displaySmall.copy(fontSize = 52.sp))
                            Text(" km", Modifier.padding(bottom = 8.dp), style = MaterialTheme.typography.titleMedium, color = cs.onSurfaceVariant)
                            Spacer(Modifier.weight(1f))
                            track.speedKmh?.takeIf { tracking }?.let { Text("${it.toInt()} km/h", Modifier.padding(bottom = 8.dp), style = MaterialTheme.typography.titleLarge, color = cs.secondary) }
                        }
                        val (icon, status, tone) = when {
                            !tracking -> Triple(Icons.Default.PauseCircle, tr("GPS en pausa · toca Reanudar", "GPS paused · tap Resume"), cs.secondary)
                            !track.gpsEnabled -> Triple(Icons.Default.GpsOff, tr("GPS apagado: enciéndelo en Ajustes", "GPS is off: turn it on in Settings"), cs.error)
                            track.accuracyM == null -> Triple(Icons.Default.GpsNotFixed, tr("Buscando señal…", "Searching for signal…"), cs.secondary)
                            else -> Triple(Icons.Default.GpsFixed, tr("GPS activo · precisión ±${track.accuracyM!!.toInt()} m", "GPS on · accuracy ±${track.accuracyM!!.toInt()} m"), cs.primary)
                        }
                        // Icono + texto: el estado del GPS no depende solo del color
                        Row(Modifier.semantics { liveRegion = LiveRegionMode.Polite }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(icon, null, tint = tone)
                            Text(status, style = MaterialTheme.typography.labelLarge, color = tone)
                        }
                        if (!tracking) {
                            Spacer(Modifier.height(10.dp))
                            TonalButton(tr("Reanudar GPS", "Resume GPS"), { resumeGate() }, Modifier.fillMaxWidth(), icon = Icons.Default.GpsFixed)
                        } else if (!track.gpsEnabled) {
                            Spacer(Modifier.height(10.dp))
                            TonalButton(tr("Abrir ajustes de ubicación", "Open location settings"), {
                                ctx.startActivity(android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                            }, Modifier.fillMaxWidth(), icon = Icons.Default.Settings)
                        }
                    } else Text(tr("GPS desactivado en Ajustes: al terminar escribes el odómetro.", "GPS is off in Settings: enter the odometer when you finish."),
                        style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    Text("${tr("Odómetro inicial:", "Start odometer:")} ${fmt(active.odoStartKm)} km", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                } else {
                    NaveField(routeName, { routeName = it }, "${tr("Nombre de la ruta · desde", "Route name · from")} ${fmt(ui.stats.totalOdometerKm)} km",
                        placeholder = tr("Ej. Caracas – La Guaira", "e.g. Coastal ride"))
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(tr("Trabajo", "Work"), tr("Entregas", "Deliveries"), tr("Paseo", "Ride")).forEach { n -> NavePill(n, routeName == n, { routeName = n }) }
                    }
                    if (ui.settings.gpsTracking) Text(tr("Se pedirá permiso de ubicación y se medirá la ruta con el GPS.", "Location permission will be requested and the route measured by GPS."),
                        Modifier.padding(top = 10.dp), style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                }
            }
            val done = ui.routes.filter { it.status == RouteStatus.COMPLETED }
            if (done.isEmpty()) EmptyCard(Icons.Default.Route, tr("Sin rutas completadas para esta motocicleta.", "No completed routes yet."))
            done.forEach { r ->
                Row(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(cs.surfaceContainerLow).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(r.name, style = MaterialTheme.typography.titleSmall)
                        Text("${r.startDate} · ${fmt(r.odoStartKm)} → ${fmt(r.odoEndKm ?: 0)} km · ${fmt1(r.estimatedFuelLiters)} L est.", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                        if (r.notes.isNotBlank()) Text(r.notes, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                    }
                    Text("+${r.distanceKm} km", style = MaterialTheme.typography.titleMedium.copy(fontFamily = Outfit, fontWeight = FontWeight.Black), color = cs.secondary)
                    SquareIconButton(Icons.Default.Delete, tr("Eliminar", "Delete"), { deleteRoute = r })
                }
            }
            // Una ruta activa también se puede descartar
            active?.let { TextButton({ deleteRoute = it }, Modifier.heightIn(min = 56.dp)) { Text(tr("Descartar ruta en curso", "Discard active route"), color = cs.error) } }
        }
    }
    // Acción principal fija abajo, al alcance del pulgar
    if (moto != null) Box(Modifier.fillMaxWidth().padding(top = 10.dp)) {
        when {
            segment == 0 -> PrimaryButton(tr("Cargar gasolina", "Log fuel"), onOpenFuel, Modifier.fillMaxWidth(), icon = Icons.Default.Add)
            active != null -> PrimaryButton(tr("Finalizar ruta", "Finish route"), { onFinishRoute(active) }, Modifier.fillMaxWidth(), icon = Icons.Default.Flag)
            else -> PrimaryButton(tr("Iniciar ruta", "Start route"), { if (ui.settings.gpsTracking) startGate() else { val n = routeName.trim(); routeName = ""; onStartRoute(n) {} } },
                Modifier.fillMaxWidth(), icon = Icons.Default.PlayArrow)
        }
    }
    }
    deleteRoute?.let { r -> ConfirmDialog(tr("¿Eliminar la ruta \"${r.name}\"?", "Delete route \"${r.name}\"?"), tr("Eliminar", "Delete"), { if (r.status == RouteStatus.ACTIVE) RouteTrackingService.stop(ctx); onDeleteRoute(r.id) }, { deleteRoute = null }) }
}
