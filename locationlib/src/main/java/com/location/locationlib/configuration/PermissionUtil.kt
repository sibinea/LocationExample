package com.location.locationlib.configuration

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.location.locationlib.R
import com.location.locationlib.textConfig


internal fun Context.hasPermission(permission: String) =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED


internal fun Activity.shouldShowRationale(permission: String) =
    ActivityCompat.shouldShowRequestPermissionRationale(this, permission)


internal val locationPermissions: Array<String> by lazy {
    arrayOf(
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION
    )
}


internal val backgroundPermission: Array<String>
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        arrayOf(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    } else arrayOf()


internal fun getAllPermissions(isBackground: Boolean) =
    if (isBackground) locationPermissions + backgroundPermission else locationPermissions


internal fun showPermissionRationale(
    activity: Activity,
    requestForPermissions: () -> Unit,
    onPermissionDenied: () -> Unit
) {
    AlertDialog.Builder(activity)
        .setTitle(textConfig.rationaleTitle)
        .setMessage(textConfig.rationaleText)
        .setPositiveButton(R.string.grant) { dialog, _ ->
            requestForPermissions()
            dialog.dismiss()
        }
        .setNegativeButton(R.string.deny) { dialog, _ ->
            dialog.dismiss()
            onPermissionDenied()
        }
        .setCancelable(false)
        .create()
        .takeIf { !activity.isFinishing }?.show()
}

internal fun showPermanentlyDeniedDialog(
    activity: Activity,
    openSettings: () -> Unit,
    onPermissionPermanentlyDenied: () -> Unit
) {
    AlertDialog.Builder(activity)
        .setTitle(textConfig.blockedTitle)
        .setMessage(textConfig.blockedText)
        .setPositiveButton(R.string.open_settings) { dialog, _ ->
            openSettings()
            dialog.dismiss()
        }
        .setNegativeButton(R.string.cancel) { dialog, _ ->
            dialog.dismiss()
            onPermissionPermanentlyDenied()
        }
        .setCancelable(false)
        .create()
        .takeIf { !activity.isFinishing }?.show()
}

internal fun onResolutionNeeded(
    exception: Exception, activity: Activity, currentState: Lifecycle.State,
    resolveLocationSettings: (Exception) -> Unit,
    onResolutionDenied: () -> Unit
) {
    exception.printStackTrace()
    if (!currentState.isAtLeast(Lifecycle.State.RESUMED)) return
    AlertDialog.Builder(activity)
        .setTitle(textConfig.locationResolutionTitle)
        .setMessage(textConfig.locationResolutionText)
        .setPositiveButton(R.string.enable) { dialog, _ ->
            resolveLocationSettings(exception)
            dialog.dismiss()
        }
        .setNegativeButton(R.string.cancel) { dialog, _ ->
            dialog.dismiss()
            onResolutionDenied()
        }
        .setCancelable(false)
        .create()
        .show()
}


internal fun checkSettings(
    success: () -> Unit,
    failure: (Exception) -> Unit,
    activity: Activity,
    locationRequest: LocationRequest
) {
    val builder = LocationSettingsRequest.Builder()
    builder.addLocationRequest(locationRequest)
    builder.setAlwaysShow(true)
    val client = LocationServices.getSettingsClient(activity)
    val locationSettingsResponseTask = client.checkLocationSettings(builder.build())
    locationSettingsResponseTask.addOnSuccessListener {
        success()
    }.addOnFailureListener { exception ->
        failure(exception)
    }
}


