package com.unaflecha.nativeapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class UnaFlechaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(
                    Constants.NOTIFICATION_CHANNEL_SERVICE,
                    "Monitoreo de chofer",
                    NotificationManager.IMPORTANCE_LOW
                ).apply { description = "Ubicación y polling nativo" }
            )
            nm.createNotificationChannel(
                NotificationChannel(
                    Constants.NOTIFICATION_CHANNEL_TRIPS,
                    "Solicitudes de viaje",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Viajes nuevos para chofer"
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                }
            )
        }
    }
}
