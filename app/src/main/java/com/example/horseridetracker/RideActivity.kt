package com.example.horseridetracker

/* ---------- importy ---------- */
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.horseridetracker.ui.theme.HorseRideTrackerTheme
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

class RideActivity : ComponentActivity() {
    private val MIN_DIST_M = 1f
    private val MAX_JUMP_M = 10f
    private val MAX_SPEED_K = 40

    private lateinit var map: MapView
    private lateinit var locationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    private var lastLocation: Location? = null
    private val trackPoints = mutableListOf<Location>()

    private var thresholdStep = 0.0
    private var thresholdTrot = 0.0
    private var thresholdCanter = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pony = intent.getStringExtra("pony") ?: "Neznámý kůň"

        // načtení kalibrací
        val defaults = resources.getStringArray(R.array.pony_default_speeds)
        thresholdStep   = defaults.getOrNull(0)?.toDoubleOrNull() ?: 4.0
        thresholdTrot   = defaults.getOrNull(1)?.toDoubleOrNull() ?: 6.0
        thresholdCanter = defaults.getOrNull(2)?.toDoubleOrNull() ?: 8.0
        loadThresholds(pony)

        Configuration.getInstance().load(
            applicationContext,
            getSharedPreferences("osm", MODE_PRIVATE)
        )

        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
        }

        locationClient = LocationServices.getFusedLocationProviderClient(this)

        setContent {
            HorseRideTrackerTheme {
                RideScreen(pony)
            }
        }
    }

    /** načte uživatelské kalibrace z SharedPreferences */
    private fun loadThresholds(name: String) {
        val prefs = getSharedPreferences("pony_calibration", Context.MODE_PRIVATE)
        prefs.getFloat("${name}_step_speed", thresholdStep.toFloat())
            .takeIf { it>0f }?.let { thresholdStep = it.toDouble() }
        prefs.getFloat("${name}_trot_speed", thresholdTrot.toFloat())
            .takeIf { it>0f }?.let { thresholdTrot = it.toDouble() }
        prefs.getFloat("${name}_canter_speed", thresholdCanter.toFloat())
            .takeIf { it>0f }?.let { thresholdCanter = it.toDouble() }
    }

    @Composable
    private fun RideScreen(pony: String) {
        val context = LocalContext.current
        val prefs = getSharedPreferences("horses", MODE_PRIVATE)
        val lastActKey = "lastActivity_$pony"
        val lastActMillis = prefs.getLong(lastActKey, 0L)
        val lastActText = if (lastActMillis>0)
            SimpleDateFormat("dd.MM.yy", Locale.getDefault()).format(Date(lastActMillis))
        else "Žádná předchozí aktivita"

        var currentSpeed by remember { mutableStateOf(0.0) }
        var currentMode  by remember { mutableStateOf("Stojí") }
        var totalDistance by remember { mutableStateOf(0.0) }
        var totalAll     by remember { mutableStateOf(prefs.getFloat("totalDistanceAll",0f).toDouble()) }
        var countStanding by remember { mutableStateOf(0f) }
        var countStep    by remember { mutableStateOf(0f) }
        var countTrot    by remember { mutableStateOf(0f) }
        var countCanter  by remember { mutableStateOf(0f) }
        var isRecording  by remember { mutableStateOf(true) }
        val speedBuffer  = remember { mutableStateListOf<Pair<Long, Double>>() }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(0.dp)
        ) {
            // **VELKÁ MAPA**
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
                    .height(400.dp)
            )

            Spacer(Modifier.height(16.dp))

            // **RYCHLOST, STAV, VZDÁLENOST, PONÍK, POSLEDNÍ AKTIVITA**
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Rychlost: ${"%.1f".format(currentSpeed)} km/h",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
                Text("Stav: $currentMode", fontSize = 20.sp)
                Text("Vzdálenost: ${"%.2f".format(totalDistance)} km", fontSize = 20.sp)
                Text("Poník: $pony", fontSize = 20.sp)
                Text("Poslední aktivita: $lastActText", fontSize = 16.sp)
            }

            Spacer(Modifier.height(12.dp))

            // **THRESHOLDY**
            Text(
                "Krok: ${"%.1f".format(thresholdStep)} km/h, " +
                        "Klus: ${"%.1f".format(thresholdTrot)} km/h, " +
                        "Cval: ${"%.1f".format(thresholdCanter)} km/h",
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(12.dp))

            // **BAREVNÝ PRUH** s rozložením časů
            val totalCount = countStanding + countStep + countTrot + countCanter
            if (totalCount > 0f) {
                fun safe(w: Float) = if (w>0f) w else 0.0001f
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                ) {
                    Box(Modifier.weight(safe(countStanding)).fillMaxHeight().background(Color.Gray))
                    Box(Modifier.weight(safe(countStep))   .fillMaxHeight().background(Color.Green))
                    Box(Modifier.weight(safe(countTrot))   .fillMaxHeight().background(Color(0xFFFFA500)))
                    Box(Modifier.weight(safe(countCanter)) .fillMaxHeight().background(Color.Red))
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    LegendItem("Stání",  Color.Gray)
                    LegendItem("Krok",   Color.Green)
                    LegendItem("Klus",   Color(0xFFFFA500))
                    LegendItem("Cval",   Color.Red)
                }
            }

            Spacer(Modifier.height(16.dp))

            // **TLAČÍTKO UKONČIT**
            Button(
                onClick = {
                    if (isRecording) {
                        stopTracking()
                        isRecording = false
                        // aktualizovat celkem
                        totalAll += totalDistance
                        prefs.edit().putFloat("totalDistanceAll", totalAll.toFloat()).apply()
                        // uložit GPX + čas
                        saveGpx().also {
                            prefs.edit().putLong(lastActKey, System.currentTimeMillis()).apply()
                        }

                        // připravit detail
                        val durationSec = trackPoints.lastOrNull()?.let {
                            (it.time - trackPoints.first().time)/1000L
                        } ?: 0L
                        val stepSec   = countStep.toLong()
                        val trotSec   = countTrot.toLong()
                        val canterSec = countCanter.toLong()

                        Intent(context, RideDetailActivity::class.java).apply {
                            putExtra("pony", pony)
                            putExtra("distance", totalDistance.toFloat())
                            putExtra("durationSeconds", durationSec)
                            putExtra("stepSeconds", stepSec)
                            putExtra("trotSeconds", trotSec)
                            putExtra("canterSeconds", canterSec)
                        }.also { context.startActivity(it) }
                    }
                },
                enabled = isRecording,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Ukončit a zobrazit detail")
            }
        }

        // **TRACKING LOGIKA**
        LaunchedEffect(isRecording) {
            if (isRecording) {
                this@RideActivity.startTracking { loc: Location, speed: Double, deltaKm: Double ->
                    trackPoints.add(loc)
                    totalDistance += deltaKm
                    // buffer pro průměr
                    val now = System.currentTimeMillis()
                    speedBuffer.add(now to speed)
                    speedBuffer.removeAll { now - it.first > 5000 }
                    val avg = speedBuffer.map { it.second }.ifEmpty { listOf(speed) }.average()
                    currentSpeed = avg
                    when {
                        avg < thresholdStep   -> { currentMode = "Stojí"; countStanding +=1f }
                        avg < thresholdTrot   -> { currentMode = "Krok";  countStep +=1f }
                        avg < thresholdCanter -> { currentMode = "Klus";  countTrot +=1f }
                        else                  -> { currentMode = "Cval";  countCanter +=1f }
                    }
                    drawTrackSegment(loc, avg)
                }
            }
        }
    }

    @Composable
    private fun LegendItem(label: String, color: Color) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(12.dp).background(color))
            Spacer(Modifier.width(4.dp))
            Text(label, fontSize = 14.sp)
        }
    }

    private fun startTracking(onUpdate: (Location, Double, Double) -> Unit) {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1_000)
            .setMinUpdateDistanceMeters(MIN_DIST_M)
            .build()
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(res: LocationResult) {
                val loc = res.lastLocation ?: return
                val prev = lastLocation
                if (prev != null) {
                    val dist = prev.distanceTo(loc)
                    val dt = (loc.time - prev.time)/1000f
                    val vKmh = if (dt>0) (dist/dt)*3.6f else 0f
                    if (dist>MAX_JUMP_M && vKmh>MAX_SPEED_K) return
                }
                val deltaKm = prev?.distanceTo(loc)?.div(1000.0)?:0.0
                onUpdate(loc, deltaKm, deltaKm)
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
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val dir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        val file = File(dir, "track_$ts.gpx").also { it.parentFile?.mkdirs() }
        FileOutputStream(file).use { fos ->
            fos.write("""<?xml version="1.0" encoding="UTF-8"?>
""".toByteArray())
            fos.write("""<gpx version="1.1" creator="HorseRideTracker">
<trk><name>Ride_$ts</name><trkseg>
""".toByteArray())
            trackPoints.forEach { loc ->
                val t = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date(loc.time))
                fos.write("""<trkpt lat="${loc.latitude}" lon="${loc.longitude}"><time>$t</time></trkpt>
""".toByteArray())
            }
            fos.write("""</trkseg></trk>
</gpx>
""".toByteArray())
        }
        return file
    }

    private fun drawTrackSegment(loc: Location, speed: Double) {
        if (!::map.isInitialized) return
        lastLocation?.let { prev ->
            val color = when {
                speed < thresholdStep   -> android.graphics.Color.GRAY
                speed < thresholdTrot   -> android.graphics.Color.GREEN
                speed < thresholdCanter -> android.graphics.Color.rgb(255,165,0)
                else                    -> android.graphics.Color.RED
            }
            Polyline().apply {
                outlinePaint.color = color
                outlinePaint.strokeWidth = 12f
                setPoints(listOf(GeoPoint(prev.latitude, prev.longitude), GeoPoint(loc.latitude, loc.longitude)))
            }.also { map.overlays.add(it) }
            map.invalidate()
        }
        map.controller.setCenter(GeoPoint(loc.latitude, loc.longitude))
    }
}
