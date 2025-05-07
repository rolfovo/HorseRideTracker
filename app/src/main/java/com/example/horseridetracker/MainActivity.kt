package com.example.horseridetracker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.horseridetracker.ui.theme.HorseRideTrackerTheme
@OptIn(ExperimentalMaterial3Api::class)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            HorseRideTrackerTheme {
                var selectedHorseName by remember { mutableStateOf("") }

                // Launcher pro výběr koně
                val selectHorseLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == RESULT_OK) {
                        val name = result.data?.getStringExtra("selectedHorse")
                        if (!name.isNullOrBlank()) {
                            selectedHorseName = name
                        }
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(title = { Text("Horse Ride Tracker") })
                    }
                ) { padding ->
                    Column(
                        modifier = Modifier
                            .padding(padding)
                            .padding(16.dp)
                            .fillMaxSize()
                    ) {
                        Text(
                            text = if (selectedHorseName.isEmpty()) "Žádný kůň vybrán" else "Vybraný kůň: $selectedHorseName",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(onClick = {
                            val intent = Intent(this@MainActivity, HorseSelectActivity::class.java)
                            selectHorseLauncher.launch(intent)
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text("Vybrat koně")
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val intent = Intent(this@MainActivity, RideActivity::class.java)
                                intent.putExtra("horseName", selectedHorseName)
                                startActivity(intent)
                            },
                            enabled = selectedHorseName.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Spustit jízdu")
                        }
                    }
                }
            }
        }
    }
}