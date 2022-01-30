package com.ibm.airlytics.sessions

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.ibm.airlytics.environments.ALEnvironment
import com.ibm.airlytics.events.ALEvent
import com.ibm.airlytics.persistence.ALAndroidCache
import com.ibm.airlytics.persistence.Cache
import com.ibm.airlytics.utils.ALCrashTracker
import org.json.JSONObject
import java.lang.Exception
import java.lang.RuntimeException
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.logging.Logger
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

object SessionsManager {

    const val SESSION_PREFIX = "session"
    private const val SESSION_START = "$SESSION_PREFIX-start"
    private const val SESSION_END = "$SESSION_PREFIX-end"
    private const val SESSION_FOREGROUND_DURATION = "sessionForegroundDuration"
    private const val SESSION_DURATION = "sessionDuration"
    const val SESSION_EXPIRATION = "sessionExpiration"
    const val DEFAULT_EXPIRATION_SEC = 5
    private const val SCHEMA_VERSION = "2.0"

    private var sessionsMap: MutableMap<String, SessionDetails> =
        HashMap()//maps env name to its session (each env has single session)

    private var previousSessionsMap: MutableMap<String, SessionDetails.SessionId> =
        HashMap()//maps env name to its previous session id

    private var environmentsMap: MutableMap<String, ALEnvironment> = HashMap()

    private var sessionsCache: Cache? = null

    private var application: Application? = null

    private var appStatusForeground = true

    private var longestSessionExpirationInSeconds = 5

    private var log: Logger? = null

    private var observerStarted = false

    private var endSessionNotClosed = false

    private val observer: LifecycleObserver = (object : LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        fun onBackground() {
            log?.info("onBackground")
            appStatusForeground = false
            updateSessionsTimeDetails(null)
            sendEventsWhenGoingToBackground()
            triggerCloseSessionService()
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_START)
        fun onStart() {
            observerStarted = true
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
        fun onForeground() {
            log?.info("onForeground")
            appStatusForeground = true
            updateSessionsTimeDetails()
        }
    })

    init {
        try {
            addLifeCycleObserver()
        }catch (rte: RuntimeException){
            //On Unit tests this is valid
        }
    }

    fun triggerCloseSessionService() {
        application?.let {
            val serviceIntent = Intent(application, CloseSessionService::class.java)
            serviceIntent.putExtra(SESSION_EXPIRATION, longestSessionExpirationInSeconds)
            try {
                application?.startService(serviceIntent)
            } catch (ex: Throwable) {
                log?.info("startService exception ${ex.message}")
                //do nothing
            }
        }
    }

    fun forceCloseSession(envId: String) {
        val sessions: MutableList<SessionDetails> = ArrayList()
        val details = sessionsMap[envId]
        if (details != null) {
            //add to sessions list for iterating
            sessionsCache?.remove(details.environmentName)
            previousSessionsMap[details.environmentName] = details.sessionId
            sessionsMap.remove(details.environmentName)
            sessions.add(details)
            sendSessionEvents(SESSION_END, sessions)
        }
    }

    fun updateSessionsTimeDetailsFromService() {
        log?.info("updateSessionsTimeDetailsFromService")
        application?.let {
            if (sessionsCache == null) {
                sessionsCache = ALAndroidCache("Sessions", application?.applicationContext)
                sessionsCache?.let {
                    val cachedSessions = it.getAllValues()
                    if (cachedSessions.isNotEmpty()) {
                        loadFromCache(cachedSessions)
                    }
                }
            }
        }
        updateSessionsTimeDetails()
    }

    @Synchronized
    fun updateSessionsTimeDetails(envId: String? = null, debug: Boolean = false) {
        if (debug) {
            log = Logger.getLogger(SessionsManager::class.java.name)
        }

        val didAppStop = !appStatusForeground
        if (!didAppStop && !isAppActive()) {
            return
        }
        val terminatedSessions: MutableList<SessionDetails> = ArrayList()
        val sessions: MutableList<SessionDetails> = ArrayList()
        if (envId != null) {
            val details = sessionsMap[envId]
            if (details != null) {
                //add to sessions list for iterating
                sessions.add(details)
            }
        } else {
            //session is new
            if (sessionsMap.isEmpty()) {
                if (didAppStop) {
                    return
                } else {
                    //app came to foreground
                    for (environment in environmentsMap.keys) {
                        sessionsMap[environment] = SessionDetails(environment)
                    }
                    //sessions were established just now - send start session
                    if (sessionsMap.isNotEmpty()) {
                        sendStartSessionEvents(ArrayList(sessionsMap.values))
                    }
                }
            }
            sessions.addAll(sessionsMap.values)
        }

        for (sessionDetail in sessions) {
            val currentTime: Long = System.currentTimeMillis()
            if (didAppStop) {
                if (sessionDetail.latestPauseTime == 0L) {
                    sessionDetail.latestPauseTime = currentTime
                    log?.info("DEBUG: set latestPauseTime to $currentTime")
                    sessionsCache?.setValue(
                        sessionDetail.environmentName,
                        sessionDetail.toString()
                    )
                }
            }
            val environment = environmentsMap[sessionDetail.environmentName]
            if (environment != null) {
                //last session was not terminated yet and app restarted....
                if (sessionDetail.isPersistedData && !didAppStop) {
                    terminatedSessions.add(sessionDetail)
                } else if (sessionDetail.latestPauseTime > 0L) {

                    val lastPauseDuration = currentTime - sessionDetail.latestPauseTime
                    log?.info("DEBUG: lastPauseDuration  = $lastPauseDuration , ${environment.config.sessionExpirationInSeconds.get()}")

                    if (lastPauseDuration.toInt() > environment.config.sessionExpirationInSeconds.get() * 1000) {

                        sessionDetail.appWasPaused = didAppStop
                        terminatedSessions.add(sessionDetail)
                        if (didAppStop) {
                            sessionsCache?.remove(sessionDetail.environmentName)
                            previousSessionsMap[sessionDetail.environmentName] =
                                sessionDetail.sessionId
                            sessionsMap.remove(sessionDetail.environmentName)
                        }
                    } else {
                        if (!didAppStop) {
                            sessionDetail.backgroundDuration += lastPauseDuration
                            sessionDetail.latestPauseTime = 0L
                        }
                    }
                }
                sessionDetail.isPersistedData = false
            }
        }

        if (terminatedSessions.isNotEmpty()) {
            sendEndSessionEvents(terminatedSessions)
        }
    }

