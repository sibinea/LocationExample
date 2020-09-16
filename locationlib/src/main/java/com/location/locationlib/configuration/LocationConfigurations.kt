package com.location.locationlib.configuration

import android.os.Parcelable
import com.google.android.gms.location.LocationRequest
import kotlinx.android.parcel.Parcelize

@Parcelize
data class LocationConfigurations(
    var shouldResolveRequest: Boolean = true,
    var enableBackgroundUpdates: Boolean = false,
    var locationUpdateIntervalInMS: Long = 1000L,
    var fastestLocationIntervalInMS: Long = 1000L,
    var maxWaitTimeInMS: Long = 1000L,
    var locationAccuracy: Int = LocationRequest.PRIORITY_HIGH_ACCURACY
) : Parcelable

internal fun getLocationRequest(config: LocationConfigurations): LocationRequest {
    return LocationRequest().apply {
        priority = config.locationAccuracy
        interval = config.locationUpdateIntervalInMS
        fastestInterval = config.fastestLocationIntervalInMS
        maxWaitTime = config.maxWaitTimeInMS
    }
}