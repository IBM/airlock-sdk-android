package com.ibm.airlytics.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast


/**
 * Shows an alert message about Rest failures on a Toast
 * This Alerter is for debug and intended to be active for dev users
 */
object RestFailuresAlerter {

    private var context: Context? = null
    private var maxAlertToShow = 3
    private var showedCounter = 0
    fun init(context: Context?, timesToShow: Int = 3) {
        this.context = context
        maxAlertToShow = timesToShow
    }

    fun resetShowedCounter() {
        showedCounter = 0
    }

    fun send(message: String): Boolean {
        if (context == null) {
            return true
        }

        if (showedCounter > maxAlertToShow && maxAlertToShow > 0) {
            return true
        }

        showedCounter++

        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
        return true
    }
}