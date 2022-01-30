package com.ibm.airlytics.providers

import android.content.Context
import com.ibm.airlytics.events.ALEvent
import com.ibm.airlytics.net.NetworkClientUtils
import com.ibm.airlytics.persistence.ALAndroidCache
import com.ibm.airlytics.providers.data.ALProviderConfig
import com.ibm.airlytics.providers.data.ALProviderTrackingPolicyConfig.Companion.FAST
import com.ibm.airlytics.providers.data.ALProviderTrackingPolicyConfig.Companion.MEDIUM
import com.ibm.airlytics.providers.data.ALProviderTrackingPolicyConfig.Companion.SLOW
import com.ibm.airlytics.providers.data.ALProviderTrackingPolicyConfig.Companion.UNLIMITED
import com.ibm.airlytics.sessions.SessionsManager
import com.ibm.airlytics.utils.DeviceConnectionUtils
import com.ibm.airlytics.utils.RestFailuresAlerter
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.StringWriter
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.logging.Logger
import kotlin.collections.ArrayList
import kotlin.collections.HashSet
import kotlin.concurrent.schedule

/**
 * A Provider that send events to rest service based on providerConfig
 * should be init with context in order to use cache of device
 * The provider loaded in reflection this is why we suppress unused
 */
@Suppress("unused")
class RestEventProxyProvider(
        private var providerConfig: ALProviderConfig,
        private val tags: Set<String>
) : ALProvider {
    companion object {
        private const val defaultTimeOutForClient: Long = 15
        private const val defaultInterval: Long = 10
        private var lastSendFailed: Boolean = false

        var executor: ExecutorService = Executors.newSingleThreadExecutor()

        val log: Logger = Logger.getLogger(RestEventProxyProvider::class.java.name)

    }

    //Lock when changing timer
    private val timerChangeLock: Any = Object()

    private var client: OkHttpClient? = NetworkClientUtils.createClient(
            getConnectTimeOut(),
            getWriteTimeout(),
            getReadTimeout(),
            providerConfig.isDebugUser
    )

    private var timer: Timer? = null

    private var alEventsCache: ALAndroidCache? = null
    private var curIntervalInSec: Long = defaultInterval
    private fun getConnectTimeOut(): Long {
        return providerConfig.additionalInfo?.optLong(
                "connectTimeout",
                defaultTimeOutForClient
        ) ?: defaultTimeOutForClient
    }

    private fun getWriteTimeout(): Long {
        return providerConfig.additionalInfo?.optLong(
                "writeTimeout",
                defaultTimeOutForClient
        ) ?: defaultTimeOutForClient
    }

    private fun getReadTimeout(): Long {
        return providerConfig.additionalInfo?.optLong(
                "readTimeout",
                defaultTimeOutForClient
        ) ?: defaultTimeOutForClient
    }

    override fun init(context: Context?) {
        if (context != null) {
            alEventsCache = ALAndroidCache(
                    providerConfig.environmentId, context
            )
        }
        initEventsSendScheduler(context)
    }

    private fun getInterval(valInterval: String): Long {
        val config = providerConfig.trackingPolicyConfigs
        if (config != null) {
            val interval = config.intervals[valInterval]
            if (interval != null) {
                return interval.toLong()
            }
        }
        return defaultInterval
    }

//    private fun getSendEventsPeriodBasedOnConnection(context: Context?): Long {
//        if (context != null) {
//            if (DeviceConnectionUtils.isConnectedWifi(context)) {
//                return getInterval(UNLIMITED)
//            }
//            when (DeviceConnectionUtils.getNetworkClass(context)) {
//                "2G" -> return getInterval(SLOW)
//                "3G" -> return getInterval(MEDIUM)
//                "4G" -> return getInterval(FAST)
//            }
//        }
//        return defaultInterval
//    }

    private fun getSendEventsPeriodBasedOnConnection(context: Context?): Long {
        if (context != null) {
            when (DeviceConnectionUtils.getNetworkClassNew(context)) {
                "WIFI" -> return getInterval(UNLIMITED)
                "2G" -> return getInterval(SLOW)
                "3G" -> return getInterval(MEDIUM)
                "4G" -> return getInterval(FAST)
                "5G" -> return getInterval(FAST)
            }
        }
        return defaultInterval
    }

    /**
     * init timer object and start schedule
     */
    private fun initEventsSendScheduler(context: Context?) {
        curIntervalInSec = getSendEventsPeriodBasedOnConnection(context)
        resetEventSendScheduler()
    }

    /**
     * cancel old timer if there is and schedule send events from cache
     */
    private fun resetEventSendScheduler() {
        //validate that last finish but with timeout
        synchronized(timerChangeLock) {
            if (timer != null) {
                timer?.cancel()
                timer?.purge()
            }

            timer = Timer()

            //convert sec to milliseconds
            val intervalInMilliseconds = curIntervalInSec * 1000
            timer?.schedule(0, intervalInMilliseconds) {
                sendAllCachedEvents()
            }
        }
    }

    private val restSendLock: Any = Object()

    /**
     * send all cached events to target
     */
    private fun sendAllCachedEvents() {
        executor.submit {
            alEventsCache?.let {
                val eventsMap = it.getAllValues()
                val eventsToSend: MutableMap<String, String> = HashMap()
                if (eventsMap.isNotEmpty()) {
                    try {
                        for (cachedEvent in eventsMap) {
                            if (SessionsManager.shouldSendEvent(providerConfig.environmentId)){
                                eventsToSend[cachedEvent.key] = cachedEvent.value
                            }
                        }
                    } catch (ex: Throwable) {
                        log.warning("Exception thrown while preventing empty sessions from being sent")
                    }
                    if (eventsToSend.isNotEmpty()) {
                        send(eventsToSend.values)
                    }
                }
            }
        }
    }

    private fun sortAndMergeEvents(events: List<ALEvent>): List<ALEvent> {
        /*
        1. sort out old events
        2. sort events according to datetime
        3. merge user-attribute, stream-results events
         */
        val listOfEventsToSend: MutableList<ALEvent> = ArrayList()
        val currentTime = Date().time
        val failedEventsInMilliSec = providerConfig.failedEventsExpirationInSeconds * 1000
        events.sortedBy {
            it.eventTime
        }
        events.forEach {
            if (currentTime - it.eventTime > failedEventsInMilliSec) {
                alEventsCache?.remove(it.id.toString())
            } else {
                listOfEventsToSend.add(it)
            }
        }
        return listOfEventsToSend
    }

    /**
     * remove from cache all the successful event or event that was error but no need to resend
     */
    private fun removeCompletedEventsFromCache(
            events: List<ALEvent>,
            responseCode: Int,
            responseBody: String
    ) {
        var listOfEventsToRemoveFromCache: MutableList<ALEvent> = ArrayList()

        if (responseCode == 200 || responseCode == 202) {
            listOfEventsToRemoveFromCache = events.toMutableList()
            //if all successful do nothing - all events should be remove
            if (responseCode == 202) {
                val eventsMessage = StringWriter()
                val errorEvents: MutableSet<String> = HashSet()
                //if some returned with errors delete (from the list of those who should be deleted )only does the sever say that we need to re-send
                val arrEvents = JSONArray(responseBody)
                val mapIdsToEvents =
                        events.filter { it.id != null }.map { it.id.toString() to it }.toMap()
                for (i in 0 until arrEvents.length()) {
                    val curEvent = arrEvents.getJSONObject(i)
                    val eventId = curEvent.optString("eventId")
                    val shouldRetry = curEvent.optBoolean("shouldRetry")
                    if (shouldRetry) {
                        if (mapIdsToEvents.containsKey(eventId)) {
                            listOfEventsToRemoveFromCache.remove(mapIdsToEvents[eventId])
                        } else {
                            mapIdsToEvents[eventId]?.let {
                                errorEvents.add(it.name)
                            }
                            eventsMessage.append(mapIdsToEvents[eventId]?.name).append(" with attributes").append(mapIdsToEvents[eventId]?.attributes?.keys.toString())
                        }
                    }
                }
                for (eventName in errorEvents) {
                    eventsMessage.append(eventName)
                }
                RestFailuresAlerter.send("Sending $eventsMessage events to airlytics proxy returned with response code : $responseCode check events!")
                log.warning("Sending $eventsMessage events to airlytics proxy returned with response code : $responseCode check events!")
            }
        } else {
            val eventsMessage = StringWriter()
            val errorEvents: MutableSet<String> = HashSet()
            for (event in events) {
                errorEvents.add(event.name)
            }
            for (eventName in errorEvents) {
                eventsMessage.append(eventName)
            }
            RestFailuresAlerter.send("Sending $eventsMessage events to airlytics proxy returned with response code : $responseCode check events!")
            log.warning("Sending $eventsMessage events to airlytics proxy returned with response code : $responseCode check events!")

        }

        //remove all cached item that needed (those with errors that no need to re send or succeeded)
        listOfEventsToRemoveFromCache.forEach {
            alEventsCache?.remove(it.id.toString())
        }
    }

    /**
     * send event to rest service and decide if to add to cache
     */
    private fun send(
            cachedEvents: Collection<String>
    ) {
        if (cachedEvents.isEmpty()) {
            return
        }

        val events: MutableList<ALEvent> = ArrayList()

        //validate there are events
        for (cachedEvent in cachedEvents) {
            events.add(ALEvent(JSONObject(cachedEvent)))
        }
        if (events.isEmpty()) {
            return
        }
        //first filter out old failing events - should not be tested
        val filteredEvents = sortAndMergeEvents(events)

        if (filteredEvents.isEmpty()) {
            return
        }
        val connection = providerConfig.connection as? JSONObject
        if (connection != null) {
            val url = connection["url"] as? String ?: return

            val apiKeys = connection["apiKeys"] as? JSONObject
            if (apiKeys == null || apiKeys.length() == 0) {
                return
            }
            val tag = tags.elementAt(0)
            var apiKey = apiKeys[tag] as? String

            if (apiKey == null || apiKey.isEmpty()) {
                apiKey = apiKeys.optString(apiKeys.keys().next())
            }

            val eventsJson = JSONObject()
            val eventsArray = JSONArray()
            filteredEvents.forEach {
                eventsArray.put(it.toJSONForSend())
            }
            eventsJson.put("events", eventsArray)
//            log.warning("log requests:" + eventsJson.toString()) good for debugging - do not delete
            val request = Request.Builder()
                    .url(url)
                    .addHeader("x-api-key", apiKey)
                    .addHeader("Content-Encoding", "gzip, deflate")
                    .addHeader("x-current-device-time", System.currentTimeMillis().toString())
                    .post(
                            RequestBody.create(
                                    MediaType.parse("application/json"),
                                    eventsJson.toString()
                            )
                    )
                    .build()
            try {
                client?.newCall(request)?.execute().use { response ->
                    if (response != null) {
                        //remove from cache if response succeed
                        response.body()?.let {
                            removeCompletedEventsFromCache(
                                filteredEvents,
                                response.code(),
                                it.string()
                            )
                        }
                        response.body()?.close()
                    }
                    if (lastSendFailed) {
                        lastSendFailed = false
                        RestFailuresAlerter.resetShowedCounter()
                    }
                }
            } catch (throwable: Throwable) {
                RestFailuresAlerter.send("Sending events to airlytics proxy failed: ${throwable.message}")
                log.warning("Sending events to airlytics proxy failed: ${throwable.message}")
                lastSendFailed = true
            }
        }
    }

    /**
     * here we not immediately send the event to rest service
     * we add it to queue that sent periodically
     * if this is a real time event we will try to schedule now the timer and cancel the old one
     * if it is already in the middle of running it will not stop
     */
    override fun send(event: ALEvent): Boolean {
        //check if should send event or not
        alEventsCache?.setValue(event.id.toString(), event.toString())
        val isRealTimeEvent = this.providerConfig.eventConfigs?.get(event.name)?.realtime
        if (isRealTimeEvent != null && isRealTimeEvent) {
            sendAllCachedEvents()
        }
        return true
    }

    override fun reset() {
        synchronized(timerChangeLock) {
            timer?.cancel()
            timer?.purge()
            timer = null
        }
    }

    override fun interval(config: ALProviderConfig) {
        synchronized(providerConfig) {
            providerConfig = config
        }
        resetEventSendScheduler()
    }

    /**
     * this method is called when going to background
     */
    override fun sendEventsWhenGoingToBackground() {
        if (true == providerConfig.trackingPolicyConfigs?.sendEventsWhenGoingToBackground) {
            sendAllCachedEvents()
        }
    }

    override fun clearSessionEvents(sessionUUID: UUID) {
        alEventsCache?.let {
            val eventsMap = it.getAllValues()
            val eventsToDelete : ArrayList<String> = ArrayList()
            if (eventsMap.isNotEmpty()) {
                try {
                    for (cachedEvent in eventsMap) {
                        val event = JSONObject(cachedEvent.value)
                        val sessionId = event.optString("sessionId")
                        if (sessionId == sessionUUID.toString() ){
                            eventsToDelete.add(cachedEvent.key)
                        }
                    }
                } catch (ex: Throwable) {
                    log.warning("Exception thrown while preventing empty sessions from being sent")
                }

                for (key in eventsToDelete) {
                    alEventsCache?.remove(key)
                }
            }
        }
    }
}