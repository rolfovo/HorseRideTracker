package com.example.horseridetracker
import android.util.Log
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.horseridetracker.ui.theme.HorseRideTrackerTheme
import kotlinx.coroutines.delay
import kotlin.random.Random
import kotlinx.coroutines.delay
class CalibrationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            HorseRideTrackerTheme {
                CalibrationScreen(horseName = "Orin") { step, trot, canter ->
                    // Tady si uložíš výsledky třeba do databáze nebo souboru
                    Log.d("Kalibrace", "Krok: $step, Klus: $trot, Cval: $canter")
                    finish() // ukončí aktivitu
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
    var currentPhase by remember { mutableStateOf("Zastavení") }
    var step by remember { mutableStateOf(0.0) }
    var trot by remember { mutableStateOf(0.0) }
    var canter by remember { mutableStateOf(0.0) }
    var isMeasuring by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Kalibrace pro koně: $horseName")
        Spacer(modifier = Modifier.height(16.dp))

        CalibrationButton("Začni kalibraci kroku", isMeasuring) {
            currentPhase = "krok"
            isMeasuring = true
            coroutineScope.launch {
                step = simulateMeasurement()
                isMeasuring = false
            }
        }

        CalibrationButton("Začni kalibraci klusu", isMeasuring) {
            currentPhase = "klus"
            isMeasuring = true
            coroutineScope.launch {
                trot = simulateMeasurement()
                isMeasuring = false
            }
        }

        CalibrationButton("Začni kalibraci cvalu", isMeasuring) {
            currentPhase = "cval"
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

fun Double.format(digits: Int = 2): String = "%.${digits}f".format(this)

suspend fun simulateMeasurement(): Double {
    delay(15_000) // 15 sekund čekání jako simulace měření
    return Random.nextDouble(3.0, 15.0) // náhodná rychlost v km/h
}
