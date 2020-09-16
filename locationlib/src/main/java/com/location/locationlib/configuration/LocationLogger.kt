package com.location.locationlib.configuration

import android.util.Log


internal var isLoggingEnabled: Boolean = true


internal inline fun <reified T : Any> T.logError(log: String) {
    if (isLoggingEnabled) {
        Log.e(this::class.java.simpleName, log)
    }
}

internal inline fun <reified T : Any> T.logError(throwable: Throwable) {
    if (isLoggingEnabled) {
        Log.e(this::class.java.simpleName, throwable.message.toString())
    }
}

internal inline fun <reified T : Any> T.logDebug(log: String) {
    if (isLoggingEnabled) {
        Log.d(this::class.java.simpleName, log)
    }
}