    private fun sendSessionEvents(name: String, sessions: List<SessionDetails>) {
        var eventTime = System.currentTimeMillis()
        for (sessionDetails in sessions) {
            val environment = environmentsMap[sessionDetails.environmentName]
            val attributes: MutableMap<String, Any?> = HashMap()
            if (name == SESSION_END) {
                if (!prepareEndSessionEvent(eventTime, sessionDetails, attributes, environment)){
                    environmentsMap[sessionDetails.environmentName]?.clearSessionEvents(sessionDetails.sessionId.id)
                    return
                }
            } else {
                prepareStartSessionEvent(eventTime, sessionDetails, attributes)
            }
//            val environment = environmentsMap[sessionDetails.environmentName]
            if (environment != null) {
                val event = ALEvent(
                    name,
                    UUID.randomUUID(),
                    environment,
                    eventTime,
                    SCHEMA_VERSION,
                    sessionDetails.appWasPaused,
                    attributes
                )
                environmentsMap[sessionDetails.environmentName]?.track(event)
            }
        }
    }

    private fun prepareStartSessionEvent(eventTime: Long, sessionDetails: SessionDetails, attributes: MutableMap<String, Any?>) {
                sessionDetails.sessionId.startTime = eventTime
                sessionsCache?.setValue(sessionDetails.environmentName, sessionDetails.toString())
                val environment = environmentsMap[sessionDetails.environmentName]
                environment?.resendUserAttributes()
    }

    private fun prepareEndSessionEvent(
        eventTime: Long,
        sessionDetails: SessionDetails,
        attributes: MutableMap<String, Any?>,
        environment: ALEnvironment?
    ): Boolean {
        if (sessionDetails.latestSessionActivity == null && environment?.config?.shouldSendEmptySessions != true){
            return false
        }
        var sessionEndTime = eventTime
        if (endSessionNotClosed){
            log?.warning("DEBUG: endSessionNotClosed")
            ALCrashTracker.getLastSeen()?.let {
                if (sessionDetails.latestPauseTime == 0L) {
                    sessionDetails.latestPauseTime = it
                    log?.warning("DEBUG: endSessionNotClosed setting latestPauseTime to : $it")
                }
            }
        }
        if (sessionDetails.latestPauseTime > 0) {
            log?.info("DEBUG: sessionDetails.latestPauseTime -   ${sessionDetails.latestPauseTime}")
            sessionEndTime = sessionDetails.latestPauseTime
        }
        var duration = sessionEndTime - sessionDetails.sessionId.startTime
        if (duration > TimeUnit.MINUTES.toMillis(30)){
            sessionDetails.latestSessionActivity?.let{
                sessionEndTime = it
                duration = sessionEndTime - sessionDetails.sessionId.startTime
            }
        }
        attributes[SESSION_FOREGROUND_DURATION] =
            duration - sessionDetails.backgroundDuration
        attributes[SESSION_DURATION] = duration
        sessionsCache?.remove(sessionDetails.environmentName)
        log?.let {
            sessionDetails.duration = duration
            it.info("DEBUG: terminatedSession  = $sessionDetails ")
        }
        if (endSessionNotClosed) {
            endSessionNotClosed = false
        }
        return true
    }

    private fun sendEndSessionEvents(sessions: List<SessionDetails>) {
        if (appStatusForeground && sessions.isNotEmpty()) {
            endSessionNotClosed = true
        }
        sendSessionEvents(SESSION_END, sessions)
        val startSessions: MutableList<SessionDetails> = ArrayList()
        if (appStatusForeground) {
            for (sessionItem in sessions) {
                sessionItem.clearSessionAttributes()
                startSessions.add(sessionItem)
            }
        }
        if (startSessions.isNotEmpty() /*&& appEvent != AppEvent.Closing*/) {
            sendStartSessionEvents(startSessions)
        }
    }

