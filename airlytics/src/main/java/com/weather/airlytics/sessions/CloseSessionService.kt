package com.weather.airlytics.sessions

import android.app.IntentService
import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.Looper

class CloseSessionService : IntentService(CloseSessionService::class.java.name) {

    override fun onHandleIntent(intent: Intent?) {
        var timeToWait: Int = SessionsManager.DEFAULT_EXPIRATION_SEC
        if (intent != null) {
            timeToWait = intent.getIntExtra(
                SessionsManager.SESSION_EXPIRATION,
                SessionsManager.DEFAULT_EXPIRATION_SEC
            )
        }
        sendEndEventSession(timeToWait)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return Service.START_STICKY
    }

    private fun sendEndEventSession(timeToWait: Int) {
        val runCloseSession = Runnable {
            SessionsManager.updateSessionsTimeDetailsFromService()
            SessionsManager.sendEventsWhenGoingToBackground()
        }
        Handler(Looper.getMainLooper()).postDelayed(runCloseSession, timeToWait.toLong()*1000)
    }
}