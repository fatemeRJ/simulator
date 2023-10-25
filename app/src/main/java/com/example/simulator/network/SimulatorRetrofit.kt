package com.example.simulator.network

import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object SimulatorRetrofit {
    val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://simulator.dv.mci.dev/simulator/")
            .client(OkHttpClient())
            .addConverterFactory(
                GsonConverterFactory.create(
                GsonBuilder()
                    .setLenient()
                .create()))
            .build()
            .create(SimulatorAPI::class.java)
    }

}