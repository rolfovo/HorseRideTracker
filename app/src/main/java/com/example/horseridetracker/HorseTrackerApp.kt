package com.example.horseridetracker

import android.app.Application
import androidx.room.Room

class HorseTrackerApp : Application() {
    companion object {
        lateinit var db: AppDatabase
            private set
    }

    override fun onCreate() {
        super.onCreate()
        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "horse-tracker-db"
        ).build()
    }
}
