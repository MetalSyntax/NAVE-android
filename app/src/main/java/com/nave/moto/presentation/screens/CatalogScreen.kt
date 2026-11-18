package com.nave.moto.presentation.screens

import androidx.compose.runtime.saveable.rememberSaveable
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.nave.moto.domain.model.CatalogModel
import com.nave.moto.domain.model.CatalogSpec
import com.nave.moto.presentation.components.*
import com.nave.moto.presentation.theme.tr
import com.nave.moto.presentation.viewmodel.NaveMotoViewModel

@Composable
private fun CatalogThumb(vm: NaveMotoViewModel, m: CatalogModel, modifier: Modifier) {
    val bmp by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, m.id) {
        value = if (m.hasThumb) vm.thumb(m.id)?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() } else null
    }
    Box(modifier.clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.surfaceContainerHigh), contentAlignment = Alignment.Center) {
        bmp?.let { Image(it, m.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
            ?: Icon(Icons.Default.TwoWheeler, null, tint = MaterialTheme.colorScheme.primary)
    }
}

/** Catálogo empaquetado: buscar marca/modelo, ver ficha técnica y (en modo pick) usarlo para crear la moto. */
@Composable
fun CatalogScreen(
    vm: NaveMotoViewModel,
    pick: Boolean,
    onBack: () -> Unit,
    onUse: (CatalogModel) -> Unit,
    onManual: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    var query by rememberSaveable { mutableStateOf("") }
    var brand by rememberSaveable { mutableStateOf<String?>(null) }
    var brands by remember { mutableStateOf(emptyList<String>()) }
    var models by remember { mutableStateOf(emptyList<CatalogModel>()) }
    var selected by remember { mutableStateOf<CatalogModel?>(null) }
    var specs by remember { mutableStateOf(emptyList<CatalogSpec>()) }

    LaunchedEffect(Unit) { brands = vm.brands() }
    LaunchedEffect(query, brand) {
        val b = brand
        models = if (query.isNotBlank()) vm.search(query).filter { b == null || it.brand == b }
        else if (b != null) vm.models(b) else vm.search("")
    }
    LaunchedEffect(selected) { specs = selected?.let { vm.specs(it.id) } ?: emptyList() }

    Column(Modifier.fillMaxSize().background(cs.surface).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton({ if (selected != null) selected = null else onBack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, tr("Atrás", "Back")) }
            Text(selected?.let { "${it.brand} ${it.name}" } ?: tr("Catálogo de motos", "Motorcycle catalog"), style = MaterialTheme.typography.titleLarge, maxLines = 1)
        }
        val sel = selected
        if (sel == null) {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                NaveField(query, { query = it }, tr("Buscar marca o modelo", "Search brand or model"),
                    trailing = { Icon(Icons.Default.Search, null) })
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NavePill(tr("Todas", "All"), brand == null, { brand = null })
                    brands.forEach { b -> NavePill(b, brand == b, { brand = if (brand == b) null else b }) }
                }
                if (pick) TonalButton(tr("Mi moto no está: crear manualmente", "Not listed: create manually"), onManual, Modifier.fillMaxWidth())
            }
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(models, key = { it.id }) { m ->
                    Row(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(cs.surfaceContainer).clickable { selected = m }.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CatalogThumb(vm, m, Modifier.size(64.dp))
                        Column(Modifier.weight(1f)) {
                            Eyebrow(m.brand)
                            Text(m.name, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                            Text(listOfNotNull(m.cc.takeIf { it > 0 }?.let { "$it cc" }, m.tankL.takeIf { it > 0 }?.let { "${fmt1(it)} L" }, m.maxSpeedKmh.takeIf { it > 0 }?.let { "$it km/h" }).joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                        }
                    }
                }
                item { Text(tr("${models.size} modelos", "${models.size} models"), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant) }
            }
        } else {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CatalogThumb(vm, sel, Modifier.fillMaxWidth().aspectRatio(4f / 3f))
                Eyebrow("${sel.brand} · ${sel.category}")
                Text(sel.name, style = MaterialTheme.typography.displaySmall)
                NaveCard(container = cs.surfaceContainerLow, padding = 14.dp) {
                    SpecRow(tr("Cilindrada", "Displacement"), sel.cc.takeIf { it > 0 }?.let { "$it cc" })
                    SpecRow(tr("Tanque", "Tank"), sel.tankL.takeIf { it > 0 }?.let { "${fmt1(it)} L" })
                    SpecRow(tr("Rendimiento", "Efficiency"), sel.kmL.takeIf { it > 0 }?.let { "${fmt1(it)} km/L" })
                    SpecRow(tr("Vel. máxima", "Top speed"), sel.maxSpeedKmh.takeIf { it > 0 }?.let { "$it km/h" })
                    SpecRow(tr("Peso", "Weight"), sel.weightKg.takeIf { it > 0 }?.let { "$it kg" })
                    SpecRow(tr("Colores", "Colors"), sel.colors.takeIf { it.isNotBlank() })
                }
                specs.forEach { sp ->
                    NaveCard(container = cs.surfaceContainerLow, padding = 14.dp) {
                        Eyebrow(sp.section)
                        Spacer(Modifier.height(6.dp))
                        sp.body.lines().filter { it.isNotBlank() }.forEach { line ->
                            val k = line.substringBefore(": ", ""); val v = line.substringAfter(": ", line)
                            if (k.isNotEmpty() && v != line) SpecRow(k, v) else Text(line, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                if (pick) PrimaryButton(tr("Usar esta moto", "Use this motorcycle"), { onUse(sel) }, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun SpecRow(k: String, v: String?) {
    if (v == null) return
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(k, Modifier.weight(0.45f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(v, Modifier.weight(0.55f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
    }
}
