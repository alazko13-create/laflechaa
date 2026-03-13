package com.unaflecha.nativeapp

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class DriverMonitorService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val apiClient = NativeApiClient()
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var pollJob: Job? = null
    private var latestLat: Double? = null
    private var latestLng: Double? = null
    private var lastLocationSentAt: Long = 0L

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let {
                latestLat = it.latitude
                latestLng = it.longitude
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        startForeground(Constants.SERVICE_ID, buildServiceNotification())
        requestLocationUpdates()
        startPolling()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        runCatching { fusedLocationClient.removeLocationUpdates(locationCallback) }
        scope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) return

        @Suppress("DEPRECATION")
        val request = LocationRequest.create().apply {
            interval = 5000L
            fastestInterval = 3000L
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        fusedLocationClient.requestLocationUpdates(request, locationCallback, mainLooper)
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = scope.launch {
            while (isActive) {
                try {
                    if (!PrefHelper.isMonitorEnabled(this@DriverMonitorService)) {
                        stopSelf()
                        break
                    }
                    val token = ensureApiToken() ?: run {
                        delay(4000)
                        continue
                    }
                    maybeSendLocation(token)
                    apiClient.pollInbox(token)?.let { trip ->
                        if (trip.id != PrefHelper.getLastTripId(this@DriverMonitorService)) {
                            PrefHelper.setLastTripId(this@DriverMonitorService, trip.id)
                            apiClient.ackTrip(token, trip.id)
                            showIncomingTrip(trip)
                        }
                    }
                } catch (_: Throwable) {
                }
                delay(3000)
            }
        }
    }

    private fun ensureApiToken(): String? {
        val cached = PrefHelper.getLastToken(this)
        if (cached.isNotBlank()) return cached
        val token = apiClient.fetchApiToken() ?: return null
        PrefHelper.setLastToken(this, token)
        return token
    }

    private fun maybeSendLocation(token: String) {
        val lat = latestLat ?: return
        val lng = latestLng ?: return
        val now = System.currentTimeMillis()
        if (now - lastLocationSentAt < 15000) return
        apiClient.pingLocation(token, lat, lng)
        lastLocationSentAt = now
    }

    private fun buildServiceNotification(): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            10,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_SERVICE)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("UnaFlecha monitor nativo")
            .setContentText("Ubicación en segundo plano y monitoreo de viajes activos")
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .build()
    }

    private fun showIncomingTrip(trip: TripPayload) {
        val targetUrl = "${Constants.BASE_URL}/trip_map.php?trip_id=${trip.id}"
        val openIntent = Intent(this, TripLaunchActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_TARGET_URL, targetUrl)
            putExtra("trip_id", trip.id)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val fullPending = PendingIntent.getActivity(
            this,
            trip.id,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nm = getSystemService(NotificationManager::class.java)
        val notification = NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_TRIPS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Nuevo viaje: ${trip.originText}")
            .setContentText("Destino: ${trip.destText} · ${"%.2f".format(trip.fareCup)} CUP")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .setContentIntent(fullPending)
            .setFullScreenIntent(fullPending, true)
            .addAction(android.R.drawable.ic_menu_view, "Abrir", fullPending)
            .build()
        nm.notify(Constants.TRIP_NOTIFICATION_ID + trip.id, notification)

        if (ContextCompat.checkSelfPermission(this, "android.permission.POST_NOTIFICATIONS") == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < 33) {
            startActivity(openIntent)
        }

        if (android.provider.Settings.canDrawOverlays(this)) {
            OverlayBubbleService.show(this, trip)
        }
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, DriverMonitorService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, DriverMonitorService::class.java))
        }
    }
}
