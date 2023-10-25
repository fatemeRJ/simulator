package com.example.simulator

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.widget.Button
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.simulator.model.GPSTrackerDataDTO
import com.example.simulator.model.VehichelDTO
import com.example.simulator.network.SimulatorRetrofit
import com.example.simulator.network.TripRetrofit
import com.google.android.gms.location.*
import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.util.*


class MainActivity : AppCompatActivity() {
    private val INTERVAL: Long = 10000
    private val FASTEST_INTERVAL: Long = 1000
    private val REQUEST_PERMISSION_LOCATION = 10

    private var mFusedLocationProviderClient: FusedLocationProviderClient? = null
    private lateinit var mLastLocation: Location
    private lateinit var mLocationRequest: LocationRequest

    private var gearState = 1
    var speed = 0
    var rpm = 0


    val vehichelDTO: VehichelDTO = VehichelDTO()
    var ignition: Boolean = false
    var lock: Boolean = false
    var light: Boolean = false
    var automation: Boolean = false

    lateinit var speedView: TextView
    lateinit var gearView: TextView
    lateinit var latView: TextView
    lateinit var longView: TextView
    lateinit var gearShiftUp: Button
    lateinit var gearShiftDown: Button
    lateinit var acceleratorSeekBar: SeekBar
    lateinit var brakeSeekBar: SeekBar
    lateinit var steeringSeekBar: SeekBar

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val registry = File(applicationContext.filesDir, "registry.txt")
        if (!registry.exists()) {
            startActivity(Intent(this, SinUpActivity::class.java))
        }

        vehichelDTO.setVinAndPlaque(registry)

        TripRetrofit.retrofit.getLastState(vehichelDTO.vehicleVin)
            .enqueue(object : Callback<GPSTrackerDataDTO?> {
                override fun onResponse(
                    call: Call<GPSTrackerDataDTO?>,
                    response: Response<GPSTrackerDataDTO?>
                ) {
                    var kilometer: Int? = null
                    if (response.isSuccessful)
                        kilometer = response.body()?.kilometer
                    vehichelDTO.setNewKilometer(kilometer)
                }

                override fun onFailure(call: Call<GPSTrackerDataDTO?>, t: Throwable) {
                }

            })


        speedView = findViewById(R.id.speed_view)
        gearView = findViewById(R.id.gear_level_view)
        latView = findViewById(R.id.lat_view)
        longView = findViewById(R.id.long_view)
        val startStopSwitch = findViewById<Switch>(R.id.StartStop)
        val automationToggle = findViewById<ToggleButton>(R.id.automation_toggle)
        val lightToggle = findViewById<ToggleButton>(R.id.lightToggle)
        val lockToggle = findViewById<ToggleButton>(R.id.lockToggle)
        gearShiftUp = findViewById(R.id.gearShiftUp)
        gearShiftDown = findViewById(R.id.gearShiftDown)
        acceleratorSeekBar = findViewById(R.id.acceleratorSeekBar)
        brakeSeekBar = findViewById(R.id.brakeSeekBar)
        steeringSeekBar = findViewById(R.id.steeringSeekBar)

        mLocationRequest = LocationRequest
            .Builder(Priority.PRIORITY_HIGH_ACCURACY, INTERVAL)
            .build();



        if (checkPermissionForLocation(this))
            startLocationUpdates()


        brakeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!automation && ignition) {
                    vehichelDTO.setManualSpeedAndRPM(
                        acceleratorSeekBar.progress,
                        progress,
                        gearState
                    )
                    speedView.text = vehichelDTO.speed.toString()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }

        })

        acceleratorSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!automation && ignition) {
                    vehichelDTO.setManualSpeedAndRPM(
                        progress,
                        brakeSeekBar.progress,
                        gearState
                    )
                    speedView.text = vehichelDTO.speed.toString()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }

        })

        lockToggle.setOnCheckedChangeListener { buttonView, isChecked ->
            lock = isChecked
        }

        lightToggle.setOnCheckedChangeListener { buttonView, isChecked ->
            light = isChecked
        }

        automationToggle.setOnCheckedChangeListener { buttonView, isChecked ->
            automation = isChecked
        }

        startStopSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            ignition = isChecked
        }

        gearShiftUp.setOnClickListener {
            if (gearState < 5)
                gearState += 1
            updateSpeedView()
            gearView.text = gearState.toString()

        }
        gearShiftDown.setOnClickListener {
            if (gearState > 1)
                gearState -= 1
            updateSpeedView()
            gearView.text = gearState.toString()
        }

        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps()
        }


    }

    private fun buildAlertMessageNoGps() {

        val builder = AlertDialog.Builder(this)
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
            .setCancelable(false)
            .setPositiveButton("Yes") { dialog, id ->
                startActivityForResult(
                    Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), 11
                )
            }
            .setNegativeButton("No") { dialog, id ->
                dialog.cancel()
                finish()
            }
        val alert: AlertDialog = builder.create()
        alert.show()


    }


    private fun startLocationUpdates() {

        // Create LocationSettingsRequest object using location request
        val builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(mLocationRequest!!)
        val locationSettingsRequest = builder.build()

        val settingsClient = LocationServices.getSettingsClient(this)
        settingsClient.checkLocationSettings(locationSettingsRequest)

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        // new Google API SDK v11 uses getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            != PackageManager.PERMISSION_GRANTED
        ) {

            return
        }
        mFusedLocationProviderClient!!.requestLocationUpdates(
            mLocationRequest, mLocationCallback,
            Looper.myLooper()
        )
    }

    private val mLocationCallback = object : LocationCallback() {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.lastLocation
            locationResult.lastLocation?.let { onLocationChanged(it) }
        }
    }

    @SuppressLint("SuspiciousIndentation")
    @RequiresApi(Build.VERSION_CODES.O)
    fun onLocationChanged(location: Location) {
        mLastLocation = location
        vehichelDTO.updateAttributes(
            ignition, steeringSeekBar.progress, acceleratorSeekBar.progress, brakeSeekBar.progress,
            gearState, automation, location.latitude, location.longitude, light, lock
        )
        speedView.text = vehichelDTO.speed.toString()
        latView.text = vehichelDTO.lat.toString()
        longView.text = vehichelDTO.lng.toString()
        gearView.text = gearState.toString()

        SimulatorRetrofit.retrofit.send(vehichelDTO.toJson())
            .enqueue(object : Callback<JsonObject?> {
                override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                    print("")
                }

                override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                    print("")
                }
            })
    }

    private fun stoplocationUpdates() {
        mFusedLocationProviderClient!!.removeLocationUpdates(mLocationCallback)
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION_LOCATION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates()

            } else {
                Toast.makeText(this@MainActivity, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkPermissionForLocation(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                true
            } else {
                // Show the permission request
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_PERMISSION_LOCATION
                )
                false
            }
        } else {
            true
        }
    }

    fun updateSpeedView() {
        if (!automation)
            speedView.text = vehichelDTO.getManualSpeed(
                acceleratorSeekBar.progress,
                brakeSeekBar.progress,
                gearState
            ).toString()
    }

}
