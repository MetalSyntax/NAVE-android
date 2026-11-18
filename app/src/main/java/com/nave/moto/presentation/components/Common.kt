package com.nave.moto.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.*
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Add
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import com.nave.moto.domain.rules.*
import com.nave.moto.presentation.theme.LocalEs
import com.nave.moto.presentation.theme.LocalHaptics
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.nave.moto.presentation.theme.Outfit
import com.nave.moto.presentation.theme.tr
import java.io.File
import java.text.NumberFormat
import java.util.Locale

private val nf = NumberFormat.getIntegerInstance(Locale("es"))
fun fmt(n: Int): String = nf.format(n)
fun fmt1(d: Double): String = if (d % 1.0 == 0.0) d.toLong().toString() else String.format(Locale.US, "%.1f", d)

/** Vibración corta al confirmar acciones (se puede apagar en Ajustes). */
@Composable
fun rememberHaptic(strong: Boolean = true): () -> Unit {
    val h = LocalHapticFeedback.current
    val on = LocalHaptics.current
    return { if (on) h.performHapticFeedback(if (strong) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove) }
}

/** Etiqueta pequeña en color secundario (text-xs font-semibold text-secondary). */
@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier) =
    Text(text, modifier, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)

@Composable
fun ScreenTitle(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(title, Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
        if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun NaveCard(
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.surfaceContainer,
    shape: androidx.compose.ui.graphics.Shape = MaterialTheme.shapes.large,
    padding: Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier.fillMaxWidth().clip(shape).background(container).padding(padding), content = content)
}
typealias Dp = androidx.compose.ui.unit.Dp

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, icon: ImageVector? = null) {
    val buzz = rememberHaptic()
    Button(
        { buzz(); onClick() }, modifier.heightIn(min = 56.dp), enabled = enabled, shape = MaterialTheme.shapes.small,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
    ) {
        if (icon != null) { Icon(icon, null, Modifier.size(24.dp)); Spacer(Modifier.width(8.dp)) }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun TonalButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val buzz = rememberHaptic(strong = false)
    Button(
        { buzz(); onClick() }, modifier.heightIn(min = 56.dp), shape = MaterialTheme.shapes.small,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh, contentColor = MaterialTheme.colorScheme.onSurface),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        if (icon != null) { Icon(icon, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(8.dp)) }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun OutlineButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick, modifier.heightIn(min = 56.dp), shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) { Text(text, style = MaterialTheme.typography.labelLarge) }
}

@Composable
fun SquareIconButton(icon: ImageVector, desc: String, onClick: () -> Unit, tint: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Box(
        Modifier.size(56.dp).clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(role = Role.Button, onClickLabel = desc, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icon, desc, Modifier.size(24.dp), tint = tint) }
}

/** Pastilla seleccionable (bg-secondary-container cuando está activa). */
@Composable
fun NavePill(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val buzz = rememberHaptic(strong = false)
    Box(
        modifier.heightIn(min = 48.dp).clip(MaterialTheme.shapes.small)
            .background(if (selected) cs.secondaryContainer else cs.surfaceContainerLow)
            .selectable(selected, role = Role.RadioButton, onClick = { buzz(); onClick() }).padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            // El estado no depende solo del color: la pastilla activa lleva una marca
            if (selected) Icon(Icons.Default.Check, null, Modifier.size(16.dp), tint = cs.onSecondaryContainer)
            Text(text, style = MaterialTheme.typography.labelLarge, color = if (selected) cs.onSecondaryContainer else cs.onSurfaceVariant, maxLines = 1)
        }
    }
}

