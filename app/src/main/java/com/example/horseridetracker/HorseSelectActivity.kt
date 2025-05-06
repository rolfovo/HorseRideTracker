package com.example.horseridetracker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.horseridetracker.ui.theme.HorseRideTrackerTheme

class HorseSelectActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HorseRideTrackerTheme {
                HorseSelectScreen()
            }
        }
    }
}
object ScaffoldSnackbar {
    fun show(context: android.content.Context, message: String) {
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
    }
}


@Composable
fun HorseSelectScreen() {
    val context = LocalContext.current
    val horses = listOf("Orin", "Ranger", "Stormy")

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {

        Text("Vyber koně", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))

        horses.forEach { horseName ->
            Text(
                text = horseName,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .clickable {
                        // Později: startActivity pro jízdu s tímto koněm
                        // context.startActivity(Intent(context, RideActivity::class.java).putExtra("horseName", horseName))
                        ScaffoldSnackbar.show(context, "Vybrán kůň: $horseName")
                    }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = {
            context.startActivity(Intent(context, AddHorseActivity::class.java))
        }) {
            Text("Přidat koně")
        }
    }
}
