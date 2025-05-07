package com.example.horseridetracker

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.horseridetracker.ui.theme.HorseRideTrackerTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random
import androidx.compose.foundation.clickable


class CalibrationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val horseName = intent.getStringExtra("horseName") ?: "Neznámý kůň"

        setContent {
            HorseRideTrackerTheme {
                CalibrationScreen(horseName) { step, trot, canter ->
                    Log.d("Kalibrace", "[$horseName] Krok: $step, Klus: $trot, Cval: $canter")

                    val prefs = getSharedPreferences("horses", MODE_PRIVATE)
                    prefs.edit().apply {
                        putFloat("${horseName}_step",   step.toFloat())
                        putFloat("${horseName}_trot",   trot.toFloat())
                        putFloat("${horseName}_canter", canter.toFloat())

                        val set = prefs.getStringSet(PrefKeys.HORSE_NAMES, setOf())?.toMutableSet()
                            ?: mutableSetOf()
                        set.add(horseName)
                        putStringSet(PrefKeys.HORSE_NAMES, set)
                    }.apply()

                    finish()
                }
            }
        }
    }
}

/* -------------------------------------------------------------------------- */

private enum class CalibMode { AUTO, MANUAL }

/* -------------------------------------------------------------------------- */

@Composable
fun CalibrationScreen(
    horseName: String,
    onCalibrationComplete: (Double, Double, Double) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isMeasuring by remember { mutableStateOf(false) }
    var mode by remember { mutableStateOf(CalibMode.AUTO) }

    var stepText by remember { mutableStateOf("") }
    var trotText by remember { mutableStateOf("") }
    var canterText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Kalibrace pro koně: $horseName", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))

        /* ---- Přepínač režimu ---- */
        ModeSwitch(mode) { mode = it }
        Spacer(Modifier.height(16.dp))

        /* ---- Vstupní pole (dostupné v obou režimech) ---- */
        OutlinedTextField(
            value = stepText,
            onValueChange = { stepText = it },
            label = { Text("Rychlost kroku (km/h)") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = trotText,
            onValueChange = { trotText = it },
            label = { Text("Rychlost klusu (km/h)") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = canterText,
            onValueChange = { canterText = it },
            label = { Text("Rychlost cvalu (km/h)") },
            modifier = Modifier.fillMaxWidth()
        )

        /* ---- Tlačítka pro AUTO režim ---- */
        if (mode == CalibMode.AUTO) {
            Spacer(Modifier.height(16.dp))

            CalibrationButton("Změřit krok", isMeasuring) {
                isMeasuring = true
                coroutineScope.launch {
                    stepText = "%.1f".format(simulateMeasurement())
                    isMeasuring = false
                }
            }
            CalibrationButton("Změřit klus", isMeasuring) {
                isMeasuring = true
                coroutineScope.launch {
                    trotText = "%.1f".format(simulateMeasurement())
                    isMeasuring = false
                }
            }
            CalibrationButton("Změřit cval", isMeasuring) {
                isMeasuring = true
                coroutineScope.launch {
                    canterText = "%.1f".format(simulateMeasurement())
                    isMeasuring = false
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                val step   = stepText.toDoubleOrNull() ?: 0.0
                val trot   = trotText.toDoubleOrNull() ?: 0.0
                val canter = canterText.toDoubleOrNull() ?: 0.0
                onCalibrationComplete(step, trot, canter)
            },
            enabled = stepText.isNotBlank() && trotText.isNotBlank() && canterText.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Uložit kalibraci") }
    }
}

/* -------------------------------------------------------------------------- */

@Composable
private fun ModeSwitch(current: CalibMode, onChange: (CalibMode) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        /* Radio – Auto */
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f).clickable { onChange(CalibMode.AUTO) }
        ) {
            RadioButton(
                selected = current == CalibMode.AUTO,
                onClick = { onChange(CalibMode.AUTO) }
            )
            Text("Automaticky")
        }

        /* Radio – Manual */
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f).clickable { onChange(CalibMode.MANUAL) }
        ) {
            RadioButton(
                selected = current == CalibMode.MANUAL,
                onClick = { onChange(CalibMode.MANUAL) }
            )
            Text("Ručně")
        }
    }
}

/* -------------------------------------------------------------------------- */

@Composable
private fun CalibrationButton(text: String, isDisabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !isDisabled,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) { Text(text) }
}

private suspend fun simulateMeasurement(): Double {
    delay(5000)                       // simulace 5 s měření
    return Random.nextDouble(3.0, 15.0)
}
