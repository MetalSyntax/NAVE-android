package com.nave.moto.presentation

import androidx.compose.runtime.saveable.rememberSaveable
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.nave.moto.R
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.contentDescription
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nave.moto.data.tracking.RouteTracker
import com.nave.moto.data.tracking.RouteTrackingService
import com.nave.moto.domain.model.RouteStatus
import com.nave.moto.presentation.components.SquareIconButton
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nave.moto.domain.model.CatalogModel
import com.nave.moto.domain.model.Motorcycle
import com.nave.moto.domain.model.RouteRecord
import com.nave.moto.presentation.components.PrimaryButton
import com.nave.moto.presentation.components.rememberHaptic
import com.nave.moto.presentation.screens.*
import com.nave.moto.presentation.theme.LocalEs
import com.nave.moto.presentation.theme.Outfit
import com.nave.moto.presentation.theme.tr
import com.nave.moto.presentation.viewmodel.NaveMotoViewModel
import com.nave.moto.presentation.viewmodel.NaveUiState

private sealed interface Overlay {
    data class Catalog(val pick: Boolean) : Overlay
    data class Form(val edit: Motorcycle?, val prefill: CatalogModel?) : Overlay
    data object Settings : Overlay
    data object Profile : Overlay
    data object Legal : Overlay
}

private enum class Dlg { Fuel, Odo, Maint, Alerts }

