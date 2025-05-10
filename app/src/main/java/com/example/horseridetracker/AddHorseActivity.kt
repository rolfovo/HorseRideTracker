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

/**
 * • “Add” mód = přidá nového koně a spustí kalibraci.
 * • “Edit” mód = přejmenuje stávajícího koně, jen se zavře.
 */
class AddHorseActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val originalName = intent.getStringExtra(KEY_ORIGINAL_NAME) // null = přidávání
        setContent {
            HorseRideTrackerTheme {
                Surface(Modifier.fillMaxSize()) { AddHorseScreen(originalName) }
            }
        }
    }
    companion object { const val KEY_ORIGINAL_NAME = "originalName" }
}

@Composable
private fun AddHorseScreen(originalName: String?) {
    val context = LocalContext.current
    var pony by remember { mutableStateOf(originalName ?: "") }
    val isEdit = originalName != null

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            if (isEdit) "Uprav jméno koně" else "Přidej nového koně",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = pony,
            onValueChange = { pony = it },
            label = { Text("Jméno koně") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                saveHorse(context, pony.trim(), originalName)

                if (!isEdit) {   // jen při přidání spustím kalibraci
                    context.startActivity(
                        Intent(context, CalibrationActivity::class.java)  // ⚠️ pokud máš CalibrationActivity v podbalíčku, přidej příslušný import
                            .putExtra("pony", pony.trim())
                    )
                }
                (context as? ComponentActivity)?.finish()
            },
            enabled = pony.trim().isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (isEdit) "Uložit změny" else "Pokračovat na kalibraci") }
    }
}

/** Uloží / přejmenuje koně přes společný HorsePrefs */
private fun saveHorse(context: android.content.Context, newName: String, oldName: String?) {
    val horses = HorsePrefs.load(context).toMutableSet()
    if (horses.any { it.equals(newName, true) && it != oldName }) return  // duplicita

    oldName?.let { horses.remove(it) }
    horses.add(newName)
    HorsePrefs.save(context, horses)
}
