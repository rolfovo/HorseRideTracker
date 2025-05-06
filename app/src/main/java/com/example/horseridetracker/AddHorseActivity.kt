package com.example.horseridetracker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.horseridetracker.ui.theme.HorseRideTrackerTheme

class AddHorseActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HorseRideTrackerTheme {
                AddHorseScreen()
            }
        }
    }
}

@Composable
fun AddHorseScreen() {
    val context = LocalContext.current
    var horseName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Zadej jméno koně", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = horseName,
            onValueChange = { horseName = it },
            label = { Text("Jméno koně") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (horseName.isNotBlank()) {
                    // Předáme jméno do kalibrace
                    val intent = Intent(context, CalibrationActivity::class.java)
                    intent.putExtra("horseName", horseName)
                    context.startActivity(intent)
                }
            },
            enabled = horseName.isNotBlank()
        ) {
            Text("Uložit a kalibrovat")
        }
    }
}
