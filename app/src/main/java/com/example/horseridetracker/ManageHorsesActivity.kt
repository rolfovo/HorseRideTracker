// ManageHorsesActivity.kt
package com.example.horseridetracker

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.horseridetracker.ui.theme.HorseRideTrackerTheme

class ManageHorsesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HorseRideTrackerTheme {
                ManageHorsesScreen()
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageHorsesScreen() {
    val context = LocalContext.current
    // načtení koní z SharedPrefs
    var horses by remember { mutableStateOf(loadHorses(context)) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Moji poníci") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                // přidávání nového – starý AddHorseActivity v režimu "new"
                context.startActivity(Intent(context, AddHorseActivity::class.java))
            }) { Text("+") }
        }
    ) { padding ->
        LazyColumn(contentPadding = padding) {
            items(horses) { name ->
                HorseRow(
                    name = name,
                    onEdit = { oldName ->
                        // spustíme AddHorseActivity v režimu „edit“
                        context.startActivity(
                            Intent(context, AddHorseActivity::class.java).apply {
                                putExtra("originalName", oldName)
                            }
                        )
                    },
                    onDelete = { toDelete ->
                        confirmAndDelete(context, toDelete)
                        horses = loadHorses(context)  // refresh UI
                    }
                )
            }
        }
    }
}

@Composable
fun HorseRow(
    name: String,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    Card(modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(name, style = MaterialTheme.typography.titleMedium)
            Row {
                IconButton(onClick = { onEdit(name) }) {
                    Icon(Icons.Default.Edit, contentDescription = "Upravit")
                }
                IconButton(onClick = { onDelete(name) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Smazat")
                }
            }
        }
    }
}

private fun loadHorses(context: Context): List<String> {
    val prefs = context.getSharedPreferences("horses", Context.MODE_PRIVATE)
    return prefs.getStringSet("horseNames", emptySet())!!.sorted()
}

private fun confirmAndDelete(context: Context, name: String) {
    val prefs = context.getSharedPreferences("horses", Context.MODE_PRIVATE)
    val updated = prefs.getStringSet("horseNames", mutableSetOf())!!.toMutableSet()
    updated.remove(name)
    prefs.edit().putStringSet("horseNames", updated).apply()
}
