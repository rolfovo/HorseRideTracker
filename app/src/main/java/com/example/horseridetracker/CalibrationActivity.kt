package com.example.horseridetracker

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.horseridetracker.ui.theme.HorseRideTrackerTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

class CalibrationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val horseName = intent.getStringExtra("horseName") ?: "Neznámý kůň"

        setContent {
            HorseRideTrackerTheme {
                CalibrationScreen(horseName) { step, trot, canter ->
                    // Uložit do SharedPreferences
                    val sharedPreferences = getSharedPreferences("horses", Context.MODE_PRIVATE)
                    val editor = sharedPreferences.edit()
                    val existing = sharedPreferences.getStringSet("horseNames", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
                    existing.add(horseName)
                    editor.putStringSet("horseNames", existing)
                    editor.putFloat("${horseName}_step", step.toFloat())
                    editor.putFloat("${horseName}_trot", trot.toFloat())
                    editor.putFloat("${horseName}_canter", canter.toFloat())
                    editor.apply()

                    Log.d("Kalibrace", "Krok: $step, Klus: $trot, Cval: $canter")
                    finish()
                }
            }
        }
    }
}

@Composable
fun CalibrationScreen(
    horseName: String,
    onCalibrationComplete: (Double, Double, Double) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var step by remember { mutableStateOf(0.0) }
    var trot by remember { mutableStateOf(0.0) }
    var canter by remember { mutableStateOf(0.0) }
    var isMeasuring by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Kalibrace pro koně: $horseName", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        CalibrationButton("Začni kalibraci kroku", isMeasuring) {
            isMeasuring = true
            coroutineScope.launch {
                step = simulateMeasurement()
                isMeasuring = false
            }
        }

        CalibrationButton("Začni kalibraci klusu", isMeasuring) {
            isMeasuring = true
            coroutineScope.launch {
                trot = simulateMeasurement()
                isMeasuring = false
            }
        }

        CalibrationButton("Začni kalibraci cvalu", isMeasuring) {
            isMeasuring = true
            coroutineScope.launch {
                canter = simulateMeasurement()
                isMeasuring = false
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Krok: ${"%.1f".format(step)} km/h")
        Text("Klus: ${"%.1f".format(trot)} km/h")
        Text("Cval: ${"%.1f".format(canter)} km/h")

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                onCalibrationComplete(step, trot, canter)
            },
            enabled = step > 0 && trot > 0 && canter > 0
        ) {
            Text("Uložit kalibraci")
        }
    }
}

@Composable
fun CalibrationButton(text: String, isDisabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !isDisabled,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text)
    }
}

suspend fun simulateMeasurement(): Double {
    val values = mutableListOf<Double>()
    repeat(30) {
        delay(1000)
        values.add(Random.nextDouble(3.0, 15.0)) // náhodná rychlost každou sekundu
    }
    return values.average()
}
