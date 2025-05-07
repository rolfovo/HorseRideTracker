package com.example.horseridetracker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import android.app.Activity
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver              // ⬅️ nový import
import com.example.horseridetracker.ui.theme.HorseRideTrackerTheme

class HorseSelectActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { HorseRideTrackerTheme { HorseSelectScreen() } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HorseSelectScreen() {
    val context = LocalContext.current
    var horses by remember { mutableStateOf(HorsePrefs.load(context)) }

    /* ---- refresh po návratu z AddHorseActivity ---- */
    LaunchedEffect(Unit) {
        (context as ComponentActivity)                         // ⬅️ cast na ComponentActivity
            .lifecycle
            .addObserver(
                LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        horses = HorsePrefs.load(context)
                    }
                }
            )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Vyber poně") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                context.startActivity(Intent(context, AddHorseActivity::class.java))
            }) { Text("+") }
        }
    ) { padding ->
        LazyColumn(contentPadding = padding) {
            items(horses) { name ->
                Card(
                    Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .clickable {
                            (context as ComponentActivity).setResult(
                                Activity.RESULT_OK,
                                Intent().putExtra("selectedHorse", name)
                            )
                            context.finish()
                        }
                ) {
                    Text(
                        name,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}
