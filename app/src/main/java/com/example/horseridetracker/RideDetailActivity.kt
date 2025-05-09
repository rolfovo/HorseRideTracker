package com.example.horseridetracker

/* ---------- importy ---------- */
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import com.example.horseridetracker.ui.theme.HorseRideTrackerTheme
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class RideDetailActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // inicializace osmdroid
        Configuration.getInstance().load(
            this,
            androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
        )

        // data z Intentu
        val speed       = intent.getFloatExtra("speed", 0f)
        val status      = intent.getStringExtra("status") ?: ""
        val distance    = intent.getFloatExtra("distance", 0f)
        val pony        = intent.getStringExtra("pony") ?: ""
        val lastAct     = intent.getStringExtra("lastActivity") ?: ""

        setContent {
            HorseRideTrackerTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    RideDetailScreen(
                        speed        = speed,
                        status       = status,
                        distance     = distance,
                        pony         = pony,
                        lastActivity = lastAct,
                        onFinish     = { sendEmailWithSnapshot() }
                    )
                }
            }
        }
    }

    private fun sendEmailWithSnapshot() {
        // vytvoření screenshotu
        val view = window.decorView.rootView
        val bmp = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        view.draw(canvas)

        // uložení do cache
        val file = File(cacheDir, "ride_snapshot.png")
        FileOutputStream(file).use { out -> bmp.compress(Bitmap.CompressFormat.PNG, 90, out) }

        // intent pro email
        val emailIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_SUBJECT, "Detaily jízdy z aplikace")
            putExtra(Intent.EXTRA_TEXT, "Přikládám snapshot z dnešní jízdy.")
            putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file))
        }
        startActivity(Intent.createChooser(emailIntent, "Odeslat e-mailem"))
    }
}

@Composable
fun RideDetailScreen(
    speed: Float,
    status: String,
    distance: Float,
    pony: String,
    lastActivity: String,
    onFinish: () -> Unit
) {
    val context = LocalContext.current

    // Načteme kalibrační rychlosti přímo zde
    val calPrefs   = context.getSharedPreferences("pony_calibration", Context.MODE_PRIVATE)
    val stepSpeed  = calPrefs.getFloat("${pony}_step_speed",   0f)
    val trotSpeed  = calPrefs.getFloat("${pony}_trot_speed",   0f)
    val canterSpeed= calPrefs.getFloat("${pony}_canter_speed", 0f)

    // Načteme celkovou ujetou vzdálenost
    val horsePrefs = context.getSharedPreferences("horses", Context.MODE_PRIVATE)
    val totalAll   = horsePrefs.getFloat("totalDistanceAll", 0f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Rychlost: ${"%.1f".format(speed)} km/h",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(12.dp))

        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    setMultiTouchControls(true)
                    controller.setZoom(18.0)
                    // přidáme marker na start
                    overlays.add(
                        Marker(this).apply {
                            position = GeoPoint(0.0, 0.0)
                            title = "Start"
                        }
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        )
        Spacer(Modifier.height(12.dp))

        Row(
            Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            AndroidView(
                factory = { ctx ->
                    LineChart(ctx).apply {
                        val entries = listOf(
                            Entry(0f, 0f), Entry(1f, speed * 0.5f),
                            Entry(2f, speed * 0.8f), Entry(3f, speed * 0.6f)
                        )
                        data = LineData(
                            LineDataSet(entries, "Rychlost (km/h)").apply {
                                lineWidth = 2f
                                circleRadius = 4f
                                setDrawValues(false)
                                mode = LineDataSet.Mode.CUBIC_BEZIER
                            }
                        )
                        axisRight.isEnabled = false
                        xAxis.position = XAxis.XAxisPosition.BOTTOM
                        animateX(500)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )

            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            ) {
                Text("Stav: $status")
                Text("Vzdálenost: ${"%.2f".format(distance)} km")
                Text("Poník: $pony")
                Text("Poslední aktivita: $lastActivity")
                Spacer(Modifier.height(8.dp))

                // --- TEĎ DÁLE DOLŮ ---
                Text(
                    text = "Krok: ${"%.1f".format(stepSpeed)} km/h, " +
                            "Klus: ${"%.1f".format(trotSpeed)} km/h, " +
                            "Cval: ${"%.1f".format(canterSpeed)} km/h",
                    fontSize = 14.sp
                )
                Text(
                    text = "Ujetá dnes: ${"%.2f".format(distance)} km, " +
                            "Celkem: ${"%.2f".format(totalAll)} km",
                    fontSize = 14.sp
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(onClick = onFinish, modifier = Modifier.fillMaxWidth()) {
            Text("Ukončit a odeslat e-mailem")
        }
    }
}
