package com.example.horseridetracker

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.horseridetracker.ui.theme.HorseRideTrackerTheme

private const val REQ_LOCATION = 100

/**
 * Domovská obrazovka:
 * • Výběr koně → tlačítko „Spustit jízdu“
 * • Ikona seznamu v TopAppBar → ManageHorsesActivity
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1) Zeptáme se na povolení polohy (i na pozadí)
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ),
            REQ_LOCATION
        )

        // 2) Teprve pak naplníme Compose UI
        setContent {
            HorseRideTrackerTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen() {
    val context = LocalContext.current
    var selectedHorseName by remember { mutableStateOf("") }

    // Launcher, který čeká na výsledek z HorseSelectActivity
    val selectHorseLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data
                ?.getStringExtra("selectedHorse")
                ?.takeIf { it.isNotBlank() }
                ?.let { selectedHorseName = it }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Horse Ride Tracker") },
                actions = {
                    IconButton(onClick = {
                        context.startActivity(
                            Intent(context, ManageHorsesActivity::class.java)
                        )
                    }) {
                        Icon(Icons.Default.List, contentDescription = "Moji koně")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            /** Info o vybraném koni **/
            Text(
                text = if (selectedHorseName.isEmpty())
                    "Žádný poník není vybrán"
                else
                    "Vybraný kůň: $selectedHorseName",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(16.dp))

            /** Tlačítko pro výběr koně **/
            Button(
                onClick = {
                    selectHorseLauncher.launch(
                        Intent(context, HorseSelectActivity::class.java)
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Vybrat poníka")
            }

            Spacer(Modifier.height(16.dp))

            /** Spustit jízdu – povoleno jen pokud je vybráno jméno **/
            Button(
                onClick = {
                    // 1) Start foreground služby
                    ContextCompat.startForegroundService(
                        context,
                        Intent(context, RideService::class.java)
                    )
                    // 2) Otevři RideActivity
                    context.startActivity(
                        Intent(context, RideActivity::class.java)
                            .putExtra("horseName", selectedHorseName)
                    )
                },
                enabled = selectedHorseName.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Spustit jízdu")
            }
        }
    }
}