    private fun sendStartSessionEvents(sessions: List<SessionDetails>?) {
        if (appStatusForeground && isAppActive()) {
            sessions?.let {
                sendSessionEvents(SESSION_START, sessions)
                log?.info("DEBUG: session started ")
            }
        }
    }

    /**
     * returns the current session id on app
     */
    fun getSessionId(envName: String): UUID? {
        var id = sessionsMap[envName]?.sessionId?.id
        if (id == null) {
            id = previousSessionsMap[envName]?.id
        }
        return id
    }

    /**
     * returns the current session id on app
     */
    fun getSessionStartTime(envName: String): Long? {
        return sessionsMap[envName]?.sessionId?.startTime
    }

    /**
     * Method that adds an environment to the session map
     */
    fun addEnvironment(
        env: ALEnvironment,
        app: Context?,
        crashDetails: ALCrashTracker.CrashDetails?
    ) {
        if (app != null) {
            if (application == null) {
                application = app as Application
                sessionsCache = ALAndroidCache("Sessions", app)
                sessionsCache?.let {
                    val cachedSessions = it.getAllValues()
                    if (cachedSessions.isNotEmpty()) {
                        loadFromCache(cachedSessions)
                    }
                }

            }
        }
        environmentsMap[env.config.name] = env

        if (env.config.sessionExpirationInSeconds.get() > longestSessionExpirationInSeconds) {
            longestSessionExpirationInSeconds = env.config.sessionExpirationInSeconds.get()
        }
        var sessionDetails = sessionsMap[env.config.name]
        if (crashDetails != null) {
            if (true == crashDetails.isCrash) {
                sessionDetails?.let {
                    env.trackCrash(crashDetails, it.sessionId)
                }
                sessionDetails = null
            }
        }
        if (appStatusForeground) {
            if (sessionDetails != null) {
                //this is a scenario where app was stopped - the last session-end event was not sent...
                updateSessionsTimeDetails(sessionDetails.environmentName)
            } else {
                sessionsMap[env.config.name] =
                    SessionDetails(env.config.name)
                val sessionEnvDetails = sessionsMap[env.config.name]
                sessionEnvDetails?.let {
                    sendStartSessionEvents(listOf(it))
                }
            }
        }
    }

    private fun isAppActive(): Boolean {
        val activityManager =
            application?.applicationContext?.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager?
        if (activityManager?.appTasks == null || activityManager.appTasks.isEmpty()) {
            return false
        }
        return true
    }

    private fun addLifeCycleObserver() {
        val lifecycleRunner = Runnable {
            ProcessLifecycleOwner.get().lifecycle.addObserver(observer)
        }
        Handler(Looper.getMainLooper()).post(
            lifecycleRunner
        )
    }

    private fun loadFromCache(cachedSessions: Map<String, String>) {
        for (sessionPair in cachedSessions) {
            try {
                val sessionDetails = SessionDetails(
                    JSONObject(sessionPair.value)
                )
                sessionDetails.isPersistedData = true
                sessionsMap[sessionPair.key] = sessionDetails
            } catch (ex: Exception) {
                log?.warning("Failed loading session from cache (${ex.message}) : ${sessionPair.key}-${sessionPair.value} ")
            }
        }
    }

    @Synchronized
    fun updateLatestSessionActivity(envName: String) {
        sessionsMap[envName]?.let {
            it.latestSessionActivity = System.currentTimeMillis()
            sessionsCache?.setValue(it.environmentName, it.toString())
        }
    }

    fun getLatestSessionActivity(envName: String): Long? {
        return sessionsMap[envName]?.latestSessionActivity
    }

    fun shouldSendEvent(envName: String): Boolean {
        if (getLatestSessionActivity(envName) != null){
            return true
        }
        if (environmentsMap[envName]?.config?.shouldSendEmptySessions == true){
            return true
        }
        return false
    }

    fun sendEventsWhenGoingToBackground() {
        environmentsMap.forEach {
            it.value.sendEventsWhenGoingToBackground()
        }

    }

    /**
     * Method that return the environment from the lifecycle management
     */
    fun getEnvironment(name: String): ALEnvironment? {
        return environmentsMap[name]
    }

    /**
     * Method that return the environment from the lifecycle management
     */
    fun getEnvironments(): Collection<ALEnvironment> {
        return environmentsMap.values
    }

    fun updateUserId(userId: String) {

        //send close session
        sendSessionEvents(SESSION_END, sessionsMap.values.toList())

        for (environment in environmentsMap.values) {
            environment.userId = userId
        }
        //send start session
        sendSessionEvents(SESSION_START, sessionsMap.values.toList())
    }

    //this method is for case that app was started and immediately stopped while observer is starting (and occupying main thread)
    fun verifyStarted() {
        if (!observerStarted) {
            TimeUnit.SECONDS.sleep(1)
        }
    }
}