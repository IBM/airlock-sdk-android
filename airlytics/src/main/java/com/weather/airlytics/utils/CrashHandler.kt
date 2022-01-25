package com.weather.airlytics.utils

/**
 * @author Denis Voloshin
 */
import com.weather.airlytics.AL

/**
 * An uncaught exception handler that tracks an App Crash event then fires the chained handler.
 *
 * @author denis voloshin
 */
class CrashHandler
/**
 * Create an instance with a chained handler.
 *
 * @param chain chained exception handler
 */
    (private val chain: Thread.UncaughtExceptionHandler) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, ex: Throwable) {
        AL.notifyCrashHandler()
        chain.uncaughtException(thread, ex)
    }
}