@Composable
fun NaveField(
    value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier,
    number: Boolean = false, decimal: Boolean = false, singleLine: Boolean = true, placeholder: String? = null,
    readOnly: Boolean = false, phone: Boolean = false, trailing: @Composable (() -> Unit)? = null
) {
    TextField(
        value, onValueChange, modifier.fillMaxWidth().heightIn(min = 56.dp), readOnly = readOnly,
        label = { Text(label) }, placeholder = placeholder?.let { { Text(it) } }, singleLine = singleLine,
        shape = MaterialTheme.shapes.small, trailingIcon = trailing,
        keyboardOptions = KeyboardOptions(keyboardType = if (phone) KeyboardType.Phone else if (decimal) KeyboardType.Decimal else if (number) KeyboardType.Number else KeyboardType.Text),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
            unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant
        )
    )
}

/** Diálogo estilo modal web: rounded-2xl bg-surface-container + borde outline-variant. */
@Composable
fun NaveModal(title: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(Modifier.padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, Modifier.weight(1f).semantics { heading() }, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                    IconButton(onDismiss, Modifier.size(56.dp)) { Icon(Icons.Default.Close, tr("Cerrar", "Close")) }
                }
                content()
            }
        }
    }
}

@Composable
fun ErrorText(msg: String?) {
    if (msg != null) Text(
        msg, Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)).padding(10.dp),
        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error
    )
}

@Composable
fun ModalActions(onCancel: () -> Unit, confirmText: String, onConfirm: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        TonalButton(tr("Cancelar", "Cancel"), onCancel, Modifier.weight(1f))
        PrimaryButton(confirmText, onConfirm, Modifier.weight(1.4f))
    }
}

@Composable
fun ConfirmDialog(text: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss, shape = MaterialTheme.shapes.large, containerColor = MaterialTheme.colorScheme.surfaceContainer,
        text = { Text(text) },
        confirmButton = { TextButton({ onConfirm(); onDismiss() }, Modifier.heightIn(min = 56.dp)) { Text(confirm, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onDismiss, Modifier.heightIn(min = 56.dp)) { Text(tr("Cancelar", "Cancel")) } }
    )
}

