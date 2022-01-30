package com.ibm.airlytics.events

import com.ibm.airlytics.environments.ALEnvironment
import com.ibm.airlytics.sessions.SessionsManager
import org.json.JSONObject
import java.util.*
import kotlin.collections.HashSet

/**
 * Class the represents the event data
 */
class ALEvent() {

    companion object {
        const val ID = "eventId"
        const val PRODUCT_ID = "productId"
        const val APP_VERSION = "appVersion"
        const val NAME = "name"
        const val USER_ID = "userId"
        const val SESSION_ID = "sessionId"
        const val SESSION_START_TIME = "sessionStartTime"
        const val EVENT_TIME = "eventTime"
        const val PLATFORM = "platform"
        const val SCHEMA_VERSION = "schemaVersion"
        const val ATTRIBUTES = "attributes"
        const val PREVIOUS_VALUES = "previousValues"
        const val PLATFORM_ANDROID = "android"
        const val CUSTOM_DIMENSIONS = "customDimensions"
    }

    var config: ALEventConfig = ALEventConfig()
    private var userId: String? = ""
    var id: UUID? = null
    private var productId: UUID? = null
    private var appVersion: String? = null
    var name: String = ""
    var sessionId: UUID? = null
    var sessionStartTime: Long? = null
    var eventTime: Long = 0
    private var schemaVersion: String? = null
    private var appIsBackground: Boolean = false
    var attributes: MutableMap<String, Any?> = HashMap()
    var customDimensions: MutableMap<String, Any?> = HashMap()
    var previousValues: MutableMap<String, Any?>? = null

    constructor(
        eventName: String,
        evenId: UUID?,
        environment: ALEnvironment,
        eventTime: Long,
        schemaVersion: String?,
        appIsBackgroundState: Boolean,
        attributes: MutableMap<String, Any?>,
        previousValues: MutableMap<String, Any?>? = null
    ) : this() {
        this.name = eventName
        id = evenId
        this.userId = environment.userId
        this.sessionId = SessionsManager.getSessionId(environment.config.name)
        this.sessionStartTime = SessionsManager.getSessionStartTime(environment.config.name)
        this.productId = environment.productId
        this.appVersion = environment.appVersion
        this.eventTime = eventTime
        if (schemaVersion != null) {
            this.schemaVersion = schemaVersion
        } else {
            this.schemaVersion = environment.eventsMap[name]?.schemaVersion
        }
        this.attributes = attributes
        this.previousValues = previousValues
        this.appIsBackground = appIsBackgroundState
    }

    constructor(json: JSONObject) : this() {
        val localAttr: MutableMap<String, Any?> = HashMap()
        val previousValues: MutableMap<String, Any?> = HashMap()
        id = UUID.fromString(json.optString(ID))
        userId = json.optString(USER_ID)
        name = json.optString(NAME)
        val sessionIdStr = json.optString(SESSION_ID)
        if (sessionIdStr.isNotEmpty()) {
            sessionId = UUID.fromString(sessionIdStr)
        }
        val productIdStr = json.optString(PRODUCT_ID)
        if (productIdStr.isNotEmpty()) {
            productId = UUID.fromString(productIdStr)
        }
        sessionStartTime = json.optLong(SESSION_START_TIME, -1)
        if (sessionStartTime == -1L) {
            sessionStartTime = null
        }

        schemaVersion = json.optString(SCHEMA_VERSION)
        eventTime = json.optLong(EVENT_TIME)
        appVersion = json.optString(APP_VERSION)

        json.optJSONObject(ATTRIBUTES)?.let {
            for (attributeKey in it.keys()) {
                localAttr[attributeKey] = it[attributeKey]
            }
        }

        json.optJSONObject(CUSTOM_DIMENSIONS)?.let {
            for (key in it.keys()) {
                customDimensions[key] = it[key]
            }
        }

        attributes = localAttr
        json.optJSONObject(PREVIOUS_VALUES)?.let {
            if (it.length() > 0){
                for (attributeKey in it.keys()) {
                    previousValues[attributeKey] = it[attributeKey]
                }
                this.previousValues = previousValues
            }else{
                this.previousValues = null
            }
        }
    }

    constructor(
        name: String,
        attributes: MutableMap<String, Any?>,
        eventTime: Long,
        environment: ALEnvironment,
        schemaVersion: String?
    ) : this(
        name, UUID.randomUUID(), environment,
        eventTime, schemaVersion, false, attributes, null
    )

    override fun toString(): String {
        return toJSONForSend().toString()
    }

    fun toJSONForSend(): JSONObject {
        val result = JSONObject()
        result.put(ID, id)
        result.put(PRODUCT_ID, productId)
        result.put(APP_VERSION, appVersion)
        result.put(SCHEMA_VERSION, schemaVersion)
        result.put(NAME, name)
        result.put(USER_ID, userId)
        sessionId?.let {
            result.put(SESSION_ID, it)
        }
        sessionStartTime?.let {
            result.put(SESSION_START_TIME, it)

        }
        result.put(EVENT_TIME, eventTime)
        result.put(PLATFORM, PLATFORM_ANDROID)
        val outAttributes = JSONObject()
        for (attribute in attributes) {
            if (attribute.value == null){
                outAttributes.put(attribute.key, JSONObject.NULL)
            }else{
                outAttributes.put(attribute.key, attribute.value)
            }
        }
        result.put(ATTRIBUTES, outAttributes)

        previousValues?.let {
            if (it.isNotEmpty()) {
                val outPreviousValues = JSONObject()
                for (previousValue in it) {
                    outPreviousValues.put(previousValue.key, previousValue.value)
                }
                result.put(PREVIOUS_VALUES, outPreviousValues)
            }
        }
        if (customDimensions.isNotEmpty()) {
            val outCustomDimensions = JSONObject()
            for (attr in customDimensions) {
                outCustomDimensions.put(attr.key, attr.value)
            }
            result.put(CUSTOM_DIMENSIONS, outCustomDimensions)
        }

        return result
    }
}