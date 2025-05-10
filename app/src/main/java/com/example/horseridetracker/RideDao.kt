package com.example.horseridetracker

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import com.example.horseridetracker.Ride

@Dao
interface RideDao {

    @Insert
    fun insert(ride: Ride): Long

    @Query("SELECT * FROM rides WHERE ponyName = :ponyName ORDER BY date DESC LIMIT 1")
    fun getLastRide(ponyName: String): Ride?

    @Query("""
        SELECT SUM(distanceKm) / (SUM(stepSeconds) / 3600.0)
        FROM rides
        WHERE ponyName = :ponyName AND stepSeconds > 0
    """)
    fun getAvgStepSpeed(ponyName: String): Double?

    @Query("""
        SELECT SUM(distanceKm) / (SUM(trotSeconds) / 3600.0)
        FROM rides
        WHERE ponyName = :ponyName AND trotSeconds > 0
    """)
    fun getAvgTrotSpeed(ponyName: String): Double?

    @Query("""
        SELECT SUM(distanceKm) / (SUM(canterSeconds) / 3600.0)
        FROM rides
        WHERE ponyName = :ponyName AND canterSeconds > 0
    """)
    fun getAvgCanterSpeed(ponyName: String): Double?

    @Query("SELECT SUM(distanceKm) FROM rides WHERE ponyName = :ponyName")
    fun getTotalDistance(ponyName: String): Double?

    @Query("""
        SELECT *
        FROM rides
        WHERE ponyName = :ponyName
        ORDER BY date DESC
    """)
    fun getAllRidesForPony(ponyName: String): Flow<List<Ride>>
}
