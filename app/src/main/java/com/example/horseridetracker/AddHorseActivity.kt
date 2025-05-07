package com.example.horseridetracker

import android.content.Context
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
        setContent { HorseRideTrackerTheme { AddHorseScreen() } }
    }
}

@Composable
fun AddHorseScreen() {
    val context = LocalContext.current
    var horseName by remember { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Zadej jméno koně", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = horseName,
            onValueChange = { horseName = it },
            label = { Text("Jméno koně") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                val prefs   = context.getSharedPreferences("horses", Context.MODE_PRIVATE)
                val current = prefs.getStringSet(PrefKeys.HORSE_NAMES, mutableSetOf())?.toMutableSet()
                    ?: mutableSetOf()

                val trimmed = horseName.trim()
                if (trimmed.isNotEmpty()) {
                    current.add(trimmed)
                    prefs.edit().putStringSet(PrefKeys.HORSE_NAMES, current).apply()

                    context.startActivity(
                        Intent(context, CalibrationActivity::class.java)
                            .putExtra("horseName", trimmed)
                    )
                    (context as? ComponentActivity)?.finish()
                }
            },
            enabled = horseName.trim().isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Pokračovat na kalibraci") }
    }
}
