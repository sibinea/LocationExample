package com.location.locationlib.location

import android.app.PendingIntent
import android.content.Context
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.*
import com.location.locationlib.configuration.logDebug
import com.location.locationlib.configuration.logError
import com.location.locationlib.locationLiveData
import com.location.locationlib.receivers.LocationReceiver
import java.util.concurrent.atomic.AtomicBoolean

internal class LocationProvider(context: Context) {


    private val mFusedLocationProviderClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }
    private val isLocationRequestActive = AtomicBoolean().apply { set(false) }

    private val pendingIntent: PendingIntent by lazy {
        LocationReceiver.getPendingIntent(context)
    }

    internal fun getCurrentLocation(
        request: LocationRequest,
        onUpdate: (ResultData) -> Unit
    ) {
        fun startUpdates() {
            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult?) {
                    result?.lastLocation?.let { location ->
                        onUpdate(ResultData.success(location))
                        mFusedLocationProviderClient.removeLocationUpdates(this)
                    }
                }
            }
            mFusedLocationProviderClient.requestLocationUpdates(
                request.apply { numUpdates = 1 },
                callback,
                Looper.getMainLooper()
            ).addOnFailureListener { error ->
                logError(error)
                onUpdate(ResultData.error(error = error))
            }
        }
        mFusedLocationProviderClient.lastLocation?.addOnSuccessListener { result ->
            result?.let { location ->
                onUpdate(ResultData.success(location))
            } ?: startUpdates()
        }?.addOnFailureListener {
            logError(it)
            startUpdates()
        }
    }



    internal fun startUpdates(request: LocationRequest) {
        logDebug("isRequestOngoing ${isLocationRequestActive.get()}")
        if (isLocationRequestActive.getAndSet(true)) return
        logDebug("Starting location updates")
        mFusedLocationProviderClient.requestLocationUpdates(request, pendingIntent)
            ?.addOnFailureListener { e ->
                logError(e)
                logDebug("Continuous location updates failed, retrieving last known location")
                mFusedLocationProviderClient.lastLocation?.addOnCompleteListener {
                    if (!it.isSuccessful) return@addOnCompleteListener
                    it.result?.let { location ->
                        locationLiveData.postValue(ResultData.success(location))
                    }
                }?.addOnFailureListener {
                    locationLiveData.postValue(ResultData.error(error = it))
                }
            }
    }

    internal fun stopUpdates() {
        logDebug("removing location updates")
        isLocationRequestActive.set(false)
        locationLiveData = MutableLiveData()
        mFusedLocationProviderClient.removeLocationUpdates(pendingIntent)
    }
}