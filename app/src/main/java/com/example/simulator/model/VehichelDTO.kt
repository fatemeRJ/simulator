package com.example.simulator.model

import android.os.Build
import androidx.annotation.RequiresApi
import com.google.gson.JsonObject
import java.io.File
import java.time.LocalDateTime
import java.util.Objects
import kotlin.random.Random


class VehichelDTO {
    @RequiresApi(Build.VERSION_CODES.O)
    var timestamp: LocalDateTime = LocalDateTime.now()
    var vehiclePlaque: String = ""
    var vehicleVin: String = ""
    var kilometer: Int = 0;
    var lat: Double = 0.0
    var lng: Double = 0.0
    var fuel: Float = 0.0F
    var ignitionStatus: Ignition = Ignition.OFF
    var temperatureGauge: Float = 0.0F
    var rpm: Int = 0
    var speed: Int = 0
    var steerDegree = 0
    var light: Boolean = false
    var lock: Boolean = false
    var lastSpeed: Int = 0


    private val gearMAXMap = mapOf(1 to 60, 2 to 94, 3 to 140, 4 to 190, 5 to 234)

    fun setNewKilometer(kilometer: Int?) {
        if (Objects.isNull(kilometer) || kilometer == 0) {
            this.kilometer = (1000..15000).random()
        } else {
            if (kilometer != null) {
                this.kilometer = kilometer
            }
        }
    }

    fun setVinAndPlaque(registry: File) {
        val lines = registry.readLines()
        this.vehicleVin = lines[0]
        this.vehiclePlaque = lines[1]

    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun updateAttributes(
        ignition: Boolean, steeringProgress: Int, acceleratorProgress: Int, brakeProgress: Int,
        gearState: Int, automate: Boolean, lat: Double, lng: Double,
        light: Boolean, lock: Boolean
    ) {
        timestamp = LocalDateTime.now()
        kilometer = speed * 10 / 3600
        fuel = Random.nextFloat() * 50.0F
        setSteering(steeringProgress)
        setSpeedAndRPM(ignition, automate, acceleratorProgress, brakeProgress, gearState, lat, lng)
        setIgnitionStatus(ignition)
        temperatureGauge = 20.0F + Random.nextFloat() * 10.0F
        this.lat = lat
        this.lng = lng
        this.light = light
        this.lock = lock

    }

    private fun setSteering(steeringProgress: Int) {
        steerDegree = 360 * steeringProgress / 100
    }

    private fun setIgnitionStatus(ignition: Boolean) {
        if (!ignition && lastSpeed == 0 && speed == 0)
            ignitionStatus = Ignition.ACC
        else {
            if (ignition)
                ignitionStatus = Ignition.ON
            else
                ignitionStatus = Ignition.OFF
        }
    }

    private fun setSpeedAndRPM(ignition: Boolean,automate: Boolean,acceleratorProgress: Int, brakeProgress: Int, gearState: Int,lat: Double, lng: Double){
        if(ignition){
            if(automate){
                setAutomateSpeedAndRPM(lat,lng)
            }else{
                setManualSpeed(acceleratorProgress, brakeProgress, gearState)
            }
        }else{
            speed = 0
            rpm = 0
        }
    }

    fun setManualSpeedAndRPM(acceleratorProgress: Int, brakeProgress: Int, gearState: Int) {
        lastSpeed = speed
        setManualRPM(acceleratorProgress, brakeProgress)
        setManualSpeed(acceleratorProgress, brakeProgress, gearState)
    }

    fun setAutomateSpeedAndRPM(lat: Double, lng: Double) {
        lastSpeed = speed
        setAutomateSpeed(lat, lng)
        setAutomateRPM()
    }

    private fun setAutomateSpeed(lat: Double, lng: Double) {
        speed = (distance(lat, lng) * (10 / 3600)).toInt()
    }

    private fun setAutomateRPM() {
        when (speed) {
            in 0..25 -> rpm = (3000 * speed) / 25
            in 26..40 -> rpm = (3000 * speed) / 40
            in 41..60 -> rpm = (3000 * speed) / 60
            in 61..80 -> rpm = (3000 * speed) / 80
            in 81..100 -> rpm = (3000 * speed) / 100
            else -> rpm = (speed * 7000) / gearMAXMap[5]!!
        }
    }

    private fun setManualRPM(acceleratorProgress: Int, brakeProgress: Int) {
        this.rpm = getManualRPM(acceleratorProgress, brakeProgress)
    }

    private fun setManualSpeed(acceleratorProgress: Int, brakeProgress: Int, gearState: Int) {
        this.speed = getManualSpeed(acceleratorProgress, brakeProgress, gearState)
    }

    fun getManualSpeed(acceleratorProgress: Int, brakeProgress: Int, gearState: Int): Int {
        var newSpeed = ((acceleratorProgress - brakeProgress) * gearMAXMap[gearState]!!) / 10
        if (newSpeed < 0)
            newSpeed = 0
        return newSpeed
    }

    fun getManualRPM(acceleratorProgress: Int, brakeProgress: Int): Int {
        var newRPM = ((acceleratorProgress - brakeProgress) * 7000) / 10
        if (rpm < 0)
            newRPM = 0
        return newRPM
    }

    fun distance(otherLat: Double, otherLng: Double): Double {
        val dLat = Math.toRadians((this.lat - otherLat))
        val dLon = Math.toRadians((this.lng - otherLng))
        val lat1 = Math.toRadians(otherLat)
        val lat2 = Math.toRadians(this.lat)
        val a =
            Math.pow(Math.sin(dLat / 2), 2.0) + (Math.pow(Math.sin(dLon / 2), 2.0) * Math.cos(lat1)
                    * Math.cos(lat2))
        val c = 2 * Math.asin(Math.sqrt(a))
        return 6371 * c
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun toJson(): JsonObject {
        var json: JsonObject = JsonObject()
        json.addProperty("timestamp", timestamp.toString())
        json.addProperty("vehiclePlaque", vehiclePlaque)
        json.addProperty("vehicleVin", vehicleVin)
        json.addProperty("kilometer", kilometer)
        json.addProperty("lat", lat.toFloat())
        json.addProperty("lng", lng.toFloat())
        json.addProperty("fuel", fuel)
        json.addProperty("ignitionStatus", ignitionStatus.name)
        json.addProperty("temperatureGauge", temperatureGauge)
        json.addProperty("rpm", rpm)
        json.addProperty("speed", speed)
        json.addProperty("lock", lock)
        json.addProperty("vehicleVin", vehicleVin)
        return json
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun toMap(): Map<String, Any> {
        var map = mutableMapOf<String, Any>()
        map.put("timestamp", timestamp)
        map.put("vehiclePlaque", vehiclePlaque)
        map.put("vehicleVin", vehicleVin)
        map.put("kilometer", kilometer)
        map.put("lat", lat.toFloat())
        map.put("lng", lng.toFloat())
        map.put("fuel", fuel)
        map.put("ignitionStatus", ignitionStatus)
        map.put("temperatureGauge", temperatureGauge)
        map.put("rpm", rpm)
        map.put("speed", speed)
        map.put("lock", lock)
        map.put("vehicleVin", vehicleVin)
        return map
    }
}