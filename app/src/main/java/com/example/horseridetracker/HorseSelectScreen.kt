package com.example.horseridetracker

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.DisposableEffect

@Composable
fun HorseSelectScreen(onHorseSelected: (String) -> Unit) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("horses", Context.MODE_PRIVATE)

    // State pro seznam koní
    var horses by remember { mutableStateOf(listOf<String>()) }

    // Při každém ON_RESUME načíst aktuální data
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val set = prefs.getStringSet("horseNames", emptySet()) ?: emptySet()
                horses = set.toList()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Vyber koně", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))

        if (horses.isEmpty()) {
            Text("Žádní koně zatím nebyli přidáni.")
        } else {
            LazyColumn {
                items(horses) { name ->
                    Text(
                        text = name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .clickable { onHorseSelected(name) }
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(onClick = {
            context.startActivity(Intent(context, AddHorseActivity::class.java))
        }) {
            Text("Přidat nového koně")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HorseSelectScreenPreview() {
    HorseSelectScreen {}
}
