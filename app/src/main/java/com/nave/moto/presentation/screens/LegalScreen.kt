package com.nave.moto.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.nave.moto.presentation.components.*
import com.nave.moto.presentation.theme.tr

private class LegalItem(val icon: ImageVector, val title: String, val body: String)

const val LEGAL_DATE = "2026-11-17"
// Pendiente de confirmar por el autor: no hay correo de soporte en el proyecto; se usa su perfil de GitHub.
const val LEGAL_CONTACT = "github.com/MetalSyntax"

/** Privacidad, términos y «Acerca de» en una sola pantalla con pestañas (tarjetas con ícono). */
@Composable
fun LegalScreen(onBack: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val ctx = LocalContext.current
    val version = remember { runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: "" }
    var tab by remember { mutableIntStateOf(0) }
    val privacy = listOf(
        LegalItem(Icons.Default.PhoneAndroid, tr("Todo queda en tu teléfono", "Everything stays on your phone"),
            tr("NAVE guarda tus motos, recargas, servicios, rutas, manuales, fotos y tu perfil solo en la memoria del teléfono. No hay cuentas, registro ni servidores de NAVE, y la app ni siquiera pide permiso de internet.",
                "NAVE stores your bikes, fuel logs, services, routes, manuals, photos and profile only on the phone. There are no accounts or NAVE servers, and the app does not even request internet permission.")),
        LegalItem(Icons.Default.MyLocation, tr("Ubicación (GPS)", "Location (GPS)"),
            tr("Solo si activas «Medir ruta con GPS» y hay una ruta en curso. Se usa para sumar los km y se guarda como un archivo de puntos (CSV) en el teléfono. No se envía a nadie, no se usa ubicación en segundo plano fuera de la ruta y puedes apagarla en Ajustes o quitar el permiso cuando quieras. Mientras la ruta está activa verás una notificación fija.",
                "Only if you enable \"Measure route with GPS\" and a route is active. It is used to add up the km and is saved as a points file (CSV) on the phone. It is never sent anywhere, location is not used outside a route, and you can turn it off in Settings or revoke the permission any time. A persistent notification shows while a route is active.")),
        LegalItem(Icons.Default.Badge, tr("Perfil y contacto de emergencia", "Profile & emergency contact"),
            tr("Tu nombre, cédula, teléfono, tipo de sangre, licencia y contacto de emergencia son opcionales y se quedan en el teléfono. El botón de llamada solo abre el marcador; tú decides si llamas.",
                "Your name, ID, phone, blood type, license and emergency contact are optional and stay on the phone. The call button only opens the dialer; you decide whether to call.")),
        LegalItem(Icons.Default.Archive, tr("Respaldos", "Backups"),
            tr("«Exportar» crea un .zip con tus datos (incluye perfil y puntos GPS) donde tú elijas. Quien tenga ese archivo puede leerlo: guárdalo con cuidado. Si tu teléfono tiene la copia de seguridad de Android activada, el sistema puede copiar los datos de la app a tu cuenta de Google según tu configuración; eso lo controla Android, no NAVE.",
                "\"Export\" creates a .zip with your data (profile and GPS points included) wherever you choose. Anyone with that file can read it, so keep it safe. If Android backup is on, the system may copy app data to your Google account per your settings; Android controls that, not NAVE.")),
        LegalItem(Icons.Default.DeleteForever, tr("Eliminar tus datos", "Deleting your data"),
            tr("Borra cada registro desde su pantalla, usa «Restaurar ejemplo» para vaciar el garaje, o desinstala la app / borra sus datos desde los ajustes de Android para eliminar todo, incluidas fotos y rutas.",
                "Delete each record from its screen, use \"Restore sample\" to clear the garage, or uninstall / clear the app data from Android settings to remove everything, including photos and routes.")),
        LegalItem(Icons.Default.Mail, tr("Contacto y vigencia", "Contact & effective date"),
            tr("Vigente desde $LEGAL_DATE. Dudas o solicitudes: $LEGAL_CONTACT. Si la política cambia, se actualizará aquí.",
                "Effective $LEGAL_DATE. Questions or requests: $LEGAL_CONTACT. If the policy changes it will be updated here."))
    )
    val terms = listOf(
        LegalItem(Icons.Default.CheckCircle, tr("Aceptación", "Acceptance"),
            tr("Al usar NAVE aceptas estos términos. Si no estás de acuerdo, deja de usar la app.", "By using NAVE you accept these terms. If you disagree, stop using the app.")),
        LegalItem(Icons.Default.TwoWheeler, tr("Uso permitido", "Permitted use"),
            tr("NAVE es una herramienta personal de control de tu moto, gasolina, servicios y rutas. Tú eres responsable de la información que registras.",
                "NAVE is a personal tool to track your bike, fuel, services and routes. You are responsible for the information you enter.")),
        LegalItem(Icons.Default.Warning, tr("Seguridad vial", "Road safety"),
            tr("No uses el teléfono mientras conduces. Detente en un lugar seguro antes de registrar datos o llamar. Los km del GPS y los cálculos de consumo y alertas de servicio son estimaciones: confirma con tu odómetro y tu taller.",
                "Do not use the phone while riding. Stop somewhere safe before logging data or calling. GPS km and fuel and service estimates are approximate: confirm with your odometer and mechanic.")),
        LegalItem(Icons.Default.Shield, tr("Sin garantías", "No warranty"),
            tr("La app se ofrece «tal cual». No respondemos por pérdida de datos por fallos del equipo o errores del usuario; haz respaldos con Exportar. El botón de contacto de emergencia no sustituye a los servicios de emergencia.",
                "The app is provided \"as is\". We are not liable for data loss from device failure or user error; back up with Export. The emergency contact button does not replace emergency services.")),
        LegalItem(Icons.Default.Update, tr("Cambios", "Changes"), tr("Podemos actualizar estos términos; la fecha de vigencia aparece en Privacidad.", "We may update these terms; the effective date is shown in Privacy."))
    )
    Column(Modifier.fillMaxSize().background(cs.surface).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onBack, Modifier.size(56.dp)) { Icon(Icons.AutoMirrored.Filled.ArrowBack, tr("Atrás", "Back")) }
            Text(tr("Privacidad y cumplimiento", "Privacy & compliance"), Modifier.semantics { heading() }, style = MaterialTheme.typography.titleLarge)
        }
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(tr("Privacidad", "Privacy"), tr("Términos", "Terms"), tr("Acerca de", "About")).forEachIndexed { i, l ->
                NavePill(l, tab == i, { tab = i }, Modifier.weight(1f))
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (tab == 2) {
                NaveCard(container = cs.surfaceContainerLow) {
                    Text("NAVE", style = MaterialTheme.typography.headlineMedium)
                    Text(tr("Versión $version", "Version $version"), style = MaterialTheme.typography.titleMedium, color = cs.primary)
                    Spacer(Modifier.height(8.dp))
                    Text(tr("Bitácora de tu moto para el que vive en la calle: gasolina, servicios, rutas con GPS, manuales y tu perfil de emergencia, todo sin internet.",
                        "A logbook for riders who live on the street: fuel, services, GPS routes, manuals and your emergency profile, all offline."),
                        style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text(tr("Contacto: $LEGAL_CONTACT", "Contact: $LEGAL_CONTACT"), style = MaterialTheme.typography.bodyMedium)
                }
            } else (if (tab == 0) privacy else terms).forEach { it ->
                NaveCard(container = cs.surfaceContainerLow) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(it.icon, null, tint = cs.primary, modifier = Modifier.size(28.dp))
                        Column(Modifier.weight(1f)) {
                            Text(it.title, Modifier.semantics { heading() }, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(it.body, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
