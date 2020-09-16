package com.location.locationlib.location

import android.location.Location

class ResultData private constructor(
    val location: Location? = null,
    val error: Throwable? = null
) {
    companion object {
        internal fun error(error: Throwable) = ResultData(error = error)
        internal fun success(location: Location) = ResultData(location = location)
    }
}