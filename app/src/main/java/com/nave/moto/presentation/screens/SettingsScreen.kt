package com.nave.moto.presentation.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nave.moto.presentation.components.*
import com.nave.moto.presentation.theme.tr
import com.nave.moto.presentation.viewmodel.NaveMotoViewModel
import com.nave.moto.presentation.viewmodel.NaveUiState
import java.time.LocalDate

/** Ajustes completos de la app. Cada control es ≥56 dp y se aplica y guarda al instante. */
@Composable
fun SettingsScreen(vm: NaveMotoViewModel, ui: NaveUiState, onProfile: () -> Unit, onLegal: () -> Unit, onBack: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val ctx = LocalContext.current
    val s = ui.settings
    var pendingImport by remember { mutableStateOf<Uri?>(null) }
    var confirmDemo by remember { mutableStateOf(false) }
    var rider by remember(s.riderName) { mutableStateOf(s.riderName) }
    var oilTxt by remember(s.oilIntervalKm) { mutableStateOf(s.oilIntervalKm.toString()) }
    // Estado de permisos: se vuelve a leer al volver de los ajustes del sistema
    var tick by remember { mutableIntStateOf(0) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val o = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) tick++ }
        owner.lifecycle.addObserver(o); onDispose { owner.lifecycle.removeObserver(o) }
    }
    val permOk = remember(tick) { hasFineLocation(ctx) }
    val gpsOn = remember(tick) { isGpsEnabled(ctx) }
    val version = remember { runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: "" }

    val createBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        uri?.let { ctx.contentResolver.openOutputStream(it)?.let(vm::exportBackup) }
    }
    val pickBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { pendingImport = it }

    Column(Modifier.fillMaxSize().background(cs.surface).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onBack, Modifier.size(56.dp)) { Icon(Icons.AutoMirrored.Filled.ArrowBack, tr("Atrás", "Back")) }
            Text(tr("Ajustes", "Settings"), Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineSmall)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

            Section(tr("En ruta", "On the road")) {
                SettingSwitch(tr("Medir ruta con GPS", "Measure route with GPS"),
                    tr("Registra los km con el GPS del teléfono, también con la pantalla apagada.", "Records distance with the phone GPS, even with the screen off."),
                    s.gpsTracking, vm::setGpsTracking)
                if (s.gpsTracking) {
                    Text(tr("Frecuencia del GPS", "GPS frequency"), style = MaterialTheme.typography.labelLarge, color = cs.onSurfaceVariant)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(2 to tr("Precisa · 2 s", "Precise · 2 s"), 3 to tr("Normal · 3 s", "Normal · 3 s"), 8 to tr("Ahorro · 8 s", "Saver · 8 s"))
                            .forEach { (v, l) -> NavePill(l, s.gpsIntervalSec == v, { vm.setGpsInterval(v) }) }
                    }
                }
                SettingSwitch(tr("Mantener pantalla encendida en ruta", "Keep screen on during a route"),
                    tr("Solo mientras hay una ruta en curso. Gasta más batería.", "Only while a route is active. Uses more battery."),
                    s.keepScreenOnRoute, vm::setKeepScreenOn)
                SettingSwitch(tr("Vibración al tocar", "Haptic feedback"),
                    tr("Confirma con una vibración cada acción importante, útil con guantes.", "A short buzz confirms key actions, handy with gloves."),
                    s.haptics, vm::setHaptics)
            }

            Section(tr("Pantalla y lectura", "Display & readability")) {
                SettingSwitch(tr("Tema oscuro", "Dark theme"), if (s.darkTheme) tr("Fondo negro, menos reflejos de noche.", "Black background, less glare at night.") else tr("Fondo claro.", "Light background."),
                    s.darkTheme) { vm.toggleTheme() }
                SettingSwitch(tr("Alto contraste (modo sol)", "High contrast (sun mode)"),
                    tr("Negro y blanco puros y colores más vivos para leer a pleno sol.", "Pure black and white with brighter colors to read in direct sun."),
                    s.highContrast, vm::setHighContrast)
                Text(tr("Tamaño del texto", "Text size"), style = MaterialTheme.typography.labelLarge, color = cs.onSurfaceVariant)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0.9f to tr("Pequeña", "Small"), 1f to tr("Normal", "Normal"), 1.15f to tr("Grande", "Large"), 1.3f to tr("Muy grande", "Extra large"), 1.5f to tr("Máxima", "Max"))
                        .forEach { (v, l) -> NavePill(l, s.fontScale == v, { vm.setFontScale(v) }) }
                }
                Text(tr("Así se ve el texto: 12.480 km", "Preview text: 12,480 km"), style = MaterialTheme.typography.titleMedium)
                Text(tr("Idioma", "Language"), style = MaterialTheme.typography.labelLarge, color = cs.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NavePill("Español", s.language == "es", { vm.setLanguage("es") }, Modifier.weight(1f))
                    NavePill("English", s.language == "en", { vm.setLanguage("en") }, Modifier.weight(1f))
                }
            }

            Section(tr("Piloto y taller", "Rider & service")) {
                NaveField(rider, { rider = it; vm.setRiderName(it) }, tr("Nombre del piloto", "Rider name"))
                TonalButton(tr("Mi perfil de motorizado", "My rider profile"), onProfile, Modifier.fillMaxWidth(), icon = Icons.Default.Person)
                NumberStepper(tr("Intervalo de aceite (km)", "Oil interval (km)"), oilTxt, {
                    oilTxt = it; it.toIntOrNull()?.takeIf { v -> v in 500..20000 }?.let(vm::setOilInterval)
                }, 500.0, min = 500.0, presets = listOf(2000, 3000, 4000, 5000).map { fmt(it) to it.toDouble() })
            }

            Section(tr("Permisos", "Permissions")) {
                StatusRow(permOk, tr("Ubicación precisa: concedida", "Precise location: granted"), tr("Ubicación precisa: sin permiso", "Precise location: not granted"))
                StatusRow(gpsOn, tr("GPS del teléfono: encendido", "Phone GPS: on"), tr("GPS del teléfono: apagado", "Phone GPS: off"))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TonalButton(tr("Permisos de la app", "App permissions"), {
                        ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", ctx.packageName, null)))
                    }, Modifier.weight(1f))
                    TonalButton(tr("Ubicación", "Location"), { ctx.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }, Modifier.weight(1f))
                }
            }

            Section(tr("Respaldo de datos", "Data backup")) {
                Text(tr("Exporta garaje, recargas, servicios, rutas con sus puntos GPS, perfil, fotos y manuales a un .zip, o restaura desde uno. Los ajustes de este teléfono no se sobrescriben al importar.",
                    "Export garage, fuel logs, services, routes with GPS points, profile, photos and manuals to a .zip, or restore from one. This phone's settings are kept on import."),
                    style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TonalButton(tr("Exportar", "Export"), { createBackup.launch("nave-respaldo-${LocalDate.now()}.zip") }, Modifier.weight(1f), icon = Icons.Default.Download)
                    TonalButton(tr("Importar", "Import"), { pickBackup.launch(arrayOf("application/zip", "application/octet-stream")) }, Modifier.weight(1f), icon = Icons.Default.Upload)
                }
            }

            Section(tr("Datos de ejemplo", "Sample data")) {
                Text(tr("Reemplaza motos, recargas, servicios y manuales por un garaje de ejemplo para probar la app. Tu perfil y ajustes se conservan.",
                    "Replaces bikes, fuel logs, services and manuals with a sample garage to try the app. Your profile and settings are kept."),
                    style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                TonalButton(tr("Restaurar ejemplo", "Restore sample"), { confirmDemo = true }, Modifier.fillMaxWidth(), icon = Icons.Default.Restore)
            }

            Section(tr("Privacidad y cumplimiento", "Privacy & compliance")) {
                TonalButton(tr("Privacidad, términos y acerca de", "Privacy, terms & about"), onLegal, Modifier.fillMaxWidth(), icon = Icons.Default.Gavel)
            }

            Text("NAVE · v$version", Modifier.padding(bottom = 16.dp), style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
        }
    }
    if (confirmDemo) ConfirmDialog(tr("Esto BORRA tus motos, recargas, servicios y manuales actuales. ¿Continuar?", "This DELETES your current bikes, fuel logs, services and manuals. Continue?"),
        tr("Restaurar", "Restore"), { vm.restoreDemo(); confirmDemo = false }, { confirmDemo = false })
    pendingImport?.let { uri ->
        ConfirmDialog(tr("Importar reemplaza TODOS los datos actuales. ¿Continuar?", "Import replaces ALL current data. Continue?"), tr("Importar", "Import"),
            { ctx.contentResolver.openInputStream(uri)?.let(vm::importBackup) }, { pendingImport = null })
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    NaveCard(container = MaterialTheme.colorScheme.surfaceContainerLow) {
        Text(title, Modifier.semantics { heading() }, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

/** Fila completa tocable (≥56 dp) con interruptor; TalkBack la lee como un solo interruptor. */
@Composable
private fun SettingSwitch(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val buzz = rememberHaptic(strong = false)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).toggleable(checked, role = Role.Switch, onValueChange = { buzz(); onChange(it) }),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked, null)
    }
}

@Composable
private fun StatusRow(ok: Boolean, yes: String, no: String) {
    val cs = MaterialTheme.colorScheme
    Row(Modifier.heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(if (ok) Icons.Default.CheckCircle else Icons.Default.Warning, null, tint = if (ok) cs.primary else cs.error)
        Text(if (ok) yes else no, style = MaterialTheme.typography.titleSmall, color = if (ok) cs.onSurface else cs.error)
    }
}
