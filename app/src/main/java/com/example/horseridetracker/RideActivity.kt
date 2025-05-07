package com.example.horseridetracker

/* ---------- importy ---------- */
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.*
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

    /* ✨ ANTI‑JUMP PARAMETRY */
    private val MIN_DIST_M  = 1f    // posílej body jen při posunu ≥ 1 m
    private val MAX_JUMP_M  = 10f   // cokoli větší = podezřelý skok
    private val MAX_SPEED_K = 40    // km/h – pro koně víc nedává smysl

    private lateinit var map: MapView
    private lateinit var locationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    private var lastLocation: Location? = null
    private val trackPoints = mutableListOf<Location>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val horseName = intent.getStringExtra("horseName") ?: "Neznámý kůň"

        /* OSM */
        Configuration.getInstance().load(applicationContext, getSharedPreferences("osm", MODE_PRIVATE))

        /* runtime permission */
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }

        locationClient = LocationServices.getFusedLocationProviderClient(this)

        /* UI */
        setContent { RideScreen(horseName) }
    }

    /* ---------- UI Composable ---------- */
    @Composable
    private fun RideScreen(horseName: String) {
        val prefs = getSharedPreferences("horses", MODE_PRIVATE)
        val lastActKey   = "lastActivity_$horseName"
        val lastActMillis = prefs.getLong(lastActKey, 0L)
        val lastActText = if (lastActMillis > 0)
            SimpleDateFormat("dd.MM.yy", Locale.getDefault()).format(Date(lastActMillis))
        else "Žádná předchozí aktivita"

        var currentSpeed   by remember { mutableStateOf(0.0) }
        var currentMode    by remember { mutableStateOf("Stojí") }
        var totalDistance  by remember { mutableStateOf(0.0) }
        var countStanding  by remember { mutableStateOf(0f) }
        var countStep      by remember { mutableStateOf(0f) }
        var countTrot      by remember { mutableStateOf(0f) }
        var countCanter    by remember { mutableStateOf(0f) }
        var isRecording    by remember { mutableStateOf(true) }
        val speedBuffer    = remember { mutableStateListOf<Pair<Long, Double>>() }

        Column(Modifier.fillMaxSize().padding(16.dp)) {

            /* mapa */
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(18.0)
                    }.also { map = it }
                },
                modifier = Modifier.fillMaxWidth().height(350.dp)
            )

            Spacer(Modifier.height(4.dp))
            Text("Rychlost: ${"%.1f".format(currentSpeed)} km/h")
            Text("Stav: $currentMode")
            Text("Vzdálenost: ${"%.2f".format(totalDistance)} km")
            Text("Poň: $horseName")
            Spacer(Modifier.height(4.dp))
            Text("Poslední aktivita: $lastActText", style = MaterialTheme.typography.bodyMedium)

            Spacer(Modifier.height(16.dp))
            /* graf */
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
                modifier = Modifier.fillMaxWidth().height(200.dp),
                update = { pie ->
                    val entries = listOf(
                        PieEntry(countStanding, "Stání"),
                        PieEntry(countStep,     "Krok"),
                        PieEntry(countTrot,     "Klus"),
                        PieEntry(countCanter,   "Cval")
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
                    pie.data = PieData(set); pie.invalidate()
                }
            )

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    if (isRecording) {
                        stopTracking(); isRecording = false
                        val gpx = saveGpx()                                      // ← už existuje
                        prefs.edit().putLong(lastActKey, System.currentTimeMillis()).apply()
                        sendGpxByEmail(gpx)
                        finish()
                    }
                },
                enabled = isRecording,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Ukončit a odeslat e‑mailem") }

            /* tracking */
            LaunchedEffect(isRecording) {
                if (isRecording) {
                    startTracking { loc, speed, deltaKm ->
                        trackPoints.add(loc); totalDistance += deltaKm
                        val now = System.currentTimeMillis()
                        speedBuffer.add(now to speed)
                        speedBuffer.removeAll { now - it.first > 5_000 }
                        val avg = speedBuffer.map { it.second }.ifEmpty { listOf(speed) }.average()
                        currentSpeed = avg
                        when {
                            avg < 2           -> { currentMode = "Stojí"; countStanding += 1 }
                            abs(avg - 4) < 1.5 -> { currentMode = "Krok"; countStep     += 1 }
                            abs(avg - 6) < 1.5 -> { currentMode = "Klus"; countTrot     += 1 }
                            else               -> { currentMode = "Cval"; countCanter   += 1 }
                        }
                        drawTrackSegment(loc, avg)
                    }
                }
            }
        }
    }

    /* ---------- Tracking + anti‑jump filtr ---------- */
    private fun startTracking(onUpdate: (Location, Double, Double) -> Unit) {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1_000)
            .setMinUpdateDistanceMeters(MIN_DIST_M)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(res: LocationResult) {
                val loc  = res.lastLocation ?: return
                val prev = lastLocation

                /* anti‑jump */
                if (prev != null) {
                    val dist = prev.distanceTo(loc)
                    val dt   = (loc.time - prev.time) / 1000f
                    val vKmh = if (dt > 0) (dist / dt) * 3.6f else 0f
                    if (dist > MAX_JUMP_M && vKmh > MAX_SPEED_K) {
                        return
                    }
                }

                val deltaKm = prev?.distanceTo(loc)?.div(1_000.0) ?: 0.0
                val speedKmH = prev?.let { p ->
                    val dKm = p.distanceTo(loc) / 1_000.0
                    val h   = (loc.time - p.time) / 3_600_000.0
                    if (h > 0) dKm / h else 0.0
                } ?: 0.0

                onUpdate(loc, speedKmH, deltaKm)
                lastLocation = loc
            }
        }
        locationClient.requestLocationUpdates(request, locationCallback!!, mainLooper)
    }

    private fun stopTracking() {
        locationCallback?.let { locationClient.removeLocationUpdates(it) }
        locationCallback = null
    }

    /* ---------- GPX, email, kreslení ---------- */
    private fun saveGpx(): File {
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val dir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        val file = File(dir, "track_$ts.gpx")
        file.parentFile?.mkdirs()
        FileOutputStream(file).use { fos ->
            fos.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n".toByteArray())
            fos.write("<gpx version=\"1.1\" creator=\"HorseRideTracker\">\n<trk><name>Ride_$ts</name><trkseg>\n".toByteArray())
            trackPoints.forEach { loc ->
                val t = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date(loc.time))
                fos.write("<trkpt lat=\"${loc.latitude}\" lon=\"${loc.longitude}\"><time>$t</time></trkpt>\n".toByteArray())
            }
            fos.write("</trkseg></trk>\n</gpx>\n".toByteArray())
        }
        return file
    }

    private fun sendGpxByEmail(file: File) {
        val uri: Uri = FileProvider.getUriForFile(this, "$packageName.provider", file)
        val email = Intent(Intent.ACTION_SEND).apply {
            type = "application/gpx+xml"
            putExtra(Intent.EXTRA_EMAIL, arrayOf("rolfovo@gmail.com"))
            putExtra(Intent.EXTRA_SUBJECT, "GPX trasa jízdy")
            putExtra(Intent.EXTRA_TEXT, "Ahoj, zde je trasa mé jízdy v GPX souboru.")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(email, "Odeslat GPX e‑mailem"))
    }

    private fun drawTrackSegment(loc: Location, speed: Double) {
        if (!::map.isInitialized) return
        lastLocation?.let { prev ->
            val color = when {
                speed < 2            -> android.graphics.Color.GRAY
                abs(speed - 4) < 1.5 -> android.graphics.Color.GREEN
                abs(speed - 6) < 1.5 -> android.graphics.Color.rgb(255,165,0)
                else                 -> android.graphics.Color.RED
            }
            Polyline().apply {
                outlinePaint.color = color
                outlinePaint.strokeWidth = 6f
                setPoints(
                    listOf(
                        GeoPoint(prev.latitude, prev.longitude),
                        GeoPoint(loc.latitude, loc.longitude)
                    )
                )
            }.also { map.overlays.add(it) }
            map.invalidate()
        }
        map.controller.setCenter(GeoPoint(loc.latitude, loc.longitude))
    }
}
