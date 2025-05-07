package com.example.horseridetracker

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.activity.ComponentActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HorseSelectScreen(onClose: () -> Unit = {}) {
    val context = LocalContext.current
    var horses by remember { mutableStateOf(HorsePrefs.load(context)) }

    /** refresh při návratu z AddHorseActivity **/
    LaunchedEffect(Unit) {
        (context as ComponentActivity)
            .lifecycle.addObserver(
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
                            (context as Activity).setResult(
                                Activity.RESULT_OK,
                                Intent().putExtra("selectedHorse", name)
                            )
                            context.finish()
                            onClose()
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
