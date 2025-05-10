package com.example.horseridetracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State

class RideStatsViewModel(private val ponyName: String) : ViewModel() {
    private val dao = HorseTrackerApp.db.rideDao()

    // Stavové backing-proměnné
    private val _lastRide = mutableStateOf<Ride?>(null)
    val lastRide: State<Ride?> = _lastRide

    private val _avgStepSpeed = mutableStateOf(0.0)
    val avgStepSpeed: State<Double> = _avgStepSpeed

    private val _avgTrotSpeed = mutableStateOf(0.0)
    val avgTrotSpeed: State<Double> = _avgTrotSpeed

    private val _avgCanterSpeed = mutableStateOf(0.0)
    val avgCanterSpeed: State<Double> = _avgCanterSpeed

    private val _totalDistance = mutableStateOf(0.0)
    val totalDistance: State<Double> = _totalDistance

    // Historie se načítá jako Flow, ale stateIn taky běží na pozadí
    val history = dao.getAllRidesForPony(ponyName)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList<Ride>())

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _lastRide.value       = dao.getLastRide(ponyName)
            _avgStepSpeed.value   = dao.getAvgStepSpeed(ponyName)   ?: 0.0
            _avgTrotSpeed.value   = dao.getAvgTrotSpeed(ponyName)   ?: 0.0
            _avgCanterSpeed.value = dao.getAvgCanterSpeed(ponyName) ?: 0.0
            _totalDistance.value  = dao.getTotalDistance(ponyName)  ?: 0.0
        }
    }
}
