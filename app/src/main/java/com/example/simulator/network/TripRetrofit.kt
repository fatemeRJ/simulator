package com.example.simulator.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object TripRetrofit {
    val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://trip.dv.mci.dev/gps-tracker/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SimulatorAPI::class.java)
    }
}