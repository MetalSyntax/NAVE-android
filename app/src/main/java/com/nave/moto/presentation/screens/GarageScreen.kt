package com.nave.moto.presentation.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.nave.moto.domain.model.MotoManual
import com.nave.moto.domain.model.Motorcycle
import com.nave.moto.presentation.components.*
import com.nave.moto.presentation.theme.tr
import com.nave.moto.presentation.viewmodel.NaveUiState
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDate

@Composable
fun GarageScreen(
    ui: NaveUiState,
    onAddMoto: () -> Unit,
    onOpenCatalog: () -> Unit,
    onEditMoto: (Motorcycle) -> Unit,
    onDeleteMoto: (Motorcycle) -> Unit,
    onSelectMoto: (Long) -> Unit,
    onAddManual: (Uri) -> Unit,
    onDeleteManual: (MotoManual) -> Unit,
    onOpenSettings: () -> Unit,
    onPreview: (String) -> String?,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    val ctx = LocalContext.current
    var preview by remember { mutableStateOf<Pair<String, String>?>(null) }
    var delMoto by remember { mutableStateOf<Motorcycle?>(null) }

    val pickManual = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(onAddManual) }

    Column(modifier.verticalScrollColumn(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        ScreenTitle(tr("Garaje y manuales", "Garage & manuals"),
            tr("${ui.motorcycles.size} motocicletas · ${ui.manuals.size} manuales", "${ui.motorcycles.size} motorcycles · ${ui.manuals.size} manuals"))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PrimaryButton(tr("Agregar moto", "Add motorcycle"), onAddMoto, icon = Icons.Default.Add)
            TonalButton(tr("Catálogo", "Catalog"), onOpenCatalog, icon = Icons.Default.Search)
        }

        ui.motorcycles.forEach { m ->
            val active = m.id == ui.activeMoto?.id
            NaveCard(padding = 16.dp) {
                MotoPhoto(m.photoUri, "${m.brand} ${m.model}", Modifier.fillMaxWidth().aspectRatio(16f / 9f))
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Eyebrow("${m.category} · ${m.year} · ${m.cc}cc")
                        Text("${m.brand} ${m.model}", style = MaterialTheme.typography.titleLarge)
                        Text("${tr("Placa", "Plate")} ${m.plate} · ${fmt(m.currentOdometerKm)} km", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                        if (m.insurer.isNotBlank()) Text("${m.insurer} (${m.policyNumber}) · ${tr("Vence", "Exp.")} ${m.policyValidUntil}", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                    }
                    SquareIconButton(Icons.Default.Edit, tr("Editar", "Edit"), { onEditMoto(m) })
                    Spacer(Modifier.width(6.dp))
                    SquareIconButton(Icons.Default.Delete, tr("Eliminar", "Delete"), { delMoto = m })
                }
                Spacer(Modifier.height(12.dp))
                Button({ onSelectMoto(m.id) }, Modifier.fillMaxWidth().heightIn(min = 56.dp), enabled = !active, shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.buttonColors(containerColor = cs.surfaceContainerHigh, contentColor = cs.onSurface,
                        disabledContainerColor = cs.secondaryContainer, disabledContentColor = cs.onSecondaryContainer)) {
                    Text(if (active) tr("Moto activa en tablero", "Active on dashboard") else tr("Seleccionar moto", "Select motorcycle"), fontWeight = FontWeight.Bold)
                }
            }
        }

        // Manuales
        NaveCard(container = cs.surfaceContainerLow) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tr("Manuales técnicos (${ui.manuals.size})", "Technical manuals (${ui.manuals.size})"), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                TonalButton(tr("Subir PDF", "Upload"), { pickManual.launch(arrayOf("application/pdf", "text/*", "image/*")) }, icon = Icons.Default.Upload)
            }
            if (ui.manuals.isEmpty()) Text(tr("Aún no hay manuales. Sube el PDF de tu moto.", "No manuals yet. Upload your bike's PDF."),
                Modifier.padding(top = 10.dp), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
            ui.manuals.forEach { man ->
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(cs.surfaceContainer).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.Description, null, tint = cs.primary)
                    Column(Modifier.weight(1f)) {
                        Text(man.title, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                        Text("${man.fileName} · ${maxOf(1, man.sizeBytes / 1024)} KB · ${man.addedAt}", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, maxLines = 1)
                    }
                    if (man.mimeType.startsWith("text/")) TextButton({
                        preview = man.title to (onPreview(man.localUri) ?: "—")
                    }, Modifier.heightIn(min = 56.dp)) { Text(tr("Leer", "Read"), fontWeight = FontWeight.Bold) }
                    TextButton({
                        runCatching {
                            val f = File(ctx.filesDir, man.localUri)
                            val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", f)
                            ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_VIEW).setDataAndType(uri, man.mimeType).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), man.title))
                        }
                    }, Modifier.heightIn(min = 56.dp)) { Text(tr("Ver", "View"), fontWeight = FontWeight.Bold) }
                    IconButton({ onDeleteManual(man) }, Modifier.size(56.dp)) { Icon(Icons.Default.Delete, tr("Eliminar", "Delete"), tint = cs.onSurfaceVariant) }
                }
            }
        }

        // Ajustes: pantalla propia
        TonalButton(tr("Ajustes de la app", "App settings"), onOpenSettings, Modifier.fillMaxWidth(), icon = Icons.Default.Settings)
        Spacer(Modifier.height(8.dp))
    }

    preview?.let { (t, body) ->
        NaveModal(t, { preview = null }) {
            Text(body, style = MaterialTheme.typography.bodyMedium)
            TonalButton(tr("Cerrar", "Close"), { preview = null }, Modifier.fillMaxWidth())
        }
    }
    delMoto?.let { m -> ConfirmDialog(tr("¿Eliminar ${m.brand} ${m.model} y todos sus registros?", "Delete ${m.brand} ${m.model} and all its records?"), tr("Eliminar", "Delete"), { onDeleteMoto(m) }, { delMoto = null }) }
}
