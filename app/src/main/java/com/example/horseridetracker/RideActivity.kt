package com.example.horseridetracker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.Alignment
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.android.gms.location.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

class RideActivity : ComponentActivity() {
    private lateinit var map: MapView
    private lateinit var locationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    private var lastLocation: Location? = null
    private val trackPoints = mutableListOf<Location>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val horseName = intent.getStringExtra("horseName") ?: "Neznámý kůň"

        // OSM konfigurace
        Configuration.getInstance().load(applicationContext, getSharedPreferences("osm", MODE_PRIVATE))

        // Povolení polohy
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

        // Získání poslední aktivity z SharedPreferences
        val prefs = getSharedPreferences("horses", MODE_PRIVATE)
        val lastActKey = "lastActivity_" + horseName
        val lastActMillis = prefs.getLong(lastActKey, 0L)
        val lastActText = if (lastActMillis > 0) {
            val df = SimpleDateFormat("dd.MM.yy", Locale.getDefault())
            df.format(Date(lastActMillis))
        } else "Žádná předchozí aktivita"

        setContent {
            var currentSpeed by remember { mutableStateOf(0.0) }
            var currentMode by remember { mutableStateOf("Stojí") }
            var totalDistance by remember { mutableStateOf(0.0) }
            var countStanding by remember { mutableStateOf(0f) }
            var countStep by remember { mutableStateOf(0f) }
            var countTrot by remember { mutableStateOf(0f) }
            var countCanter by remember { mutableStateOf(0f) }
            var isRecording by remember { mutableStateOf(true) }
            val speedBuffer = remember { mutableStateListOf<Pair<Long, Double>>() }

            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                // Mapa
                AndroidView(
                    factory = { ctx ->
                        MapView(ctx).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)
                            controller.setZoom(18.0)
                        }.also { map = it }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                )

                Spacer(Modifier.height(4.dp))
                Text("Rychlost: ${"%.1f".format(currentSpeed)} km/h", Modifier.padding(vertical = 2.dp))
                Text("Stav: $currentMode", Modifier.padding(vertical = 2.dp))
                Text("Vzdálenost: ${"%.2f".format(totalDistance)} km", Modifier.padding(vertical = 2.dp))
                Text("Kůň: $horseName", Modifier.padding(vertical = 2.dp))

                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Poslední aktivita: $lastActText",
                    modifier = Modifier.align(Alignment.Start),
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(Modifier.height(16.dp))
                AndroidView(
                    factory = { ctx ->
                        PieChart(ctx).apply {
                            description.isEnabled = false
                            legend.isEnabled = true
                            legend.orientation = Legend.LegendOrientation.HORIZONTAL
                            legend.isWordWrapEnabled = true
                            setDrawEntryLabels(false)
                            setUsePercentValues(true)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    update = { pie ->
                        val entries = listOf(
                            PieEntry(countStanding, "Stání"),
                            PieEntry(countStep, "Krok"),
                            PieEntry(countTrot, "Klus"),
                            PieEntry(countCanter, "Cval")
                        )
                        val set = PieDataSet(entries, "").apply {
                            colors = listOf(
                                android.graphics.Color.GRAY,
                                android.graphics.Color.GREEN,
                                android.graphics.Color.rgb(255, 165, 0),
                                android.graphics.Color.RED
                            )
                            sliceSpace = 2f
                            setDrawValues(false)
                        }
                        pie.data = PieData(set)
                        pie.invalidate()
                    }
                )

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (isRecording) {
                            stopTracking()
                            isRecording = false
                            val gpxFile = saveGpx()
                            // Uložení timestampu aktivity
                            prefs.edit().putLong(lastActKey, System.currentTimeMillis()).apply()
                            sendGpxByEmail(gpxFile)
                            finish()
                        }
                    },
                    enabled = isRecording,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Ukončit a odeslat e-mailem")
                }

