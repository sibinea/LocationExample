package com.location.locationlib

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.Observer
import com.location.locationlib.location.ResultData

class LocationForegroundService : LifecycleService() {

    companion object {
        const val NOTIFICATION_ID = 787
        const val STOP_SERVICE_BROADCAST_ACTON =
            "com.location.locationlib.LocationServiceStopReceiver"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Handler().postDelayed({
            startLocationBackgroundFetch()
        }, 1000)
        return START_REDELIVER_INTENT
    }

    private fun startLocationBackgroundFetch() {
        startForeground(NOTIFICATION_ID, getForegroundNotification())
        SLocationLib.locationConfigure {
            enableBackgroundUpdates = true
        }
        SLocationLib.startLocationUpdates(this).observe(this, Observer { result ->
            manager.notify(NOTIFICATION_ID, getForegroundNotification(result))
        })

        SLocationLib.startLocationUpdates(this) { resultData ->
            resultData.location?.let {
                Log.e("location", "${resultData.location.toString()}")
            }
            resultData.error?.let {
//                tvLocation.text = "Location is Unavailable"

            }
        }
    }

    private val manager: NotificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: throw Exception("No notification manager found")
    }

    private fun getForegroundNotification(resultData: ResultData? = null): Notification {
        val manager =
            getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: throw Exception("No notification manager found")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    "location",
                    "Location Updates",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
        }
        return with(NotificationCompat.Builder(this, "location")) {
            setContentTitle("Location Service")
            resultData?.apply {
                location?.let {
                    setContentText("${it.latitude}, ${it.longitude}")
                } ?: setContentText("Error: ${error?.message}")
            } ?: setContentText("Getting location update ...")
            setSmallIcon(R.drawable.ic_launcher)
            setAutoCancel(false)
            setOnlyAlertOnce(true)
//            addAction(
//                0,
//                "Stop Updates",
//                PendingIntent.getBroadcast(
//                    this@LocationForegroundService,
//                    0,
//                    Intent(this@LocationForegroundService, LocationServiceStopReceiver::class.java).apply {
//                        action = STOP_SERVICE_BROADCAST_ACTON
//                    },
//                    PendingIntent.FLAG_UPDATE_CURRENT
//                )
//            )
            build()
        }
    }
}


