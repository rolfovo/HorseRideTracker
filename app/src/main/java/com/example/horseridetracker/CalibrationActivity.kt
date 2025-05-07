package com.example.horseridetracker   // ← stejný balíček jako ostatní activity

/* ---------- importy ---------- */
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.horseridetracker.ui.theme.HorseRideTrackerTheme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/* ---------- Activity ---------- */
class CalibrationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val horseName = intent.getStringExtra("horseName") ?: "Neznámý kůň"

        setContent {
            HorseRideTrackerTheme {
                CalibrationScreen(horseName) { step, trot, canter ->
                    // TODO: ulož do SharedPrefs / DB
                    setResult(RESULT_OK)
                    finish()
                }
            }
        }
    }
}

/* ---------- UI + kalibrační logika ---------- */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalibrationScreen(
    horseName: String,
    onCalibrationComplete: (Double, Double, Double) -> Unit
) {
    val context = LocalContext.current
    val fused = remember { LocationServices.getFusedLocationProviderClient(context) }
    val scope = rememberCoroutineScope()

    var mode by remember { mutableStateOf(CalibMode.AUTO) }
    var step by remember { mutableStateOf(0.0) }
    var trot by remember { mutableStateOf(0.0) }
    var canter by remember { mutableStateOf(0.0) }
    var measuring by remember { mutableStateOf(false) }
    var measureLabel by remember { mutableStateOf("") }

    /* ----- runtime permission launcher ----- */
    val permLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) mode = CalibMode.MANUAL // fallback ruční zadání
        }

    /* ----- UI layout ----- */
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Kalibrace pro poníka: $horseName", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))

        /* --- přepínač AUTO / MANUAL --- */
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = mode == CalibMode.AUTO, onClick = { mode = CalibMode.AUTO })
            Text("Automaticky")
            Spacer(Modifier.width(24.dp))
            RadioButton(selected = mode == CalibMode.MANUAL, onClick = { mode = CalibMode.MANUAL })
            Text("Ručně")
        }
        Spacer(Modifier.height(16.dp))

        /* --- políčka rychlostí --- */
        SpeedField("Rychlost kroku (km/h)", step,   mode) { step   = it }
        SpeedField("Rychlost klusu (km/h)", trot,   mode) { trot   = it }
        SpeedField("Rychlost cvalu (km/h)", canter, mode) { canter = it }

        Spacer(Modifier.height(24.dp))

        /* --- tlačítka měření (pouze AUTO) --- */
        if (mode == CalibMode.AUTO) {
            MeasureButton("Změřit krok", measuring) {
                measuring = true; measureLabel = "krok"
                startMeasurement(fused, scope) { v -> step = v; measuring = false }
            }
            MeasureButton("Změřit klus", measuring) {
                measuring = true; measureLabel = "klus"
                startMeasurement(fused, scope) { v -> trot = v; measuring = false }
            }
            MeasureButton("Změřit cval", measuring) {
                measuring = true; measureLabel = "cval"
                startMeasurement(fused, scope) { v -> canter = v; measuring = false }
            }
            Spacer(Modifier.height(16.dp))
            if (measuring) Text("Měřím $measureLabel…", style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(Modifier.height(24.dp))

        /* --- uložit --- */
        Button(
            onClick = { onCalibrationComplete(step, trot, canter) },
            enabled = step > 0 && trot > 0 && canter > 0 && !measuring,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Uložit kalibraci") }
    }

    /* ----- požádej o location permission, pokud chybí ----- */
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
}

/* ---------- composable helpers ---------- */
@Composable
private fun SpeedField(
    label: String,
    value: Double,
    mode: CalibMode,
    onChange: (Double) -> Unit
) {
    var text by remember(value) { mutableStateOf(if (value > 0) value.toString() else "") }

    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it.replace(',', '.')
            onChange(text.toDoubleOrNull() ?: 0.0)
        },
        label = { Text(label) },
        singleLine = true,
        enabled = mode == CalibMode.MANUAL,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    )
}

@Composable
private fun MeasureButton(text: String, disabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !disabled,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) { Text(text) }
}

/* ---------- samotné měření rychlosti ---------- */
@SuppressLint("MissingPermission") // runtime permission řešíme výše
private fun startMeasurement(
    fused: FusedLocationProviderClient,
    scope: CoroutineScope,
    onDone: (Double) -> Unit
) {
    val samples = mutableListOf<Float>()

    val callback = object : LocationCallback() {
        override fun onLocationResult(res: LocationResult) {
            res.lastLocation?.speed?.takeIf { it > 0f }?.let { samples += it }
        }
    }

    val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1_000)
        .setMinUpdateDistanceMeters(5f)
        .build()

    fused.requestLocationUpdates(request, callback, null)

    scope.launch {
        delay(10_000)                           // 10 s měření
        fused.removeLocationUpdates(callback)

        val kmh = if (samples.isEmpty()) 0.0
        else (samples.average() * 3.6 * 10).roundToInt() / 10.0  // 1 desetinné místo

        onDone(kmh)     // vrátí průměrnou rychlost chodu
    }
}

/* ---------- pomocný enum ---------- */
private enum class CalibMode { AUTO, MANUAL }