                LaunchedEffect(isRecording) {
                    if (isRecording) {
                        startTracking { location, speed, deltaKm ->
                            trackPoints.add(location)
                            totalDistance += deltaKm
                            val now = System.currentTimeMillis()
                            speedBuffer.add(now to speed)
                            speedBuffer.removeAll { now - it.first > 5000 }
                            val avgSpeed = speedBuffer.map { it.second }.ifEmpty { listOf(speed) }.average()
                            currentSpeed = avgSpeed
                            when {
                                avgSpeed < 2.0 -> { currentMode = "Stojí"; countStanding += 1f }
                                abs(avgSpeed - 4.0) < 1.5 -> { currentMode = "Krok"; countStep += 1f }
                                abs(avgSpeed - 6.0) < 1.5 -> { currentMode = "Klus"; countTrot += 1f }
                                else -> { currentMode = "Cval"; countCanter += 1f }
                            }
                            drawTrackSegment(location, avgSpeed)
                        }
                    }
                }
            }
        }
    }

    private fun sendGpxByEmail(file: File) {
        val uri: Uri = FileProvider.getUriForFile(
            this,
            "$packageName.provider",
            file
        )
        val recipient = arrayOf("rolfovo@gmail.com") // Zde vložte cílový e-mail
        val emailIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/gpx+xml"
            putExtra(Intent.EXTRA_EMAIL, recipient)
            putExtra(Intent.EXTRA_SUBJECT, "GPX trasa jízdy")
            putExtra(Intent.EXTRA_TEXT, "Ahoj, zde je trasa mé jízdy v GPX souboru.")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(emailIntent, "Odeslat GPX e-mailem"))
    }

    private fun startTracking(onUpdate: (Location, Double, Double) -> Unit) {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000).build()
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                val prev = lastLocation
                val deltaKm = prev?.distanceTo(loc)?.div(1000.0) ?: 0.0
                val speed = prev?.let { p ->
                    val d = p.distanceTo(loc) / 1000.0
                    val t = (loc.time - p.time) / 3600000.0
                    if (t > 0) d / t else 0.0
                } ?: 0.0
                onUpdate(loc, speed, deltaKm)
                lastLocation = loc
            }
        }
        locationClient.requestLocationUpdates(request, locationCallback!!, mainLooper)
    }

    private fun stopTracking() {
        locationCallback?.let { locationClient.removeLocationUpdates(it) }
        locationCallback = null
    }

    private fun saveGpx(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val dir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        val file = File(dir, "track_$timestamp.gpx")
        file.parentFile?.mkdirs()
        FileOutputStream(file).use { fos ->
            fos.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<gpx version=\"1.1\" creator=\"HorseRideTracker\">\n<trk><name>Jížda_$timestamp</name><trkseg>\n".toByteArray())
            trackPoints.forEach { loc ->
                val time = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date(loc.time))
                fos.write("<trkpt lat=\"${loc.latitude}\" lon=\"${loc.longitude}\"><time>$time</time></trkpt>\n".toByteArray())
            }
            fos.write("</trkseg></trk>\n</gpx>\n".toByteArray())
        }
        return file
    }

    private fun drawTrackSegment(location: Location, speed: Double) {
        if (!::map.isInitialized) return
        lastLocation?.let { prev ->
            val color = when {
                speed < 2.0               -> android.graphics.Color.GRAY
                abs(speed - 4.0) < 1.5    -> android.graphics.Color.GREEN
                abs(speed - 6.0) < 1.5    -> android.graphics.Color.rgb(255,165,0)
                else                       -> android.graphics.Color.RED
            }
            val line = Polyline().apply {
                outlinePaint.color = color
                outlinePaint.strokeWidth = 11f
                setPoints(listOf(
                    GeoPoint(prev.latitude, prev.longitude),
                    GeoPoint(location.latitude, location.longitude)
                ))
            }
            map.overlays.add(line)
            map.invalidate()
        }
        map.controller.setCenter(GeoPoint(location.latitude, location.longitude))
    }
}