package com.example.horseridetracker

/* ---------- importy ---------- */
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
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

    /* ✨ ANTI-JUMP PARAMETRY */
    private val MIN_DIST_M  = 1f
    private val MAX_JUMP_M  = 10f
    private val MAX_SPEED_K = 40

    private lateinit var map: MapView
    private lateinit var locationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    private var lastLocation: Location? = null
    private val trackPoints = mutableListOf<Location>()

    // dynamické prahy
    private var thresholdStep   = 0.0
    private var thresholdTrot   = 0.0
    private var thresholdCanter = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val horseName = intent.getStringExtra("horseName") ?: "Neznámý kůň"

        // načteme fallback a kalibraci
        val defaults = resources.getStringArray(R.array.pony_default_speeds)
        thresholdStep   = defaults.getOrNull(0)?.toDoubleOrNull() ?: 4.0
        thresholdTrot   = defaults.getOrNull(1)?.toDoubleOrNull() ?: 6.0
        thresholdCanter = defaults.getOrNull(2)?.toDoubleOrNull() ?: 8.0
        loadThresholds(horseName)

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
            RideScreen(horseName)
        }
    }

    /** Načte kalibraci */
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
    private fun RideScreen(horseName: String) {
        val prefs = getSharedPreferences("horses", MODE_PRIVATE)
        val lastActKey = "lastActivity_$horseName"
        val lastActMillis = prefs.getLong(lastActKey, 0L)
        val lastActText = if (lastActMillis>0)
            SimpleDateFormat("dd.MM.yy", Locale.getDefault()).format(Date(lastActMillis))
        else "Žádná předchozí aktivita"

        var currentSpeed  by remember { mutableStateOf(0.0) }
        var currentMode   by remember { mutableStateOf("Stojí") }
        var totalDistance by remember { mutableStateOf(0.0) }
        var totalAll      by remember { mutableStateOf(
            prefs.getFloat("totalDistanceAll", 0f).toDouble()
        ) }
        var countStanding by remember { mutableStateOf(0f) }
        var countStep     by remember { mutableStateOf(0f) }
        var countTrot     by remember { mutableStateOf(0f) }
        var countCanter   by remember { mutableStateOf(0f) }
        var isRecording   by remember { mutableStateOf(true) }
        val speedBuffer   = remember { mutableStateListOf<Pair<Long, Double>>() }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
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
                    .height(400.dp) // zvětšená výška mapy
            )

            Spacer(Modifier.height(70.dp)) // odsazení textu

            Text(
                "Rychlost: ${"%.1f".format(currentSpeed)} km/h",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Stav: $currentMode",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Vzdálenost: ${"%.2f".format(totalDistance)} km",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text("Poník: $horseName", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Poslední aktivita: $lastActText",
                style = MaterialTheme.typography.bodyMedium)

            Spacer(Modifier.height(12.dp))
            // kalibrace a vzdálenosti
            Text("Krok: ${"%.1f".format(thresholdStep)} km/h, " +
                    "Klus: ${"%.1f".format(thresholdTrot)} km/h, " +
                    "Cval: ${"%.1f".format(thresholdCanter)} km/h",
                fontSize = 14.sp)
            Spacer(Modifier.height(10.dp)) // odsazení textu
            Text("Celkem ujeto: ${"%.2f".format(totalAll)} ponykilometrů",
                fontSize = 14.sp)
            Spacer(Modifier.height(20.dp)) // odsazení textu
            // tady vlož barevný pruh
            val totalCount = countStanding + countStep + countTrot + countCanter
            if (totalCount > 0f) {
                fun safeWeight(w: Float) = if (w > 0f) w else 0.0001f
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                ) {
                    Box(Modifier.weight(safeWeight(countStanding)).fillMaxHeight().background(Color.Gray))
                    Box(Modifier.weight(safeWeight(countStep))    .fillMaxHeight().background(Color.Green))
                    Box(Modifier.weight(safeWeight(countTrot))    .fillMaxHeight().background(Color(0xFFFFA500)))
                    Box(Modifier.weight(safeWeight(countCanter))  .fillMaxHeight().background(Color.Red))
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment   = Alignment.CenterVertically
                ) {
                    LegendItem("Stání",  Color.Gray)
                    LegendItem("Krok",   Color.Green)
                    LegendItem("Klus",   Color(0xFFFFA500))
                    LegendItem("Cval",   Color.Red)
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    if (isRecording) {
                        // ukončit
                        stopTracking()
                        isRecording = false
                        // aktualizovat celkem
                        totalAll += totalDistance
                        prefs.edit().putFloat("totalDistanceAll", totalAll.toFloat()).apply()
                        // uložit GPX + send by email
                        val gpx = saveGpx()
                        prefs.edit().putLong(lastActKey, System.currentTimeMillis()).apply()
                        sendGpxByEmail(gpx)
                        finish()
                    }
                },
                enabled = isRecording,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Ukončit a odeslat e-mailem")
            }
        }

        LaunchedEffect(isRecording) {
            if (isRecording) startTracking { loc, speed, deltaKm ->
                trackPoints.add(loc)
                totalDistance += deltaKm
                // buffer
                val now = System.currentTimeMillis()
                speedBuffer.add(now to speed)
                speedBuffer.removeAll { now - it.first > 5_000 }
                val avg = speedBuffer.map { it.second }.ifEmpty { listOf(speed) }.average()
                currentSpeed = avg
                when {
                    avg < thresholdStep   -> { currentMode = "Stojí"; countStanding +=1 }
                    avg < thresholdTrot   -> { currentMode = "Krok"; countStep   +=1 }
                    avg < thresholdCanter -> { currentMode = "Klus"; countTrot   +=1 }
                    else                  -> { currentMode = "Cval"; countCanter +=1 }
                }
                drawTrackSegment(loc, avg)
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
                val deltaKm = prev?.distanceTo(loc)?.div(1_000.0)?:0.0
                val speedKmH= prev?.let{p->val dKm=p.distanceTo(loc)/1_000.0; val h=(loc.time-p.time)/3_600_000.0; if(h>0)dKm/h else 0.0 }?:0.0
                onUpdate(loc,speedKmH,deltaKm)
                lastLocation=loc
            }
        }
        locationClient.requestLocationUpdates(request, locationCallback!!, mainLooper)
    }

    private fun stopTracking() {
        locationCallback?.let {locationClient.removeLocationUpdates(it)}
        locationCallback=null
    }

    private fun saveGpx(): File {
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val dir= getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        val file=File(dir, "track_$ts.gpx").also{it.parentFile?.mkdirs()}
        FileOutputStream(file).use{fos->
            fos.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n".toByteArray())
            fos.write("<gpx version=\"1.1\" creator=\"HorseRideTracker\">\n<trk><name>Ride_$ts</name><trkseg>\n".toByteArray())
            trackPoints.forEach{loc->
                val t=SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'",Locale.getDefault()).format(Date(loc.time))
                fos.write("<trkpt lat=\"${loc.latitude}\" lon=\"${loc.longitude}\"><time>$t</time></trkpt>\n".toByteArray())
            }
            fos.write("</trkseg></trk>\n</gpx>\n".toByteArray())
        }
        return file
    }

    private fun sendGpxByEmail(file: File) {
        val uri=FileProvider.getUriForFile(this,"$packageName.provider",file)
        val i=Intent(Intent.ACTION_SEND).apply{
            type="application/gpx+xml"
            putExtra(Intent.EXTRA_STREAM,uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(i,"Odeslat GPX e-mailem"))
    }

    private fun drawTrackSegment(loc: Location, speed: Double) {
        if(!::map.isInitialized)return
        lastLocation?.let{prev->
            val color=when{
                speed<thresholdStep   -> android.graphics.Color.GRAY
                speed<thresholdTrot   -> android.graphics.Color.GREEN
                speed<thresholdCanter -> android.graphics.Color.rgb(255,165,0)
                else                  -> android.graphics.Color.RED
            }
            Polyline().apply{
                outlinePaint.color=color
                outlinePaint.strokeWidth=12f
                setPoints(listOf(GeoPoint(prev.latitude,prev.longitude),GeoPoint(loc.latitude,loc.longitude)))
            }.also{map.overlays.add(it)}
            map.invalidate()
        }
        map.controller.setCenter(GeoPoint(loc.latitude,loc.longitude))
    }
}
