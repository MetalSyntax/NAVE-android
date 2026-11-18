package com.nave.moto.presentation.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.nave.moto.domain.rules.BLOOD_TYPES
import com.nave.moto.domain.rules.dialable
import com.nave.moto.domain.rules.daysUntil
import com.nave.moto.presentation.components.*
import com.nave.moto.presentation.theme.tr
import com.nave.moto.presentation.viewmodel.NaveMotoViewModel
import com.nave.moto.presentation.viewmodel.NaveUiState
import java.io.File

/** Perfil del motorizado: lectura por defecto, «Editar» para cambiar. Datos y foto solo en el teléfono. */
@Composable
fun ProfileScreen(vm: NaveMotoViewModel, ui: NaveUiState, onBack: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val ctx = LocalContext.current
    val s = ui.settings
    var editing by remember { mutableStateOf(false) }
    var name by remember(editing) { mutableStateOf(s.riderName) }
    var idNum by remember(editing) { mutableStateOf(s.riderIdNumber) }
    var phone by remember(editing) { mutableStateOf(s.riderPhone) }
    var blood by remember(editing) { mutableStateOf(s.bloodType) }
    var emName by remember(editing) { mutableStateOf(s.emergencyName) }
    var emPhone by remember(editing) { mutableStateOf(s.emergencyPhone) }
    var licNum by remember(editing) { mutableStateOf(s.licenseNumber) }
    var licExp by remember(editing) { mutableStateOf(s.licenseExpiry) }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { it?.let(vm::setRiderPhoto) }
    val buzz = rememberHaptic()

    fun dial(n: String) = runCatching { ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${dialable(n)}"))) }

    Column(Modifier.fillMaxSize().background(cs.surface).statusBarsPadding().navigationBarsPadding().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton({ if (editing) editing = false else onBack() }, Modifier.size(56.dp)) { Icon(Icons.AutoMirrored.Filled.ArrowBack, tr("Atrás", "Back")) }
            Text(tr("Mi perfil", "My profile"), Modifier.weight(1f).semantics { heading() }, style = MaterialTheme.typography.headlineSmall)
            if (!editing) SquareIconButton(Icons.Default.Edit, tr("Editar perfil", "Edit profile"), { editing = true })
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Foto + nombre
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                val photo = s.riderPhoto?.let { File(ctx.filesDir, it) }?.takeIf { it.exists() }
                val avatarDesc = tr("Foto del piloto", "Rider photo")
                Box(Modifier.size(96.dp).clip(CircleShape).background(cs.surfaceContainerHigh)
                    .then(if (editing) Modifier.clickable(role = Role.Button, onClickLabel = tr("Cambiar foto", "Change photo")) { pickPhoto.launch("image/*") } else Modifier)
                    .semantics { contentDescription = avatarDesc }, contentAlignment = Alignment.Center) {
                    if (photo != null) AsyncImage(photo, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    else Icon(Icons.Default.Person, null, Modifier.size(52.dp), tint = cs.primary)
                    if (editing) Icon(Icons.Default.PhotoCamera, null, Modifier.align(Alignment.BottomEnd).size(28.dp).clip(CircleShape).background(cs.primary).padding(5.dp), tint = cs.onPrimary)
                }
                Column(Modifier.weight(1f)) {
                    Text(s.riderName.ifBlank { tr("Sin nombre", "No name") }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    if (ui.activeMoto != null) Text("${ui.activeMoto.brand} ${ui.activeMoto.model} · ${ui.activeMoto.plate}", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                    if (editing && s.riderPhoto != null) TextButton({ vm.setRiderPhoto(null) }, Modifier.heightIn(min = 48.dp)) { Text(tr("Quitar foto", "Remove photo")) }
                }
            }

            if (editing) {
                PCard(tr("Datos personales", "Personal info")) {
                    NaveField(name, { name = it }, tr("Nombre", "Name"))
                    NaveField(idNum, { idNum = it }, tr("Cédula (opcional)", "ID number (optional)"), number = true)
                    NaveField(phone, { phone = it }, tr("Teléfono", "Phone"), phone = true)
                    Text(tr("Tipo de sangre", "Blood type"), style = MaterialTheme.typography.labelLarge, color = cs.onSurfaceVariant)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NavePill(tr("No sé", "Unknown"), blood.isEmpty(), { blood = "" })
                        BLOOD_TYPES.forEach { NavePill(it, blood == it, { blood = it }) }
                    }
                }
                PCard(tr("Contacto de emergencia", "Emergency contact")) {
                    NaveField(emName, { emName = it }, tr("Nombre", "Name"))
                    NaveField(emPhone, { emPhone = it }, tr("Teléfono", "Phone"), phone = true)
                }
                PCard(tr("Licencia de conducir", "Driver license")) {
                    NaveField(licNum, { licNum = it }, tr("Número de licencia", "License number"))
                    DateField(tr("Vence", "Expires"), licExp, { licExp = it }, expiry = true, optional = true)
                }
            } else {
                // Emergencia: lo más importante, arriba y con botón grande
                PCard(tr("Contacto de emergencia", "Emergency contact")) {
                    if (s.emergencyPhone.isBlank()) {
                        Text(tr("Aún no tienes contacto de emergencia. Agrégalo en Editar.", "No emergency contact yet. Add one in Edit."), style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                    } else {
                        Text(s.emergencyName.ifBlank { tr("Contacto", "Contact") }, style = MaterialTheme.typography.titleLarge)
                        Text(s.emergencyPhone, style = MaterialTheme.typography.titleMedium, color = cs.onSurfaceVariant)
                        Button({ buzz(); dial(s.emergencyPhone) }, Modifier.fillMaxWidth().heightIn(min = 72.dp), shape = MaterialTheme.shapes.small,
                            colors = ButtonDefaults.buttonColors(containerColor = cs.error, contentColor = cs.onError)) {
                            Icon(Icons.Default.Call, null, Modifier.size(28.dp)); Spacer(Modifier.width(10.dp))
                            Text(tr("Llamar a ${s.emergencyName.ifBlank { "emergencia" }}", "Call ${s.emergencyName.ifBlank { "emergency contact" }}"),
                                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                PCard(tr("Datos", "Info")) {
                    InfoRow(Icons.Default.Bloodtype, tr("Tipo de sangre", "Blood type"), s.bloodType)
                    InfoRow(Icons.Default.Badge, tr("Cédula", "ID number"), s.riderIdNumber)
                    InfoRow(Icons.Default.Phone, tr("Teléfono", "Phone"), s.riderPhone) { if (s.riderPhone.isNotBlank()) dial(s.riderPhone) }
                }
                PCard(tr("Licencia de conducir", "Driver license")) {
                    InfoRow(Icons.Default.CreditCard, tr("Número", "Number"), s.licenseNumber)
                    val d = daysUntil(s.licenseExpiry)
                    val (txt, bad) = when {
                        s.licenseExpiry.isBlank() || d == null -> "" to false
                        d < 0 -> tr("${s.licenseExpiry} · VENCIDA hace ${-d} días", "${s.licenseExpiry} · EXPIRED ${-d} days ago") to true
                        d <= 30 -> tr("${s.licenseExpiry} · vence en $d días", "${s.licenseExpiry} · expires in $d days") to true
                        else -> s.licenseExpiry to false
                    }
                    InfoRow(Icons.Default.Event, tr("Vence", "Expires"), txt, warn = bad)
                }
                PCard(tr("Tu actividad", "Your activity")) {
                    val km = ui.routes.sumOf { it.distanceKm }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Stat(fmt(km), tr("km en rutas", "route km"), Modifier.weight(1f)); Stat("${ui.routes.size}", tr("rutas", "routes"), Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Stat("${ui.fuelLogs.size}", tr("recargas", "refuels"), Modifier.weight(1f)); Stat("${ui.maintenance.size}", tr("servicios", "services"), Modifier.weight(1f))
                    }
                    if (ui.activeMoto != null) Text(tr("De la moto activa (${ui.activeMoto.brand} ${ui.activeMoto.model}). ${ui.motorcycles.size} motos en el garaje.",
                        "From the active bike (${ui.activeMoto.brand} ${ui.activeMoto.model}). ${ui.motorcycles.size} bikes in the garage."), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        if (editing) Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TonalButton(tr("Cancelar", "Cancel"), { editing = false }, Modifier.weight(1f))
            PrimaryButton(tr("Guardar", "Save"), {
                vm.saveProfile { it.copy(riderName = name.trim().ifBlank { "Piloto NAVE" }, riderIdNumber = idNum.trim(), riderPhone = phone.trim(), bloodType = blood,
                    emergencyName = emName.trim(), emergencyPhone = emPhone.trim(), licenseNumber = licNum.trim(), licenseExpiry = licExp) }
                editing = false
            }, Modifier.weight(1.4f))
        }
    }
}

@Composable
private fun PCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    NaveCard(container = MaterialTheme.colorScheme.surfaceContainerLow) {
        Text(title, Modifier.semantics { heading() }, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
private fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, warn: Boolean = false, onClick: (() -> Unit)? = null) {
    val cs = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).then(if (onClick != null && value.isNotBlank()) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, null, tint = if (warn) cs.error else cs.primary)
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
            Text(value.ifBlank { "—" }, style = MaterialTheme.typography.titleMedium, color = if (warn) cs.error else cs.onSurface)
        }
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier) {
    val cs = MaterialTheme.colorScheme
    Column(modifier.clip(MaterialTheme.shapes.small).background(cs.surfaceContainerHigh).padding(12.dp).semantics(mergeDescendants = true) {}) {
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        Text(label, style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
    }
}
