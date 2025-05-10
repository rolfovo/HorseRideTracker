package com.example.horseridetracker

/* ---------- importy ---------- */
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.horseridetracker.ui.theme.HorseRideTrackerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import java.text.SimpleDateFormat
import java.util.*

class RideDetailActivity : ComponentActivity() {
    private val rideDao by lazy { HorseTrackerApp.db.rideDao() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pony          = intent.getStringExtra("pony") ?: ""
        val distance      = intent.getFloatExtra("distance", 0f)
        val durationSec   = intent.getLongExtra("durationSeconds", 0L)
        val stepSec       = intent.getLongExtra("stepSeconds", 0L)
        val trotSec       = intent.getLongExtra("trotSeconds", 0L)
        val canterSec     = intent.getLongExtra("canterSeconds", 0L)

        // Uložíme do Room na IO vlákně
        lifecycleScope.launch(Dispatchers.IO) {
            val ride = Ride(
                date            = Date(),
                ponyName        = pony,
                distanceKm      = distance.toFloat(),
                durationSeconds = durationSec,   // pokud entita má toto pole
                stepSeconds     = stepSec,
                trotSeconds     = trotSec,
                canterSeconds   = canterSec
            )
            rideDao.insert(ride)
        }

        setContent {
            HorseRideTrackerTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    StatsScreen(pony)
                }
            }
        }
    }
}

@Composable
private fun StatsScreen(ponyName: String) {
    val vm = remember { RideStatsViewModel(ponyName) }
    val history by vm.history.collectAsState()
    val last by vm.lastRide
    val dateFmt = remember { SimpleDateFormat("d. M. yyyy", Locale.getDefault()) }

    Column(Modifier.padding(16.dp)) {
        Text("Poslední jízda: ${
            last?.date?.let { dateFmt.format(it) } ?: "–"
        }")

        Spacer(Modifier.height(8.dp))
        Text("Průměr rychlost – krok: ${String.format("%.1f", vm.avgStepSpeed.value)} km/h")
        Text("Průměr rychlost – klus: ${String.format("%.1f", vm.avgTrotSpeed.value)} km/h")
        Text("Průměr rychlost – cval: ${String.format("%.1f", vm.avgCanterSpeed.value)} km/h")
        Text("Celkem ujeto: ${String.format("%.2f", vm.totalDistance.value)} km")

            Spacer(Modifier.height(16.dp))
                    Text("Historie jízd:", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

                    LazyColumn(Modifier.fillMaxWidth()) {
                items(history) { ride ->
                    Text(
                        "${dateFmt.format(ride.date)} – " +
                                "${ride.durationSeconds/60} min, " +
                                "${String.format("%.2f", ride.distanceKm)} km"
                                    )
                                }
                }
            }
        }