@Composable
fun MotoPhoto(relPath: String?, title: String, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val file = relPath?.let { File(ctx.filesDir, it) }?.takeIf { it.exists() }
    if (file != null) {
        AsyncImage(file, title, modifier.clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surfaceContainerHigh), contentScale = ContentScale.Crop)
    } else {
        Column(
            modifier.clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.TwoWheeler, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(6.dp))
            Text(title, style = MaterialTheme.typography.labelMedium.copy(fontFamily = Outfit, fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun EmptyCard(icon: ImageVector, text: String, actionText: String? = null, onAction: (() -> Unit)? = null) {
    NaveCard(container = MaterialTheme.colorScheme.surfaceContainerLow, padding = 28.dp) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(icon, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (actionText != null && onAction != null) PrimaryButton(actionText, onAction)
        }
    }
}

/** Columna de pantalla con scroll vertical (padding lo pone el contenedor). */
@Composable
fun Modifier.verticalScrollColumn(): Modifier = this.fillMaxWidth().verticalScroll(rememberScrollState())


// ---------------------------------------------------------------- Fechas sin teclear

private fun quickLabel(k: QuickKey, es: Boolean) = when (k) {
    QuickKey.TODAY -> if (es) "Hoy" else "Today"
    QuickKey.YESTERDAY -> if (es) "Ayer" else "Yesterday"
    QuickKey.TWO_DAYS -> if (es) "Hace 2 días" else "2 days ago"
    QuickKey.SIX_MONTHS -> if (es) "+6 meses" else "+6 months"
    QuickKey.ONE_YEAR -> if (es) "+1 año" else "+1 year"
    QuickKey.TWO_YEARS -> if (es) "+2 años" else "+2 years"
}

/**
 * Campo de fecha sin teclado: atajos (Hoy, Ayer…) + calendario Material3.
 * [value] es ISO (AAAA-MM-DD) o vacío si [optional]. [expiry] = fechas futuras (póliza).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier, expiry: Boolean = false, optional: Boolean = false) {
    val cs = MaterialTheme.colorScheme
    val es = LocalEs.current
    val today = remember { LocalDate.now() }
    val date = runCatching { LocalDate.parse(value) }.getOrNull()
    var picking by remember { mutableStateOf(false) }
    val shown = date?.format(DateTimeFormatter.ofPattern("EEE d MMM yyyy", java.util.Locale(if (es) "es" else "en")))
        ?: tr("Sin fecha", "No date")
    val calendar = tr("Elegir en el calendario", "Pick from calendar")
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = cs.onSurfaceVariant)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            quickDates(today, expiry).forEach { q -> NavePill(quickLabel(q.key, es), date == q.date, { onChange(q.date.toString()) }) }
            if (optional) NavePill(tr("Sin fecha", "No date"), date == null, { onChange("") })
        }
        val buzz = rememberHaptic(strong = false)
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(MaterialTheme.shapes.small).background(cs.surfaceContainerHigh)
                .clickable(role = Role.Button, onClickLabel = calendar) { buzz(); picking = true }.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.CalendarMonth, null, tint = cs.primary)
            Text(shown, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = cs.onSurface)
            Text(tr("Cambiar", "Change"), style = MaterialTheme.typography.labelLarge, color = cs.primary)
        }
    }
    if (picking) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = (date ?: today).toPickerMillis(),
            selectableDates = object : SelectableDates {
                // Registros: nunca en el futuro. Vencimientos: sin restricción.
                override fun isSelectableDate(utcTimeMillis: Long) = expiry || !pickerMillisToDate(utcTimeMillis).isAfter(today)
            }
        )
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton({ state.selectedDateMillis?.let { onChange(pickerMillisToDate(it).toString()) }; picking = false }, Modifier.heightIn(min = 56.dp)) {
                    Text(tr("Aceptar", "OK"), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton({ picking = false }, Modifier.heightIn(min = 56.dp)) { Text(tr("Cancelar", "Cancel")) } }
        ) { DatePicker(state) }
    }
}

// ---------------------------------------------------------------- Números sin teclear

private fun fmtNum(v: Double, decimal: Boolean) = if (decimal) fmt1(v) else v.toLong().toString()

/**
 * Número con − / + grandes y atajos. [value] es texto (el teclado numérico sigue disponible).
 * [presets] = etiqueta → valor resultante.
 */
@Composable
fun NumberStepper(
    label: String, value: String, onChange: (String) -> Unit, step: Double, modifier: Modifier = Modifier,
    decimal: Boolean = false, min: Double = 0.0, presets: List<Pair<String, Double>> = emptyList()
) {
    val cs = MaterialTheme.colorScheme
    val buzz = rememberHaptic(strong = false)
    fun cur() = value.replace(',', '.').toDoubleOrNull() ?: 0.0
    fun bump(d: Double) { buzz(); onChange(fmtNum((cur() + d).coerceAtLeast(min), decimal)) }
    val less = tr("Menos $label", "Less $label"); val more = tr("Más $label", "More $label")
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StepButton(Icons.Default.Remove, less) { bump(-step) }
            NaveField(
                value, { onChange(if (decimal) it.filter { c -> c.isDigit() || c == '.' || c == ',' } else it.filter(Char::isDigit)) },
                label, Modifier.weight(1f), number = !decimal, decimal = decimal
            )
            StepButton(Icons.Default.Add, more) { bump(step) }
        }
        if (presets.isNotEmpty()) Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            presets.forEach { (t, v) -> NavePill(t, cur() == v, { onChange(fmtNum(v, decimal)) }) }
        }
    }
}

@Composable
private fun StepButton(icon: ImageVector, desc: String, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Box(
        Modifier.size(56.dp).clip(MaterialTheme.shapes.small).background(cs.secondaryContainer)
            .clickable(role = Role.Button, onClickLabel = desc, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icon, desc, Modifier.size(28.dp), tint = cs.onSecondaryContainer) }
}
