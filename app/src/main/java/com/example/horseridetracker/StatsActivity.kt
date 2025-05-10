package com.example.horseridetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.horseridetracker.ui.theme.HorseRideTrackerTheme
import com.example.horseridetracker.RideStatsViewModel

@Composable
fun PonyStatsScreen(viewModel: RideStatsViewModel) {
    val history by viewModel.history.collectAsState()
    val dateFmt = java.text.SimpleDateFormat("d. M. yyyy", java.util.Locale.getDefault())

    // Připravíme si text pro poslední jízdu
    val lastRideText = viewModel.lastRide.value?.date
        ?.let { dateFmt.format(it) }
        ?: "–"

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Poslední jízda: $lastRideText")

        Spacer(Modifier.height(4.dp))

        Text("Průměr rychlosti – krok: ${String.format("%.1f", viewModel.avgStepSpeed.value)} km/h")
                Text("Průměr rychlosti – klus: ${String.format("%.1f", viewModel.avgTrotSpeed.value)} km/h")
                Text("Průměr rychlosti – cval: ${String.format("%.1f", viewModel.avgCanterSpeed.value)} km/h")
                Text("Celkem ujeto: ${String.format("%.2f", viewModel.totalDistance.value)} km")

                Spacer(Modifier.height(16.dp))
                
                Text("Historie jízd:", style = MaterialTheme.typography.titleMedium)
                                        Spacer(Modifier.height(8.dp))

                                                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                                            items(history) { ride ->
                                                Text(
                                                    text = "${dateFmt.format(ride.date)} – " +
                                                            "${ride.durationSeconds / 60} min, " +
                                                            "${String.format("%.2f", ride.distanceKm)} km, " +
                                                                    "[${ride.stepSeconds}s/${ride.trotSeconds}s/${ride.canterSeconds}s]",
                                                                modifier = Modifier.padding(vertical = 4.dp)
                                                                )
                                                            }
                                            }
                                        }
                                    }


class StatsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ponyName = intent.getStringExtra("pony") ?: ""
        val viewModel = RideStatsViewModel(ponyName)

        setContent {
            HorseRideTrackerTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    PonyStatsScreen(viewModel)
                }
            }
        }
    }
}
