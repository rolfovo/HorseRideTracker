package com.example.horseridetracker

import android.app.*
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*

class RideService : Service() {

    private val fused by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private val callback = object : LocationCallback() {
        override fun onLocationResult(r: LocationResult) {
            // TODO: ulož / pošli bod
        }
    }

    override fun onCreate() {
        super.onCreate()

        /* notifikační kanál – jednorázově */
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(
                    NotificationChannel(
                        "ride", "Probíhá jízda",
                        NotificationManager.IMPORTANCE_LOW
                    )
                )
        }

        val notif = NotificationCompat.Builder(this, "ride")
            .setSmallIcon(R.drawable.ic_horse)
            .setContentTitle("Jízda probíhá")
            .setContentText("Sleduji trasu 🐎")
            .setOngoing(true)
            .build()

        startForeground(1, notif)   // ← do 5 s od onCreate()

        val req = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 2_000
        ).setMinUpdateDistanceMeters(5f).build()

        fused.requestLocationUpdates(req, callback, mainLooper)
    }

    override fun onDestroy() {
        fused.removeLocationUpdates(callback)
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    override fun onBind(p0: Intent?): IBinder? = null  // nic nenavazuješ
}
