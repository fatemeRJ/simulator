package com.example.simulator.model

import java.time.LocalDateTime

data class GPSTrackerDataDTO(
    val timestamp: LocalDateTime,
    val vin : String,
    val plaque: String,
    val location : String,
    val speed : Int,
    val engineRpm : Int,
    val ignition : Ignition,
    val kilometer : Int
)
