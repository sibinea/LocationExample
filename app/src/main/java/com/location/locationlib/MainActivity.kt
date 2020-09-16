package com.location.locationlib

import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.home_activity.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home_actvity)
        btnSingleLocation.setOnClickListener {
            SLocationLib.getCurrentLocation(this) { resultData ->
                resultData.location?.let {
                    tvLocation.text =
                        "Location lat : ${resultData.location!!.latitude} long :  ${resultData.location!!.longitude}"
                    Log.d("CurrentLocation", "" + resultData.location.toString())
                } ?: run {
                    Log.d("CurrentLocation", "Not available")
                    tvLocation.text = "Location is Unavailable"

                }
            }
        }
        btnStartService.setOnClickListener {
            SLocationLib.locationConfigure {
                enableBackgroundUpdates = true
                shouldResolveRequest = true
            }
            SLocationLib.startLocationUpdates(this) { resultData ->
                resultData.location?.let(::onLocationUpdate)
                resultData.error?.let {
                    tvLocation.text = "Location is Unavailable"

                }
            }
        }
        btnStartForeService.setOnClickListener {
            startService(Intent(this, LocationForegroundService::class.java))
        }
        btnStopForeService.setOnClickListener {
            val intent = Intent(this, LocationServiceStopReceiver::class.java).apply {
                action = LocationForegroundService.STOP_SERVICE_BROADCAST_ACTON
            }
            sendBroadcast(intent)
        }
    }

    private fun onLocationUpdate(location: Location) {
        Log.d("LocationUpdate", "" + location.toString())
        tvLocation.text =
            "Location lat : ${location!!.latitude} long :  ${location!!.longitude}"

    }
}




