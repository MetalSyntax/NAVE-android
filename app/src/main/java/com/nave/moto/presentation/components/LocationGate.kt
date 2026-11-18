package com.nave.moto.presentation.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nave.moto.presentation.theme.tr

fun hasFineLocation(ctx: Context) =
    ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

fun isGpsEnabled(ctx: Context) =
    runCatching { (ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager).isProviderEnabled(LocationManager.GPS_PROVIDER) }.getOrDefault(false)

private enum class Step { None, Rationale, Denied, LocationOff }

/**
 * Puerta de ubicación para iniciar una ruta: permiso en ejecución (con explicación) y
 * comprobación de que el GPS del teléfono esté encendido (lleva a Ajustes si no).
 * Devuelve la función que hay que llamar al tocar "Iniciar ruta".
 * [onReady] = todo listo para GPS; [onWithoutGps] = el piloto elige seguir sin GPS.
 */
@Composable
fun rememberLocationGate(onReady: () -> Unit, onWithoutGps: () -> Unit): () -> Unit {
    val ctx = LocalContext.current
    var step by remember { mutableStateOf(Step.None) }
    var awaitingSettings by remember { mutableStateOf(false) }
    val onReadyNow by rememberUpdatedState(onReady)

    fun proceed() {
        step = if (!hasFineLocation(ctx)) Step.Rationale else if (!isGpsEnabled(ctx)) Step.LocationOff else { Step.None.also { onReadyNow() } }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (hasFineLocation(ctx)) proceed() else step = Step.Denied
    }

    // Al volver de Ajustes del sistema, reintenta sin pedir nada más al piloto
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME && awaitingSettings) {
                awaitingSettings = false
                if (hasFineLocation(ctx) && isGpsEnabled(ctx)) { step = Step.None; onReadyNow() }
            }
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }

    when (step) {
        Step.Rationale -> NaveModal(tr("Permitir ubicación", "Allow location"), { step = Step.None }) {
            Text(tr("NAVE usa el GPS de tu teléfono solo mientras una ruta está en curso, para medir los kilómetros recorridos. No se comparte con nadie.",
                "NAVE uses your phone GPS only while a route is running, to measure distance. It is never shared."), style = MaterialTheme.typography.bodyLarge)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryButton(tr("Permitir ubicación", "Allow location"), {
                    val perms = buildList {
                        add(Manifest.permission.ACCESS_FINE_LOCATION); add(Manifest.permission.ACCESS_COARSE_LOCATION)
                        if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS) // aviso de "ruta en curso"
                    }
                    launcher.launch(perms.toTypedArray())
                }, Modifier.fillMaxWidthCompat())
                TonalButton(tr("Iniciar sin GPS", "Start without GPS"), { step = Step.None; onWithoutGps() }, Modifier.fillMaxWidthCompat())
            }
        }
        Step.Denied -> NaveModal(tr("Sin permiso de ubicación", "Location permission missing"), { step = Step.None }) {
            Text(tr("Necesitamos la ubicación precisa para medir la ruta. Actívala en los ajustes de la app, o inicia la ruta sin GPS y registra el odómetro a mano.",
                "We need precise location to measure the route. Enable it in app settings, or start without GPS and enter the odometer by hand."), style = MaterialTheme.typography.bodyLarge)
            PrimaryButton(tr("Abrir ajustes de la app", "Open app settings"), {
                awaitingSettings = true
                ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", ctx.packageName, null)))
            }, Modifier.fillMaxWidthCompat())
            TonalButton(tr("Iniciar sin GPS", "Start without GPS"), { step = Step.None; onWithoutGps() }, Modifier.fillMaxWidthCompat())
        }
        Step.LocationOff -> NaveModal(tr("Enciende el GPS", "Turn on GPS"), { step = Step.None }) {
            Text(tr("La ubicación del teléfono está apagada. Enciéndela para registrar la ruta y vuelve a NAVE.",
                "Phone location is off. Turn it on to record the route, then come back to NAVE."), style = MaterialTheme.typography.bodyLarge)
            PrimaryButton(tr("Abrir ajustes de ubicación", "Open location settings"), {
                awaitingSettings = true
                ctx.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }, Modifier.fillMaxWidthCompat())
            TonalButton(tr("Iniciar sin GPS", "Start without GPS"), { step = Step.None; onWithoutGps() }, Modifier.fillMaxWidthCompat())
        }
        Step.None -> {}
    }
    return { proceed() }
}

private fun Modifier.fillMaxWidthCompat() = fillMaxWidth()
