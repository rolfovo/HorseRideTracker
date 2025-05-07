package com.example.horseridetracker

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment

@Composable
fun HorseSelectScreen(onHorseSelected: (String) -> Unit) {
    val context = LocalContext.current
    val prefs   = context.getSharedPreferences("horses", Context.MODE_PRIVATE)

    var horses by remember { mutableStateOf(listOf<String>()) }
    var horseInDialog by remember { mutableStateOf<String?>(null) }     // jméno, které se právě řeší

    /* --- refresh při každém onResume --- */
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                horses = prefs.getStringSet(PrefKeys.HORSE_NAMES, emptySet())?.toList() ?: emptyList()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    /* --- UI --- */
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Vyber koně", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))

        if (horses.isEmpty()) {
            Text("Žádní koně zatím nejsou.")
        } else {
            LazyColumn {
                items(horses) { name ->

                    /* Jedna položka seznamu */
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onHorseSelected(name) }            // krátký tap
                            .combinedClickable(
                                onLongClick = { horseInDialog = name }     // dlouhý tap → dialog
                            )
                            .padding(12.dp)
                    ) {
                        Text(name, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { context.startActivity(Intent(context, AddHorseActivity::class.java)) }
        ) { Text("Přidat nového koně") }
    }

    /* --- Dialog pro vybraného koně --- */
    horseInDialog?.let { name ->
        AlertDialog(
            onDismissRequest = { horseInDialog = null },
            title = { Text(name) },
            text  = { Text("Co chceš s tímto koněm provést?") },
            confirmButton = {            /* Rekalibrace */
                TextButton(onClick = {
                    context.startActivity(
                        Intent(context, CalibrationActivity::class.java)
                            .putExtra("horseName", name)
                    )
                    horseInDialog = null
                }) { Text("Rekalibrovat") }
            },
            dismissButton = {            /* Smazání */
                TextButton(onClick = {
                    deleteHorse(name, prefs)
                    horses = horses.filterNot { it == name }   // okamžitý refresh v UI
                    horseInDialog = null
                }) { Text("Smazat") }
            },
            neutralButton = {            /* Zpět */
                TextButton(onClick = { horseInDialog = null }) { Text("Zpět") }
            }
        )
    }
}

/* -------------------------------------------------------------------------- */
/* Helper pro mazání – odstranit jméno ze setu + všechny rychlostní klíče     */
private fun deleteHorse(name: String, prefs: android.content.SharedPreferences) {
    prefs.edit().apply {
        /* Set s názvy koní */
        val set = prefs.getStringSet(PrefKeys.HORSE_NAMES, setOf())?.toMutableSet() ?: mutableSetOf()
        set.remove(name)
        putStringSet(PrefKeys.HORSE_NAMES, set)

        /* Individuální rychlosti */
        remove("${name}_step")
        remove("${name}_trot")
        remove("${name}_canter")
    }.apply()
}
