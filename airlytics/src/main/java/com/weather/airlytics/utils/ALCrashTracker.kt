package com.weather.airlytics.utils

import android.content.Context
import android.content.SharedPreferences
import java.util.*
import kotlin.concurrent.schedule

/**
 * Class for tracking scenario of crash
 * This class triggers a timer that writes to preferences once a while
 * When app terminates gracefully the preference value is deleted prior to shut down
 * On crash scenario the preference value will not be deleted and a crash will be identified and sent
 */
object ALCrashTracker {
    private var prefs: SharedPreferences? = null
    private const val preferenceName = "ALCrashTracker"
    private const val fieldNameLastSeen = "lastSeen"
    private const val fieldNameIsCrash = "isCrash"
    private var timer: Timer? = null

    data class CrashDetails(var lastSeen: Long?, var isCrash: Boolean?)

    private const val period: Long = 1000
    fun initTrackerAndGetLastSeenTime(context: Context?): CrashDetails {
        var lastSeen: Long? = null
        var isLastWasCrash: Boolean? = null

        if (context != null) {
            prefs = context.getSharedPreferences(
                preferenceName, Context.MODE_PRIVATE
            )
            timer = Timer()
            lastSeen = getLastSeen()
            isLastWasCrash =
                getIsLastWasCrash()

            writeIsCrashValue(false)
            timer?.schedule(
                0,
                period
            ) {
                prefs?.edit()?.putLong(
                    fieldNameLastSeen, Date().time
                )?.apply()
            }
        }


        return CrashDetails(
            lastSeen,
            isLastWasCrash
        )
    }

    fun getLastSeen(): Long? {
        return prefs?.getLong(
            fieldNameLastSeen, -1
        )
    }

    private fun getIsLastWasCrash(): Boolean? {
        return prefs?.getBoolean(
            fieldNameIsCrash, false
        )
    }

    fun writeIsCrashValue(isCrash: Boolean) {
        prefs?.edit()?.putBoolean(
            fieldNameIsCrash, isCrash
        )?.apply()
    }
}