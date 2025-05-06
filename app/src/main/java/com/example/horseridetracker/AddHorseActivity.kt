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
import android.content.Context


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
        Text("Zadej jméno koně", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = horseName,
            onValueChange = { horseName = it },
            label = { Text("Jméno koně") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val prefs = context.getSharedPreferences("horses", Context.MODE_PRIVATE)
                val currentHorses = prefs.getStringSet("horseNames", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
                currentHorses.add(horseName.trim())
                prefs.edit().putStringSet("horseNames", currentHorses).apply()

                val intent = Intent(context, CalibrationActivity::class.java)
                intent.putExtra("horseName", horseName.trim())
                context.startActivity(intent)

                if (context is ComponentActivity) {
                    context.finish()
                }
            },
            enabled = horseName.trim().isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Pokračovat na kalibraci")
        }
    }
}
