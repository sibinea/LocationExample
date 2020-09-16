package com.location.locationlib

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class LocationServiceStopReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.e("onReceive", "Location servie stopped")
        SLocationLib.stopLocationUpdates()
        context.stopService(Intent(context, LocationForegroundService::class.java))
    }

}