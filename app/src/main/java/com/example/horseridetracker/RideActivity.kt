package com.example.horseridetracker

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import kotlin.math.abs

class RideActivity : ComponentActivity() {
    private lateinit var map: MapView
    private lateinit var locationClient: FusedLocationProviderClient
    private var lastLocation: Location? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().load(applicationContext, getSharedPreferences("osm", MODE_PRIVATE))

        // Žádost o povolení polohy
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
        }

        locationClient = LocationServices.getFusedLocationProviderClient(this)

        setContent {
            var currentSpeed by remember { mutableStateOf(0.0) }
            var currentMode by remember { mutableStateOf("Stojí") }

            Column(modifier = Modifier.fillMaxSize()) {
                AndroidView(factory = { context ->
                    map = MapView(context).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(18.0)
                    }
                    map
                }, modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp))

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Rychlost: ${"%.1f".format(currentSpeed)} km/h",
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Text(
                    text = "Chod: $currentMode",
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                // Start sledování polohy
                LaunchedEffect(Unit) {
                    startTracking(
                        onUpdate = { location, speed ->
                            currentSpeed = speed
                            currentMode = when {
                                speed < 2.0 -> "Stojí"
                                abs(speed - 4.0) < 1.5 -> "Krok"
                                abs(speed - 6.0) < 1.5 -> "Klus"
                                else -> "Cval"
                            }
                            drawTrackSegment(location, speed)
                        }
                    )
                }
            }
        }
    }

    private fun startTracking(onUpdate: (Location, Double) -> Unit) {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            3000
        ).build()

        locationClient.requestLocationUpdates(
            locationRequest,
            object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val location = result.lastLocation ?: return
                    val speed = lastLocation?.let { prev ->
                        val distance = prev.distanceTo(location) / 1000.0 // km
                        val time = (location.time - prev.time) / 3600000.0 // h
                        if (time > 0) distance / time else 0.0
                    } ?: 0.0

                    onUpdate(location, speed)
                    lastLocation = location
                }
            },
            mainLooper
        )
    }

    private fun drawTrackSegment(location: Location, speed: Double) {
        val geoPoint = GeoPoint(location.latitude, location.longitude)

        if (!::map.isInitialized) return

        lastLocation?.let { prev ->
            val color = when {
                speed < 2.0 -> android.graphics.Color.GRAY
                abs(speed - 4.0) < 1.5 -> android.graphics.Color.GREEN
                abs(speed - 6.0) < 1.5 -> android.graphics.Color.rgb(255, 165, 0) // oranžová
                else -> android.graphics.Color.RED
            }

            val line = Polyline().apply {
                outlinePaint.color = color
                outlinePaint.strokeWidth = 6.0f
                setPoints(
                    listOf(
                        GeoPoint(prev.latitude, prev.longitude),
                        GeoPoint(location.latitude, location.longitude)
                    )
                )
            }

            map.overlays.add(line)
            map.invalidate()
        }

        map.controller.setCenter(geoPoint)
    }
}
