package com.example.simulator.network

import com.example.simulator.model.GPSTrackerDataDTO
import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface SimulatorAPI {

    @GET("vehicle-existence")
    fun singUp(@Query("vin") vin: String
                       ,@Query("plaque")  plaque : String ): Call<JsonObject>

    @POST("produce")
    fun send(@Body vehicleStatusDTO: JsonObject):Call<JsonObject>

    @GET("last-state")
    fun getLastState(@Query("vin") vin: String): Call<GPSTrackerDataDTO>
}