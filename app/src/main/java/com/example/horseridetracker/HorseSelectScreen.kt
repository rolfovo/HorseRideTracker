package com.example.horseridetracker

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HorseSelectScreen(onClose: () -> Unit = {}) {
    val context = LocalContext.current

    // State drží seznam jmen
    val horsesState = remember { mutableStateOf(HorsePrefs.load(context)) }

    // Funkce pro (re)načtení seznamu
    fun loadHorses() {
        horsesState.value = HorsePrefs.load(context)
    }

    // Jednorázové načtení při vstupu
    LaunchedEffect(Unit) {
        loadHorses()
    }

    // Lifecycle observer pro opakované načtení při návratu
    val lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                loadHorses()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Moji poníci") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                val intent = Intent(context, AddHorseActivity::class.java)
                    // explicitně null jako String?
                    .putExtra(AddHorseActivity.KEY_ORIGINAL_NAME, null as String?)
                context.startActivity(intent)
            }) {
                Text("+")
            }
        }
    ) { paddingValues ->
        LazyColumn(contentPadding = paddingValues) {
            items(horsesState.value) { name ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .clickable {
                            (context as Activity).setResult(
                                Activity.RESULT_OK,
                                Intent().putExtra("selectedHorse", name)
                            )
                            context.finish()
                            onClose()
                        }
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}
