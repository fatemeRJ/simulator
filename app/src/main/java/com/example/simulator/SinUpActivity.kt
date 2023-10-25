package com.example.simulator

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.simulator.network.SimulatorRetrofit
import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File


class SinUpActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        val registry = File(applicationContext.filesDir,"registry.txt")
        val vin = findViewById<EditText>(R.id.vin)
        val plaque = findViewById<EditText>(R.id.plaque)
        val signIn = findViewById<Button>(R.id.signUpButton)
        signIn.setOnClickListener {
            checkExistence(vin.text.toString(),plaque.text.toString(),registry)
        }
    }

    private fun addRegistryToFile(vin : String, plaque : String, registry : File){
        registry.writeText(vin+"\n", Charsets.UTF_8)
        registry.appendText(plaque, Charsets.UTF_8)
    }

    internal fun checkExistence(vin : String, plaque: String, registry: File){
        val result: Call<JsonObject> = SimulatorRetrofit.retrofit.singUp(vin, plaque)
        result.enqueue(object : Callback<JsonObject?> {
            override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                if(response.isSuccessful) {
                    addRegistryToFile(vin, plaque, registry)
                    val intent = Intent(this@SinUpActivity, MainActivity::class.java)
                    startActivity(intent)
                }else{
                    Toast.makeText(this@SinUpActivity,"vin or plaque is wrong",Toast.LENGTH_LONG).show()
                    val intent = Intent(this@SinUpActivity, SinUpActivity::class.java)
                    startActivity(intent)
                }
            }

            override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                Toast.makeText(this@SinUpActivity,t.message,Toast.LENGTH_LONG).show()
                val intent = Intent(this@SinUpActivity, SinUpActivity::class.java)
                startActivity(intent)
            }
        })
    }
}