@Composable
fun NaveApp(vm: NaveMotoViewModel, ui: NaveUiState) {
    val cs = MaterialTheme.colorScheme
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var overlay by remember { mutableStateOf<Overlay?>(null) }
    var dlg by remember { mutableStateOf<Dlg?>(null) }
    var finishRoute by remember { mutableStateOf<RouteRecord?>(null) }
    val snack = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { vm.messages.collect { snack.showSnackbar(it) } }
    val ctx = LocalContext.current
    val track by RouteTracker.state.collectAsStateWithLifecycle()

    // Pantalla encendida solo mientras hay una ruta en curso (y el piloto lo permite)
    val keepOn = ui.settings.keepScreenOnRoute && ui.routes.any { it.status == RouteStatus.ACTIVE }
    val view = LocalView.current
    DisposableEffect(keepOn) { view.keepScreenOn = keepOn; onDispose { view.keepScreenOn = false } }

    val items: List<Pair<String, ImageVector>> = listOf(
        tr("Tablero", "Home") to Icons.Default.Speed,
        tr("Bitácora", "Logs") to Icons.Default.LocalGasStation,
        tr("Taller", "Service") to Icons.Default.Build,
        tr("Garaje", "Garage") to Icons.Default.TwoWheeler
    )

    Box(Modifier.fillMaxSize().background(cs.surface)) {
        Scaffold(
            containerColor = cs.surface,
            snackbarHost = { SnackbarHost(snack) },
            topBar = {
                Column(Modifier.background(cs.surfaceContainerLowest).statusBarsPadding()) {
                    Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Row(Modifier.weight(1f).heightIn(min = 48.dp).clickable(role = Role.Button) { tab = 0 }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(Modifier.size(40.dp).clip(MaterialTheme.shapes.small)) {
                                Image(painterResource(R.drawable.ic_launcher_background), null, Modifier.fillMaxSize())
                                Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.fillMaxSize())
                            }
                            Text("NAVE", style = MaterialTheme.typography.titleLarge.copy(fontFamily = Outfit, fontWeight = FontWeight.Black), color = cs.onSurface)
                        }
                        SquareIconButton(Icons.Default.Person, tr("Mi perfil", "My profile"), { overlay = Overlay.Profile })
                        Spacer(Modifier.width(6.dp))
                        SquareIconButton(Icons.Default.Settings, tr("Ajustes", "Settings"), { overlay = Overlay.Settings })
                    }
                    HorizontalDivider(color = cs.outlineVariant)
                }
            },
            bottomBar = {
                Column(Modifier.background(cs.surfaceContainer)) {
                    HorizontalDivider(color = cs.outlineVariant)
                    // Acción principal siempre al alcance del pulgar
                    val barAction: Pair<String, () -> Unit>? = when {
                        ui.activeMoto == null -> null
                        tab == 0 -> tr("Cargar gasolina", "Log fuel") to { dlg = Dlg.Fuel }
                        tab == 2 -> tr("Registrar servicio", "Log service") to { dlg = Dlg.Maint }
                        else -> null
                    }
                    barAction?.let { (t, a) -> PrimaryButton(t, a, Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) }
                    Row(Modifier.fillMaxWidth().height(72.dp).navigationBarsPadding().padding(horizontal = 4.dp)) {
                        items.forEachIndexed { i, (label, icon) ->
                            val active = tab == i
                            val buzz = rememberHaptic(strong = false)
                            Column(Modifier.weight(1f).fillMaxHeight().selectable(active, role = Role.Tab, onClick = { buzz(); tab = i }), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Box(
                                    Modifier.size(if (active) 64.dp else 40.dp, 32.dp).clip(MaterialTheme.shapes.extraLarge)
                                        .background(if (active) cs.secondaryContainer else androidx.compose.ui.graphics.Color.Transparent),
                                    contentAlignment = Alignment.Center
                                ) { Icon(icon, null, Modifier.size(24.dp), tint = if (active) cs.onSecondaryContainer else cs.onSurfaceVariant) }
                                Text(label, style = MaterialTheme.typography.labelMedium, color = if (active) cs.onSurface else cs.onSurfaceVariant, maxLines = 1)
                            }
                        }
                    }
                }
            }
        ) { pad ->
            val m = Modifier.padding(pad).padding(horizontal = 16.dp, vertical = 16.dp)
            if (ui.loaded) Crossfade(tab, label = "tab") { t ->
                when (t) {
                    0 -> DashboardScreen(ui, vm::selectMoto, vm::setFuelLevel, { dlg = Dlg.Fuel }, { dlg = Dlg.Odo }, { dlg = Dlg.Alerts },
                        vm::toggleTheme, { tab = 2 }, { tab = 1 }, { tab = 3 }, vm::deleteFuelLog, m)
                    1 -> LogsScreen(ui, { dlg = Dlg.Fuel }, vm::deleteFuelLog, { n, cb -> vm.startRoute(n, cb) }, { finishRoute = it }, vm::deleteRoute, m)
                    2 -> MaintenanceScreen(ui, { dlg = Dlg.Maint }, vm::markServiced, vm::toggleSchedule, vm::addSchedule, vm::deleteSchedule, vm::deleteMaintenance, m)
                    else -> GarageScreen(
                        ui, { overlay = Overlay.Catalog(pick = true) }, { overlay = Overlay.Catalog(pick = false) },
                        { overlay = Overlay.Form(it, null) }, vm::deleteMoto, vm::selectMoto, vm::addManual,
                        vm::deleteManual, { overlay = Overlay.Settings }, vm::manualPreview, m
                    )
                }
            }
        }

        // Pantallas completas
        if (overlay != null) Surface(Modifier.fillMaxSize(), color = cs.surface, contentColor = cs.onSurface) {
        when (val o = overlay) {
            is Overlay.Catalog -> {
                BackHandler { overlay = null }
                CatalogScreen(vm, o.pick, { overlay = null }, { overlay = Overlay.Form(null, it) }, { overlay = Overlay.Form(null, null) })
            }
            is Overlay.Form -> {
                BackHandler { overlay = null }
                MotoFormScreen(o.edit, o.prefill, { overlay = null }) { moto, photo, thumbId ->
                    (if (o.edit == null) vm.addMoto(moto, photo, thumbId) else vm.updateMoto(moto, photo))
                        .also { if (it == null) { overlay = null; tab = 0 } }
                }
            }
            Overlay.Settings -> {
                BackHandler { overlay = null }
                SettingsScreen(vm, ui, onProfile = { overlay = Overlay.Profile }, onLegal = { overlay = Overlay.Legal }) { overlay = null }
            }
            Overlay.Profile -> {
                BackHandler { overlay = null }
                ProfileScreen(vm, ui) { overlay = null }
            }
            Overlay.Legal -> {
                BackHandler { overlay = Overlay.Settings }
                LegalScreen { overlay = Overlay.Settings }
            }
            null -> {}
        }
        }
    }

    when (dlg) {
        Dlg.Fuel -> FuelDialog(ui, { dlg = null }) { odo, l, d, n -> vm.logFuel(odo, l, d, n) }
        Dlg.Odo -> OdoDialog(ui.stats.totalOdometerKm, { dlg = null }) { vm.setOdometer(it) }
        Dlg.Maint -> MaintenanceDialog(ui, { dlg = null }) { t, o, d, c, w, n, s -> vm.addMaintenance(t, o, d, c, w, n, s) }
        Dlg.Alerts -> AlertsDialog(ui) { dlg = null }
        null -> {}
    }
    finishRoute?.let { r ->
        // Km medidos por GPS: en vivo si el servicio sigue corriendo, o los guardados en el CSV
        val gpsKm = if (!ui.settings.gpsTracking) null else
            (if (track.routeId == r.id) track.distanceM else RouteTracker.savedDistanceM(ctx, r.id)).takeIf { it > 0 }?.div(1000.0)
        FinishRouteDialog(r, gpsKm, { finishRoute = null }) { odo, n ->
            vm.finishRoute(r, odo, n).also { if (it == null) RouteTrackingService.stop(ctx) }
        }
    }
}
