package com.location.locationlib.location

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationSettingsStatusCodes
import com.location.locationlib.R
import com.location.locationlib.configuration.*
import com.location.locationlib.configuration.Constants
import com.location.locationlib.configuration.backgroundPermission
import com.location.locationlib.configuration.locationPermissions
import com.location.locationlib.isPermissionRequesting
import com.location.locationlib.permissionLiveData


class LocationActivity : AppCompatActivity(), ActivityCompat.OnRequestPermissionsResultCallback {

    companion object {
        private const val REQUEST_CODE_LOCATION_SETTINGS = 985
        private const val REQUEST_CODE_PERMISSION = 986
        private const val REQUEST_CODE_SETTINGS_ACTIVITY = 659
        private const val PREF_NAME = "location_pref"
    }

    private var config: LocationConfigurations = LocationConfigurations()
    private val pref: SharedPreferences by lazy {
        getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }
    private var permissions: Array<String> = arrayOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_layout)
        intent?.getParcelableExtra<LocationConfigurations>(Constants.INTENT_EXTRA_CONFIGURATION)
            ?.let {
                config = it
            } ?: logError("No config is sent to the permission activity")
        val isSingleUpdate =
            intent?.getBooleanExtra(Constants.INTENT_EXTRA_IS_SINGLE_UPDATE, false) ?: false
        permissions =
            if (config.enableBackgroundUpdates && !isSingleUpdate) locationPermissions + backgroundPermission else locationPermissions
        initPermissionModel()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_LOCATION_SETTINGS) {

            checkSettings(
                ::shouldProceedForLocation,
                { postResult(Constants.LOCATION_SETTINGS_DENIED) },
                this,
                getLocationRequest(config)
            )

        } else if (requestCode == REQUEST_CODE_SETTINGS_ACTIVITY) {
            if (permissions.all(::hasPermission)) {
                onPermissionGranted()
            } else {
                onPermissionPermanentlyDenied()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val perms = if (config.enableBackgroundUpdates) {
            locationPermissions + backgroundPermission
        } else {
            locationPermissions
        }
        if (requestCode == REQUEST_CODE_PERMISSION) {
            when {
                grantResults.isEmpty() ->
                    // If user interaction was interrupted, the permission request is cancelled and you
                    // receive empty arrays.
                    logDebug("User interaction was cancelled.")
                grantResults.all { it == PackageManager.PERMISSION_GRANTED } -> onPermissionGranted()
                perms.all { grantResults[permissions.indexOf(it)] == PackageManager.PERMISSION_GRANTED } -> onPermissionGranted()
                else -> onPermissionDenied()
            }
        }
    }

    private fun initPermissionModel() {
        logDebug("Initializing permission model")
        if (!permissions.all(::hasPermission)) {
            if (permissions.any(::shouldShowRationale)) {
                showPermissionRationale(this, ::requestForPermissions, ::onPermissionDenied)
            } else {
                //checking if the permission is asking first time
                if (permissions.any { pref.getBoolean(it, true) }) {
                    setPrefValueAsked()
                    requestForPermissions()
                } else {
                    showPermanentlyDeniedDialog(
                        this,
                        ::openSettings,
                        ::onPermissionPermanentlyDenied
                    )
                }
            }
        } else {
            onPermissionGranted()
        }
    }


    private fun requestForPermissions() =
        ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_PERMISSION)


    private fun openSettings() {
        val intent = Intent()
        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivityForResult(intent, REQUEST_CODE_SETTINGS_ACTIVITY)
    }

    private fun setPrefValueAsked() {
        with(pref.edit()) {
            permissions.forEach { permission -> putBoolean(permission, false) }
            commit()
        }
    }

    private fun onPermissionGranted() {
        if (config.shouldResolveRequest) {
            checkIfLocationSettingsAreEnabled()
        } else {
            shouldProceedForLocation()
        }
    }


    private fun resolveLocationSettings(exception: Exception) {
        val resolvable = exception as? ResolvableApiException ?: return
        try {
            resolvable.startResolutionForResult(this, REQUEST_CODE_LOCATION_SETTINGS)
        } catch (e1: IntentSender.SendIntentException) {
            e1.printStackTrace()
        }
    }

    private fun checkIfLocationSettingsAreEnabled() {
        checkSettings(
            ::shouldProceedForLocation,
            ::locationSettingsFailure,
            this,
            getLocationRequest(config)
        )
    }

    private fun locationSettingsFailure(exception: Exception) {
        if (exception is ApiException) {
            when (exception.statusCode) {
                LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                    logDebug("resolution  required")
                    onResolutionNeeded(
                        exception,
                        this,
                        lifecycle.currentState,
                        ::resolveLocationSettings,
                        ::onResolutionDenied
                    )
                }
                LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                    logDebug("SETTINGS_CHANGE_UNAVAILABLE")
                    shouldProceedForLocation()
                }
                else -> logDebug("something went wrong : $exception")
            }
        } else {
            logDebug("Location settings resolution denied")
            // resolution failed somehow
            onResolutionDenied()
        }
    }

    private fun clearPermissionNotificationIfAny() {
        val manager =
            getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        manager.cancel(Constants.LOCATION_NOTIFICATION_ID)
    }

    private fun onPermissionPermanentlyDenied() {
        postResult(Constants.PERMANENTLY_DENIED)
    }

    private fun shouldProceedForLocation() {
        clearPermissionNotificationIfAny()
        postResult(Constants.GRANTED)
    }

    private fun onResolutionDenied() {
        postResult(Constants.RESOLUTION_FAILED)
    }

    private fun onPermissionDenied() {
        postResult(Constants.DENIED)
    }

    private fun postResult(status: String) {
        permissionLiveData.postValue(status)
        isPermissionRequesting.set(false)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        isPermissionRequesting.set(false)
    }
}