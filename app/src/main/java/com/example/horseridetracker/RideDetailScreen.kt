package com.example.horseridetracker

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun RideDetailScreen(
    speed: Float,
    status: String,
    distance: Float,
    pony: String,
    lastActivity: String,
    onFinish: () -> Unit,
    onShowStats: () -> Unit   // přidán callback pro statistiky
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Rychlost: ${"%.1f".format(speed)} km/h")
        Text("Stav: $status")
        Text("Vzdálenost: ${"%.2f".format(distance)} km")
        Text("Poník: $pony")
        Text("Poslední aktivita: $lastActivity")

        Spacer(Modifier.height(16.dp))

        Button(onClick = onFinish, modifier = Modifier.fillMaxWidth()) {
            Text("Ukončit a odeslat e-mailem")
        }

        Spacer(Modifier.height(8.dp))

        Button(onClick = onShowStats, modifier = Modifier.fillMaxWidth()) {
            Text("Zobrazit statistiky")
        }
    }
}
