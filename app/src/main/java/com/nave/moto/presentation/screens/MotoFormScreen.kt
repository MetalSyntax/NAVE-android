package com.nave.moto.presentation.screens

import androidx.compose.runtime.saveable.rememberSaveable
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nave.moto.domain.model.CatalogModel
import com.nave.moto.domain.model.Motorcycle
import com.nave.moto.presentation.components.*
import com.nave.moto.presentation.theme.tr
import java.time.LocalDate

/** Alta / edición de moto. [prefill] viene del catálogo; [initial] es la moto a editar. */
@Composable
fun MotoFormScreen(
    initial: Motorcycle?,
    prefill: CatalogModel?,
    onBack: () -> Unit,
    onSave: (Motorcycle, Uri?, Long?) -> String?
) {
    val cs = MaterialTheme.colorScheme
    val year0 = LocalDate.now().year
    var brand by rememberSaveable { mutableStateOf(initial?.brand ?: prefill?.brand ?: "") }
    var model by rememberSaveable { mutableStateOf(initial?.model ?: prefill?.name ?: "") }
    var year by rememberSaveable { mutableStateOf((initial?.year ?: year0).toString()) }
    var cc by rememberSaveable { mutableStateOf((initial?.cc ?: prefill?.cc?.takeIf { it > 0 })?.toString() ?: "") }
    var category by rememberSaveable { mutableStateOf(initial?.category ?: prefill?.category ?: "PASEO") }
    var color by rememberSaveable { mutableStateOf(initial?.color ?: prefill?.colors?.substringBefore(",")?.trim() ?: "") }
    var plate by rememberSaveable { mutableStateOf(initial?.plate ?: "") }
    var vin by rememberSaveable { mutableStateOf(initial?.vin ?: "") }
    var tank by rememberSaveable { mutableStateOf(fmt1(initial?.tankCapacityLiters ?: prefill?.tankL?.takeIf { it > 0 } ?: 13.0)) }
    var eff by rememberSaveable { mutableStateOf(fmt1(initial?.efficiencyKmPerLiter ?: prefill?.kmL?.takeIf { it > 0 } ?: 38.0)) }
    var odo by rememberSaveable { mutableStateOf((initial?.currentOdometerKm ?: 0).toString()) }
    var insurer by rememberSaveable { mutableStateOf(initial?.insurer ?: "") }
    var policy by rememberSaveable { mutableStateOf(initial?.policyNumber ?: "") }
    var policyUntil by rememberSaveable { mutableStateOf(initial?.policyValidUntil ?: "") }
    var photo by remember { mutableStateOf<Uri?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val badPolicy = tr("La fecha de la póliza debe ser AAAA-MM-DD.", "Policy date must be YYYY-MM-DD.")
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { photo = it ?: photo }

    Column(Modifier.fillMaxSize().background(cs.surface).statusBarsPadding().navigationBarsPadding().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onBack, Modifier.size(56.dp)) { Icon(Icons.AutoMirrored.Filled.ArrowBack, tr("Atrás", "Back")) }
            Text(if (initial == null) tr("Agregar moto", "Add motorcycle") else tr("Editar moto", "Edit motorcycle"), style = MaterialTheme.typography.titleLarge)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (prefill != null) NaveCard(container = cs.secondaryContainer, padding = 12.dp, shape = MaterialTheme.shapes.medium) {
                Text(tr("Datos precargados del catálogo: ${prefill.brand} ${prefill.name}. Ajusta lo que necesites.", "Prefilled from catalog: ${prefill.brand} ${prefill.name}. Adjust as needed."),
                    style = MaterialTheme.typography.labelMedium, color = cs.onSecondaryContainer)
            }
            NaveField(brand, { brand = it }, tr("Marca", "Brand"))
            NaveField(model, { model = it }, tr("Modelo", "Model"))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NaveField(year, { year = it.filter(Char::isDigit).take(4) }, tr("Año", "Year"), Modifier.weight(1f), number = true)
                NaveField(cc, { cc = it.filter(Char::isDigit) }, "cc", Modifier.weight(1f), number = true)
            }
            NaveField(category, { category = it.uppercase() }, tr("Categoría", "Category"))
            NaveField(color, { color = it }, tr("Color", "Color"))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NaveField(plate, { plate = it.uppercase() }, tr("Placa", "Plate"), Modifier.weight(1f))
                NaveField(odo, { odo = it.filter(Char::isDigit) }, tr("Odómetro (km)", "Odometer (km)"), Modifier.weight(1f), number = true)
            }
            NaveField(vin, { vin = it.uppercase() }, "VIN / ${tr("Serial", "Serial")}")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NaveField(tank, { tank = it }, tr("Tanque (L)", "Tank (L)"), Modifier.weight(1f), decimal = true)
                NaveField(eff, { eff = it }, "km/L", Modifier.weight(1f), decimal = true)
            }
            NaveField(insurer, { insurer = it }, tr("Aseguradora", "Insurer"))
            NaveField(policy, { policy = it }, tr("N.º de póliza", "Policy no."))
            DateField(tr("Vencimiento de la póliza", "Policy expiry"), policyUntil, { policyUntil = it }, expiry = true, optional = true)
            TonalButton(
                if (photo != null) tr("Foto elegida ✓ (cambiar)", "Photo chosen ✓ (change)") else if (prefill?.hasThumb == true && initial == null) tr("Foto del catálogo · elegir otra", "Catalog photo · pick another") else tr("Elegir foto", "Pick photo"),
                { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, Modifier.fillMaxWidth(), icon = Icons.Default.Image
            )
            ErrorText(error)
            Spacer(Modifier.height(4.dp))
        }
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TonalButton(tr("Cancelar", "Cancel"), onBack, Modifier.weight(1f))
            PrimaryButton(tr("Guardar", "Save"), {
                val now = LocalDate.now().toString()
                val base = initial ?: Motorcycle(brand = "", model = "", year = 0, category = "", cc = 0, color = "", plate = "", currentOdometerKm = 0, createdAt = now, updatedAt = now)
                val policyOk = policyUntil.isBlank() || runCatching { LocalDate.parse(policyUntil) }.isSuccess
                error = if (!policyOk) badPolicy else onSave(
                    base.copy(
                        brand = brand.trim(), model = model.trim(), year = year.toIntOrNull() ?: 0, category = category.ifBlank { "PASEO" }, cc = cc.toIntOrNull() ?: 0,
                        color = color.trim(), plate = plate.trim(), vin = vin.trim(), tankCapacityLiters = tank.replace(',', '.').toDoubleOrNull() ?: 13.0,
                        efficiencyKmPerLiter = eff.replace(',', '.').toDoubleOrNull() ?: 38.0, currentOdometerKm = odo.toIntOrNull() ?: -1,
                        insurer = insurer.trim(), policyNumber = policy.trim(), policyValidUntil = policyUntil.trim()
                    ),
                    photo, if (initial == null && photo == null) prefill?.takeIf { it.hasThumb }?.id else null
                )
            }, Modifier.weight(1.4f))
        }
    }
}
