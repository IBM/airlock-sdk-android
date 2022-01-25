package com.weather.airlytics.providers.data

import org.json.JSONObject

/**
 * Class for representing the tracking policy of a single provider
 */
class ALProviderTrackingPolicyConfig() {
    /*
    	"trackingPolicy":{
		"intervals":{
			"slow":60,
			"medium":30,
			"fast":15,
			"unlimited":5
		},
		"sendEventsWhenGoingToBackground":false
	}
     */
    var intervals: MutableMap<String, Int?> = HashMap()
    var sendEventsWhenGoingToBackground: Boolean

    init {
        sendEventsWhenGoingToBackground = false
    }

    companion object {
        const val INTERVALS = "intervalsInSeconds"
        const val SLOW = "slow"
        const val MEDIUM = "medium"
        const val FAST = "fast"
        const val UNLIMITED = "unlimited"
        const val SEND_EVENTS_WHEN_GOING_TO_BACKGROUND = "sendEventsWhenGoingToBackground"
    }

    constructor(obj: JSONObject) : this() {
        obj.optJSONObject(INTERVALS)?.let {
            it.keys().forEach { key ->
                intervals[key] = it[key] as? Int
            }
        }
        sendEventsWhenGoingToBackground = obj.optBoolean(SEND_EVENTS_WHEN_GOING_TO_BACKGROUND, false)
    }

    fun toJSON(): JSONObject {
        val jsoObject = JSONObject()
        jsoObject.put(SEND_EVENTS_WHEN_GOING_TO_BACKGROUND, sendEventsWhenGoingToBackground)
        val intervalsObj = JSONObject()
        for (pair in intervals){
            intervalsObj.put(pair.key, pair.value)
        }
        jsoObject.put(INTERVALS, intervalsObj)
        return  jsoObject
    }

    fun clone() : ALProviderTrackingPolicyConfig {
        return ALProviderTrackingPolicyConfig(
            toJSON()
        )
    }

    @Override
    fun equals(policyConfig: ALProviderTrackingPolicyConfig?) :Boolean{
        if (policyConfig == null){
            return true
        }
        if (sendEventsWhenGoingToBackground != policyConfig.sendEventsWhenGoingToBackground){
            return false
        }
        if (intervals.keys != policyConfig.intervals.keys){
            return false
        }
        for (pair in intervals){
            pair.value?.let {
                if (!it.equals(policyConfig.intervals[pair.key])){
                    return false
                }
            }
        }
        return true
    }
}