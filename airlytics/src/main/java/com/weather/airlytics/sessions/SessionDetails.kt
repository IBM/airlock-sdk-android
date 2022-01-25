package com.weather.airlytics.sessions

import org.json.JSONObject
import java.util.*

/**
 *  holds the session data
 */
class SessionDetails {

    constructor(environment: String) {
        this.environmentName = environment
    }

    constructor(jsonObject: JSONObject) {
        sessionId = SessionId(
            UUID.fromString(jsonObject.optString("id")),
            jsonObject.optLong("startTime", 0L)
        )
        backgroundDuration = jsonObject.optLong("backgroundDuration", 0L)
        duration = jsonObject.optLong("duration", 0L)
        latestPauseTime = jsonObject.optLong("latestPauseTime", 0L)
        environmentName = jsonObject.optString("environmentName")
        appWasPaused = jsonObject.optBoolean("sessionWasStopped")
        val latestSessionActivityVal = jsonObject.optLong("latestSessionActivity")
        if (latestSessionActivityVal > 0){
            latestSessionActivity = latestSessionActivityVal
        }
    }


    var sessionId = SessionId()
    var backgroundDuration: Long = 0L
    var duration: Long = 0L
    var latestPauseTime: Long = 0L
    var environmentName: String
    var appWasPaused: Boolean = false
    var isPersistedData: Boolean = false
    var latestSessionActivity: Long? = null

    /**
     * Clears session attributes
     * Because there is only one session on app the data can be cleared out and reused for next session
     */
    fun clearSessionAttributes() {
        sessionId = SessionId()
        duration = 0L
        latestPauseTime = 0L
        backgroundDuration = 0L
        appWasPaused = false
        isPersistedData = false
        latestSessionActivity = null
    }

    override fun toString(): String {
        return toJSON().toString()
    }

    private fun toJSON(): JSONObject {
        val jsonValues = JSONObject()
        jsonValues.put("id", sessionId.id.toString())
        jsonValues.put("startTime", sessionId.startTime)
        jsonValues.put("backgroundDuration", backgroundDuration)
        jsonValues.put("duration", duration)
        jsonValues.put("latestPauseTime", latestPauseTime)
        jsonValues.put("environmentName", environmentName)
        jsonValues.put("sessionWasStopped", appWasPaused)
        jsonValues.put("latestSessionActivity", latestSessionActivity)
        return jsonValues
    }

    class SessionId {
        constructor(id: UUID, startTime: Long) {
            this.id = id
            this.startTime = startTime
        }

        constructor() {
            this.id = UUID.randomUUID()
            this.startTime = System.currentTimeMillis()
        }

        var id: UUID
        var startTime: Long
    }
}