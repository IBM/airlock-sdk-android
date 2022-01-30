package com.ibm.airlytics.providers

import android.content.Context
import android.content.SharedPreferences
import com.ibm.airlytics.events.ALEvent
import com.ibm.airlytics.providers.data.ALProviderConfig
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger


/**
 * A Provider that send events sent to log
 * The logged events could be seen on log file and will be used on debug screen
 */
class EventLogProvider(private var providerConfig: ALProviderConfig) : ALProvider {
    companion object{
        val log: Logger = Logger.getLogger(EventLogProvider::class.java.name)
        val maxEventsSaved = 200

    }
    private var prefs: SharedPreferences? = null
    private var mapValues: MutableMap<String, ALEvent> = ConcurrentHashMap()
    var context : Context? = null
    override fun init(context: Context?) {
        this.context = context
        prefs = context?.getSharedPreferences("ALLoggerCache_${providerConfig.environmentId}", Context.MODE_PRIVATE)
        loadFromPrefToMemory()
    }

    override fun send(event: ALEvent): Boolean {
        if (providerConfig.isDebugUser){
            log.info(event.toString())
        }
        setValue(event)
        return true
    }

    /**
     * set value for event
     */
    fun setValue(event: ALEvent) {
        val key = event.id?.toString()
        if (key != null) {
            mapValues[key] = event
            val value: String? = event.toString()
            value.let {
                prefs?.edit()?.putString(key, value)?.apply()
            }
        }
        if (mapValues.size > (maxEventsSaved * 1.5)){
            removeOldEntries()
        }
    }

    private fun removeOldEntries() {
        val eventLogs =  mapValues.values.toList()
        eventLogs.sortedBy { it.eventTime }

        for (n in maxEventsSaved until eventLogs.size){
            val key = eventLogs[n].id.toString()
            mapValues.remove(key)
            prefs?.edit()?.remove(key)?.apply()

        }
    }

    fun getAllCachedEvents(): List<ALEvent> {
        return ArrayList(mapValues.values)
    }

    private fun loadFromPrefToMemory() {
        prefs?.all?.forEach {
            val key = it.key
            val value = it.value as? String
            if (value != null) {
                mapValues[key] = ALEvent(JSONObject(value))
            }
        }
    }
}