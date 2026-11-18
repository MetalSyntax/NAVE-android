package com.nave.moto.data.tracking

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.nave.moto.MainActivity
import com.nave.moto.domain.tracking.TrackAccumulator
import com.nave.moto.domain.tracking.TrackPoint

/**
 * GPS de ruta con la pantalla apagada: servicio en primer plano tipo "location".
 * Se arranca siempre desde la app visible (requisito de Android 14 para el tipo location) y
 * usa solo ubicación "mientras se usa", sin ACCESS_BACKGROUND_LOCATION.
 */
class RouteTrackingService : Service(), LocationListener {
    private lateinit var lm: LocationManager
    private var routeId = -1L
    private var acc = TrackAccumulator()
    private var last: Location? = null

    override fun onBind(intent: Intent?) = null

    override fun onCreate() {
        super.onCreate()
        lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP || intent == null) { stopTracking(); return START_NOT_STICKY }
        routeId = intent.getLongExtra(EXTRA_ROUTE, -1)
        val intervalMs = intent.getIntExtra(EXTRA_INTERVAL, 3).coerceIn(1, 60) * 1000L
        createChannel()
        ServiceCompat.startForeground(this, NOTIF_ID, notification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        acc = RouteTracker.restore(this, routeId)
        RouteTracker.update { TrackState(routeId, true, acc.distanceM, acc.accepted, gpsEnabled = gpsOn()) }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            stopTracking(); return START_NOT_STICKY
        }
        try {
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, intervalMs, 0f, this, Looper.getMainLooper())
        } catch (_: SecurityException) { stopTracking() } catch (_: IllegalArgumentException) { stopTracking() }
        return START_NOT_STICKY
    }

    private fun gpsOn() = runCatching { lm.isProviderEnabled(LocationManager.GPS_PROVIDER) }.getOrDefault(false)

    override fun onLocationChanged(loc: Location) {
        val p = TrackPoint(loc.latitude, loc.longitude, loc.time, if (loc.hasAccuracy()) loc.accuracy else 0f)
        if (acc.add(p)) RouteTracker.append(this, routeId, p)
        last = loc
        RouteTracker.update {
            it.copy(distanceM = acc.distanceM, points = acc.accepted, accuracyM = if (loc.hasAccuracy()) loc.accuracy else null,
                speedKmh = if (loc.hasSpeed()) loc.speed * 3.6f else null, gpsEnabled = true)
        }
    }

    override fun onProviderEnabled(provider: String) { RouteTracker.update { it.copy(gpsEnabled = true) } }
    override fun onProviderDisabled(provider: String) { RouteTracker.update { it.copy(gpsEnabled = false, speedKmh = null) } }
    @Deprecated("API < 29") override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

    private fun stopTracking() {
        runCatching { lm.removeUpdates(this) }
        RouteTracker.update { it.copy(running = false, speedKmh = null) }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() { runCatching { lm.removeUpdates(this) }; super.onDestroy() }

    private fun createChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "Ruta en curso", NotificationManager.IMPORTANCE_LOW))
    }

    private fun notification(): Notification {
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("NAVE · ruta en curso")
            .setContentText("Registrando tu recorrido con GPS")
            .setContentIntent(open).setOngoing(true).setCategory(NotificationCompat.CATEGORY_NAVIGATION).build()
    }

    companion object {
        private const val CHANNEL = "route_tracking"
        private const val NOTIF_ID = 41
        private const val ACTION_STOP = "com.nave.moto.STOP_TRACKING"
        private const val EXTRA_ROUTE = "route"
        private const val EXTRA_INTERVAL = "interval"

        fun start(ctx: Context, routeId: Long, intervalSec: Int) {
            ContextCompat.startForegroundService(ctx, Intent(ctx, RouteTrackingService::class.java)
                .putExtra(EXTRA_ROUTE, routeId).putExtra(EXTRA_INTERVAL, intervalSec))
        }
        fun stop(ctx: Context) {
            if (RouteTracker.state.value.running) ctx.startService(Intent(ctx, RouteTrackingService::class.java).setAction(ACTION_STOP))
        }
    }
}
