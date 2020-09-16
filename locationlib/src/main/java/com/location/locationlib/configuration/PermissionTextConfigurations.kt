package com.location.locationlib.configuration

import android.os.Parcelable
import com.google.android.gms.location.LocationRequest
import kotlinx.android.parcel.Parcelize

@Parcelize
data class PermissionTextConfigurations(
    var rationaleTitle: String = "Location permission required!",
    var rationaleText: String =
        "Location permission is required in order to use this feature. Please grant the permission.",
    var blockedTitle: String = "Location Permission Blocked",
    var blockedText: String =
        "Location permission is blocked. Please allow location permission to use this feature",
    var locationResolutionTitle: String = "Location is currently disabled",
    var locationResolutionText: String =
        "Please enable access to device location to proceed further."
) : Parcelable

