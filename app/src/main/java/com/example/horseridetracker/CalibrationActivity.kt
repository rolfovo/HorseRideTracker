package com.example.horseridetracker

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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

class CalibrationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val horseName = intent.getStringExtra("horseName") ?: "Neznámý kůň"

        setContent {
            HorseRideTrackerTheme {
                CalibrationScreen(horseName) { step, trot, canter ->
                    // 1) Ulož kalibraci do SharedPreferences
                    val prefs = getSharedPreferences("calibration", Context.MODE_PRIVATE)
                    with(prefs.edit()) {
                        putFloat("step_${'$'}horseName", step.toFloat())
                        putFloat("trot_${'$'}horseName", trot.toFloat())
                        putFloat("canter_${'$'}horseName", canter.toFloat())
                        apply()
                    }
                    // 2) Vrátíme výsledek a ukončíme
                    setResult(RESULT_OK)
                    finish()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalibrationScreen(
    horseName: String,
    onCalibrationComplete: (Double, Double, Double) -> Unit
) {
    val context = LocalContext.current
    val fielPrefs = context.getSharedPreferences("calibration", Context.MODE_PRIVATE)
    val savedStep = fielPrefs.getFloat("step_${'$'}horseName", 0f).toDouble()
    val savedTrot = fielPrefs.getFloat("trot_${'$'}horseName", 0f).toDouble()
    val savedCanter = fielPrefs.getFloat("canter_${'$'}horseName", 0f).toDouble()
    val fused = remember { LocationServices.getFusedLocationProviderClient(context) }
    val scope = rememberCoroutineScope()

    var mode by remember { mutableStateOf(CalibMode.AUTO) }
    var step by remember { mutableStateOf(savedStep) }
    var trot by remember { mutableStateOf(savedTrot) }
    var canter by remember { mutableStateOf(savedCanter) }
    var measuring by remember { mutableStateOf(false) }
    var measureLabel by remember { mutableStateOf("") }

    val permLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) mode = CalibMode.MANUAL
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Kalibrace pro koně: ${'$'}horseName", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = mode == CalibMode.AUTO, onClick = { mode = CalibMode.AUTO })
            Text("Automaticky")
            Spacer(Modifier.width(24.dp))
            RadioButton(selected = mode == CalibMode.MANUAL, onClick = { mode = CalibMode.MANUAL })
            Text("Ručně")
        }
        Spacer(Modifier.height(16.dp))

        SpeedField("Rychlost kroku (km/h)", step, mode) { step = it }
        SpeedField("Rychlost klusu (km/h)", trot, mode) { trot = it }
        SpeedField("Rychlost cvalu (km/h)", canter, mode) { canter = it }

        Spacer(Modifier.height(24.dp))

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
            if (measuring) Text("Měřím ${'$'}measureLabel…", style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { onCalibrationComplete(step, trot, canter) },
            enabled = step > 0 && trot > 0 && canter > 0 && !measuring,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Uložit kalibraci")
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
}

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

@SuppressLint("MissingPermission")
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
        delay(10_000)
        fused.removeLocationUpdates(callback)
        val kmh = if (samples.isEmpty()) 0.0
        else (samples.average() * 3.6 * 10).roundToInt() / 10.0
        onDone(kmh)
    }
}

private enum class CalibMode { AUTO, MANUAL }
