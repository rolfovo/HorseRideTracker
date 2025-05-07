package com.example.horseridetracker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.horseridetracker.ui.theme.HorseRideTrackerTheme

class HorseSelectActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HorseRideTrackerTheme {
                HorseSelectScreen { selectedHorse ->
                    val resultIntent = Intent().apply {
                        putExtra("selectedHorse", selectedHorse)
                    }
                    setResult(RESULT_OK, resultIntent)
                    finish()
                }
            }
        }
    }
}