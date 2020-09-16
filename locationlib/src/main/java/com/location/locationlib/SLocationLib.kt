package com.location.locationlib

import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.location.locationlib.configuration.*
import com.location.locationlib.configuration.Constants
import com.location.locationlib.configuration.getAllPermissions
import com.location.locationlib.configuration.hasPermission
import com.location.locationlib.location.LocationActivity
import com.location.locationlib.location.LocationProvider
import com.location.locationlib.location.ResultData
import com.location.locationlib.receivers.LocationPermissionReceiver
import java.util.concurrent.atomic.AtomicBoolean

internal var locationLiveData = MutableLiveData<ResultData>()

internal var permissionLiveData = MutableLiveData<String>()

internal val isPermissionRequesting = AtomicBoolean().apply {
    set(false)
}
internal var textConfig: PermissionTextConfigurations = PermissionTextConfigurations()

object SLocationLib {

    private var config = LocationConfigurations()


    private lateinit var locationProvider: LocationProvider

    @JvmStatic
    fun getCurrentLocation(
        context: Context,
        onResult: (ResultData) -> Unit
    ) {
        initLocationProvider(context.applicationContext)
        checkAndStartLocationUpdates(context.applicationContext, onResult)
    }
    @JvmStatic
    fun startLocationUpdates(context: Context): LiveData<ResultData> {
        checkAndStartLocationUpdates(context.applicationContext)
        return locationLiveData
    }
    @JvmStatic
    fun <T> startLocationUpdates(
        lifecycleOwnerContext: T,
        onResult: (ResultData) -> Unit
    ) where T : Context, T : LifecycleOwner {
        logDebug("startLocationUpdates")
        locationLiveData.observe(lifecycleOwnerContext, Observer(onResult))
        startLocationUpdates(lifecycleOwnerContext.applicationContext)
    }
    @JvmStatic
    fun locationConfigure(func: LocationConfigurations.() -> Unit) {
        func(config)
    }
    @JvmStatic
    fun permissionTextConfigure(func: PermissionTextConfigurations.() -> Unit) {
        func(textConfig)
    }

    private fun initLocationProvider(context: Context) {
        if (!::locationProvider.isInitialized) {
            locationProvider = LocationProvider(context)
        }
    }

    private fun checkAndStartLocationUpdates(
        context: Context,
        singleUpdate: ((ResultData) -> Unit)? = null
    ) {
        val observer = LocationPermissionReceiver {
            it?.let { error ->
                singleUpdate?.let {
                    it(ResultData.error(error))
                } ?: locationLiveData.postValue(ResultData.error(it))
            } ?: startUpdates(context, singleUpdate)
        }
        when {
            !getAllPermissions(config.enableBackgroundUpdates).all(context::hasPermission) ->
                startPermissionAndResolutionProcess(context, observer, singleUpdate != null)
            config.shouldResolveRequest ->
                // has permissions, need to check for location settings
                checkLocationSettings(context) { isSatisfied ->
                    if (isSatisfied) {
                        startUpdates(context, singleUpdate)
                    } else {
                        startPermissionAndResolutionProcess(context, observer, singleUpdate != null)
                    }
                }
            else ->
                // has permission but location settings resolution is disabled so start updates directly
                startUpdates(context, singleUpdate)
        }
    }

    private fun startUpdates(context: Context, singleUpdate: ((ResultData) -> Unit)? = null) {
        initLocationProvider(context.applicationContext)
        logDebug("startUpdates $singleUpdate")
        if (singleUpdate == null) {
            locationProvider.startUpdates(getLocationRequest(config))
        } else {
            locationProvider.getCurrentLocation(getLocationRequest(config), singleUpdate)
        }
    }

    private fun startPermissionAndResolutionProcess(
        context: Context,
        permissionObserver: Observer<String>,
        isOneTime: Boolean = false
    ) {
        if (isPermissionRequesting.getAndSet(true)) {
//            logDebug("A request is already ongoing")
            return
        }
        val intent = getLocationActivityIntent(context, isOneTime)
        permissionLiveData.observeForever(permissionObserver)
        if (appIsInForeground(context)) {
            context.applicationContext.startActivity(intent)
        } else {
            val pendingIntent =
                PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
            showPermissionNotification(context, pendingIntent)
        }
    }

    private fun getLocationActivityIntent(context: Context, isOneTime: Boolean) =
        Intent(context, LocationActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            if (config.shouldResolveRequest) {
                putExtra(Constants.INTENT_EXTRA_CONFIGURATION, config)
                putExtra(Constants.INTENT_EXTRA_IS_SINGLE_UPDATE, isOneTime)
            }
        }

    private fun appIsInForeground(context: Context): Boolean {
        return (context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)?.runningAppProcesses?.filter {
            it.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
        }?.any {
            it.pkgList.any { pkg -> pkg == context.packageName }
        } ?: false
    }

    private fun showPermissionNotification(context: Context, pendingIntent: PendingIntent) {
        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    "permission_channel",
                    "Permission Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            manager.createNotificationChannel(channel)
        }
        with(NotificationCompat.Builder(context, "permission_channel")) {
            setContentTitle("Require Location Permission")
            setContentText("This feature requires location permission to access device location. Please allow to access device location")
            setSmallIcon(R.drawable.ic_launcher)
            addAction(NotificationCompat.Action.Builder(0, "Grant", pendingIntent).build())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                priority = NotificationManager.IMPORTANCE_HIGH
            }
            setAutoCancel(true)
            manager.notify(Constants.LOCATION_NOTIFICATION_ID, build())
        }
    }

    private fun checkLocationSettings(context: Context, onResult: (Boolean) -> Unit) {
        val builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(getLocationRequest(config))
        builder.setAlwaysShow(true)
        val client = LocationServices.getSettingsClient(context)
        val locationSettingsResponseTask = client.checkLocationSettings(builder.build())
        locationSettingsResponseTask.addOnSuccessListener {
            onResult(true)
        }.addOnFailureListener { exception ->
            logError(exception)
            onResult(false)
        }
    }
    @JvmStatic
    fun stopLocationUpdates() {
        if (::locationProvider.isInitialized) {
            locationProvider.stopUpdates()
        }
    }
}