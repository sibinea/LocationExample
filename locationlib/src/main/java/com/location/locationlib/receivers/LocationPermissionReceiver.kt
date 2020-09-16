package com.location.locationlib.receivers

import androidx.lifecycle.Observer
import com.location.locationlib.configuration.Constants
import com.location.locationlib.isPermissionRequesting
import com.location.locationlib.permissionLiveData

class LocationPermissionReceiver(private val onResult: (Throwable?) -> Unit) : Observer<String> {

    override fun onChanged(status: String?) {
        status ?: return
        isPermissionRequesting.set(false)
        when (status) {
            Constants.GRANTED -> {
                onResult(null)
            }
            else -> {
                onResult(Throwable(status))
            }
        }
        permissionLiveData.removeObserver(this)
        permissionLiveData.postValue(null)
    }

}