package com.example.horseridetracker

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "rides")
data class Ride(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val ponyName: String,
    val date: Date,
    val durationSeconds: Long,
    val distanceKm: Float,

    val stepSeconds: Long,
    val trotSeconds: Long,
    val canterSeconds: Long
)
