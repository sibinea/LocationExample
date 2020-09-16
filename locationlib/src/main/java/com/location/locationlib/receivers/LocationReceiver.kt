package com.location.locationlib.receivers

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.LocationResult
import com.location.locationlib.configuration.logDebug
import com.location.locationlib.location.ResultData
import com.location.locationlib.locationLiveData

internal class LocationReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_LOCATION_UPDATES =
            "locationlib.LocationReceiver.action.LOCATION_UPDATES"

        fun getPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, LocationReceiver::class.java)
            intent.action = ACTION_LOCATION_UPDATES
            return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        intent ?: return
        if (intent.action == ACTION_LOCATION_UPDATES) {
            LocationResult.extractResult(intent)?.let { result ->
                if (result.locations.isNotEmpty()) {
                    logDebug("New Location ${result.lastLocation}")
                    locationLiveData.postValue(ResultData.success(result.lastLocation))
                }
            }
        }
    }
}