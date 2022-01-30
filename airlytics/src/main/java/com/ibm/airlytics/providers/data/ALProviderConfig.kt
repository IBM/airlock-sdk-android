package com.ibm.airlytics.providers.data

import org.json.JSONObject

/**
 * Class representing the provider configuration
 */
class ALProviderConfig(config: JSONObject?) {

    companion object{
        const val ID = "id"
        const val TYPE = "type"
        const val DESCRIPTION = "description"
        const val CONNECTION = "connection"
        const val ACCEPT_ALL_EVENTS = "acceptAllEvents"
        const val BUILTIN_EVENTS = "builtInEvents"
        const val EVENT_CONFIGS = "events"
        const val TRACKING_CONFIGS = "trackingPolicy"
        const val ADDITIONAL_INFO = "additionalInfo"
        const val FILTER = "filter"
        const val FAILED_EVENTS_EXPIRATION_IN_SECONDS = "failedEventsExpirationInSeconds"
        const val ENVIRONMENT_ID = "environmentId"
        const val IS_DEBUG_USER = "debugUser"
    }

    var id: String = ""
    var environmentId: String = ""
    var type: String = ""
    var description: String = ""
    var connection: Any? = null
    var acceptAllEvents: Boolean = false
    private var builtInEvents: Boolean = false
    var failedEventsExpirationInSeconds: Double = (60 * 60 * 24 * 90).toDouble()
    var filter = ""
    var trackingPolicyConfigs: ALProviderTrackingPolicyConfig? = null
    var eventConfigs: MutableMap<String, ALProviderEventConfig>? = HashMap()
    var additionalInfo: JSONObject? = null
    var isDebugUser: Boolean = false

    init {
        if (config != null){
            fromJSON(config)
        }
    }

    /**
     * Converts data for JSON ti the data model
     */
    private fun fromJSON(jsonObject: JSONObject) {
        id = jsonObject.optString(ID)
        type = jsonObject.optString(TYPE)
        description = jsonObject.optString(DESCRIPTION)
        connection = jsonObject.optJSONObject(CONNECTION)
        acceptAllEvents = jsonObject.optBoolean(ACCEPT_ALL_EVENTS)
        builtInEvents = jsonObject.optBoolean(BUILTIN_EVENTS)
        filter = jsonObject.optString(FILTER)
        failedEventsExpirationInSeconds = jsonObject.optDouble(FAILED_EVENTS_EXPIRATION_IN_SECONDS,(60 * 60 * 24 * 90).toDouble())
        val events = jsonObject.optJSONArray(EVENT_CONFIGS)
        if(events != null){
            for (i in 0 until events.length()){
                val providerEventConfig =
                    ALProviderEventConfig(
                        events[i] as JSONObject
                    )
                eventConfigs?.put(providerEventConfig.id, providerEventConfig)
            }
        }

        val objPolicy = jsonObject.optJSONObject(TRACKING_CONFIGS)
        if(objPolicy != null) {
            trackingPolicyConfigs =
                ALProviderTrackingPolicyConfig(
                    objPolicy
                )
        }
        additionalInfo = jsonObject.optJSONObject(ADDITIONAL_INFO)
        environmentId = jsonObject.optString(ENVIRONMENT_ID)
        isDebugUser = jsonObject.optBoolean(IS_DEBUG_USER)
    }

    /**
     * Converts data to JSON
     */
    fun toJSON(): JSONObject {
        val jsonObject = JSONObject()
        jsonObject.put(ID, id)
        jsonObject.put(TYPE, type)
        jsonObject.put(DESCRIPTION, description)
        jsonObject.put(CONNECTION, connection)
        jsonObject.put(ACCEPT_ALL_EVENTS, acceptAllEvents)
        jsonObject.put(BUILTIN_EVENTS, builtInEvents)
        jsonObject.put(FILTER, filter)
        jsonObject.put(EVENT_CONFIGS, eventConfigs)
        jsonObject.put(TRACKING_CONFIGS, trackingPolicyConfigs?.toJSON())
        jsonObject.put(ADDITIONAL_INFO, additionalInfo)
        jsonObject.put(FAILED_EVENTS_EXPIRATION_IN_SECONDS, failedEventsExpirationInSeconds)
        return jsonObject
    }

    /**
     * Method to check if provider was updated on data that is important for processing
     */
    @Override
    fun equals(alProviderConfig: ALProviderConfig?): Boolean {
            trackingPolicyConfigs?.let {
                if (alProviderConfig != null){
                    if (it.equals(alProviderConfig.trackingPolicyConfigs)){
                        return true
                    }
                }
            }
            if (trackingPolicyConfigs == null && alProviderConfig?.trackingPolicyConfigs == null){
                return true
            }
        return false
    }

    /**
     * method for cloning the object values
     */
    fun clone() : ALProviderConfig {
      return ALProviderConfig(toJSON())
    }
}

