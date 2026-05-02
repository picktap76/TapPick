package com.krisadan.tappick.util

import android.content.Context
import android.widget.Toast

/**
 * Helper to prevent Toast queuing.
 * Cancels the previous toast before showing a new one.
 */
object ToastHelper {
    private var currentToast: Toast? = null

    fun showToast(context: Context, message: String, duration: Int = Toast.LENGTH_SHORT) {
        currentToast?.cancel()
        currentToast = Toast.makeText(context.applicationContext, message, duration)
        currentToast?.show()
    }
}
