package com.ibm.airlytics.environments

import android.content.Context
import com.ibm.airlytics.AL
import com.ibm.airlytics.events.ALEvent
import com.ibm.airlytics.events.ALEventConfig
import com.ibm.airlytics.persistence.ALCache
import com.ibm.airlytics.providers.*
import com.ibm.airlytics.providers.data.ALProviderConfig
import com.ibm.airlytics.sessions.SessionDetails
import com.ibm.airlytics.sessions.SessionsManager
import com.ibm.airlytics.utils.ALCrashTracker
import com.ibm.airlytics.utils.JSRunner
import org.jetbrains.annotations.TestOnly
import org.json.JSONException
import org.json.JSONObject
import java.lang.reflect.Constructor
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.logging.Logger
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * Represents the Environment Object that holds all its provider definition and its events
 */
@Suppress("unused") //since it can be used by the consumer of the SDK
class ALEnvironment {

    private val logger = Logger.getLogger(ALEnvironment::class.java.name)

    var config: ALEnvironmentConfig = ALEnvironmentConfig()
    var providers: ConcurrentMap<String, ALProviderConfig> = ConcurrentHashMap()
    var userId: String = ""
    var productId: UUID? = null
    var appVersion: String? = null
    private var providerHandlers: ConcurrentHashMap<String, ALProvider> = ConcurrentHashMap()
    var eventsMap: ConcurrentMap<String, ALEventConfig> = ConcurrentHashMap()
    var userAttributesNameOverrideMap: MutableMap<String, String> = ConcurrentHashMap()
    var customDimensionsList: MutableList<String>? = null

    var context: Context? = null
    private var userAttributesCache: ALCache? = null
    private val userAttributesCacheLock: Any =
        Object()//lock object when updating userAttributes cache

    //prevent calling the default constructor
    private constructor()

    constructor(
        config: ALEnvironmentConfig,
        context: Context? = null,
        userAttributesCache: ALCache? = null
    ) {
        this.config = config
        this.context = context
        this.userAttributesCache = userAttributesCache
    }

    /**
     * Method to init the provider belonging to this environment
     * Expects getting a lint of provider configs
     */
    @Throws(ALProviderException::class)
    fun initProviders(providerConfigs: MutableList<ALProviderConfig>) {
        val exceptions = StringBuilder()
        for (config in providerConfigs) {
            val handlerName = AL.handlersMap[config.type]
            if (handlerName != null) {
                val clazz = Class.forName(handlerName)
                val ctr = clazz.getConstructor(ALProviderConfig::class.java)
                val providerInstance: ALProvider = ctr.newInstance(config) as ALProvider
                providerInstance.init(context)
                providerHandlers[config.type] = providerInstance
            } else {
                exceptions.append("handler of type ${config.type} was not found;\n")
            }
            if (exceptions.isNotEmpty()) {
                throw ALProviderException(exceptions.toString())
            }
            config.isDebugUser = this.config.isDebugUser
            providers[config.id] = config
        }
    }

    /**
     * Method to initialize provider handlers
     * The initialization is using reflection for instantiating the handler Object
     * Note: initialization of handler should be called once only
     */
    fun initProviderHandlers() {
        for (config in providers.values) {
            try {
                config.environmentId = this.config.name
                providerHandlers[config.type] = createProviderHandler(config, context)
            } catch (ex: ALProviderException) {
                logger.warning(ex.message)
            }
        }
    }

    /**
     * Method for tracking Crash
     * Crash is identified when last app life cycle ended not gracefully
     */
    fun trackCrash(
        crashDetails: ALCrashTracker.CrashDetails,
        sessionId: SessionDetails.SessionId
    ) {
        val eventTime = crashDetails.lastSeen ?: System.currentTimeMillis()
        val event = ALEvent(
            "app-crash",
            UUID.randomUUID(),
            this,
            eventTime,
            "2.0",
            false,
            HashMap()
        )
        event.sessionId = sessionId.id
        event.sessionStartTime = sessionId.startTime
        track(event)
    }

    /**
     * Method for creating the Method handler Object - using Reflection
     */
    private fun createProviderHandler(
        config: ALProviderConfig,
        context: Context? = null
    ): ALProvider {
        val handlerType = AL.handlersMap[config.type]
        config.environmentId = this.config.name
        var providerInstance: Any? = null
        if (handlerType != null) {
             try {
                if (handlerType.startsWith("com.ibm.airlytics.provider")){
                    when {
                        handlerType.endsWith("DebugBannersProvider") -> {
                            providerInstance = DebugBannersProvider(config)
                        }
                        handlerType.endsWith("EventLogProvider") -> {
                            providerInstance = EventLogProvider(config)
                        }
                        handlerType.endsWith("RestEventProxyProvider") -> {
                            providerInstance = RestEventProxyProvider(config, this.config.tags)
                        }
                    }
                }else {

                        val clazz = Class.forName(handlerType)
                        var ctr: Constructor<out Any>?
                        try {
                            ctr =
                                clazz.getConstructor(
                                    ALProviderConfig::class.java,
                                    MutableSet::class.java
                                )
                            providerInstance = ctr?.newInstance(config, this.config.tags)

                        } catch (noSuch: NoSuchMethodException) {
                            ctr =
                                clazz.getConstructor(ALProviderConfig::class.java)
                            providerInstance = ctr?.newInstance(config)

                        }

                }
             } catch (th: Throwable) {
                 throw ALProviderException("handler of type ${config.type} was not created ${th.message};\n")
             }
            providerInstance?.let{
                val alProvider = it as ALProvider
                alProvider.init(context)
                providerHandlers[config.type] = alProvider
                return it
            }
        }
        throw ALProviderException("handler of type ${config.type} was not found;\n")
    }

    /**
     *This method is good for tests - clear out previous handlers
     */
    @TestOnly
    fun clearProviders() {
        providerHandlers.clear()
    }

    /**
     * Method to register events for this environment
     * None registered Events will not be processed unless environment allowed it
     */
    fun registerEvents(eventConfigs: List<ALEventConfig>) {
        val exceptions = StringBuilder()
        for (config in eventConfigs) {
            val id = config.name
            if (eventsMap.containsKey(id)) {
                exceptions.append("event with name \"$id\" already exists;\n")
            } else {
                eventsMap[id] = config
            }
        }
        if (exceptions.isNotEmpty()) {
            throw ALProviderException(exceptions.toString())
        }
    }

    private fun getSupportedProvidersOfEvent(eventConfig: ALEventConfig): List<Pair<ALProviderConfig, ALProvider>> {
        val retProviders: MutableList<Pair<ALProviderConfig, ALProvider>> = ArrayList()
        for (providerConfig in providers.values) {
            if (providerConfig.acceptAllEvents || providerConfig.eventConfigs?.containsKey(
                    eventConfig.name
                ) == true
            ) {
                providerHandlers[providerConfig.type]?.let {
                    retProviders.add(
                        Pair(
                            providerConfig,
                            it
                        )
                    )
                }
            }
        }
        return retProviders
    }

    /**
     * Method for sending to providers user attributes data
     * Note that only attributes that were changed from previous calls will be sent
     */
    fun setUserAttributes(attributes: Map<String, Any?>, schemaVersion: String? = null) {
        val updatedAttributes: MutableMap<String, Any?> = HashMap()
        val previousValues: MutableMap<String, Any?> = HashMap()
        synchronized(userAttributesCacheLock) {
            for (key in attributes.keys) {
                //if userAttributesSet is not null and it does not contain the key - setting should be skipped
                // userAttributesSet not null means airlock switched to managed user attributes using separate feature for each attribute
                if (userAttributesNameOverrideMap.contains(key)){
                    val value = attributes[key]
                    var cachedValue: Any? = null
                    var cacheKeyExists = false
                    userAttributesCache?.let { cache ->
                        if (cache.containsKey(key)) {
                            cacheKeyExists = true
                            cache.getValue(key)?.let {
                                cachedValue = try {
                                    JSONObject(it).opt("value")
                                } catch (ex: JSONException) {
                                    //old format with flat value
                                    it
                                }
                            }
                        }
                    }

                    if (!cacheKeyExists || value.toString() != cachedValue.toString()) {
                        updatedAttributes[key] = value
                        if (cacheKeyExists) {
                            previousValues[key] = cachedValue
                        }
                        val valueObject = JSONObject()
                        valueObject.put("value", value)
                        valueObject.put("lastSent", System.currentTimeMillis())
                        if (schemaVersion != null) {
                            valueObject.put("schemaVersion", schemaVersion)
                        }
                        userAttributesCache?.setValue(key, valueObject.toString())
                    }
                }
            }
        }
        if (updatedAttributes.isNotEmpty()) {
            //If attribute depends on other attribute send the dependant attribute as well
            val dependantGroups: MutableList<Int> = ArrayList()
            for (attribute in updatedAttributes.keys) {
                AL.userAttributeGroupsMap[attribute]?.let {
                    dependantGroups.add(it)
                }
            }
            for (groupIndex in dependantGroups) {
                for (attributeDependant in AL.groupedAttributes[groupIndex]) {
                    if (!updatedAttributes.containsKey(attributeDependant)) {
                        userAttributesCache?.let {
                            updatedAttributes[attributeDependant] =
                                it.getValue(attributeDependant)?.let { cacheValue ->
                                    JSONObject(cacheValue).opt("value")
                                }
                        }
                    }
                }
            }

            val attributeNamesToOverride: MutableMap<String, String> = HashMap()

            for (key in updatedAttributes.keys){
                userAttributesNameOverrideMap[key]?.let {
                    if (it.isNotEmpty()){
                        attributeNamesToOverride[key] = it
                    }
                }
            }
            for (key in attributeNamesToOverride.keys){
                attributeNamesToOverride[key]?.let {
                    updatedAttributes[it] = updatedAttributes[key]
                    updatedAttributes.remove(key)
                }
            }

            val event = ALEvent(
                "user-attributes",
                UUID.randomUUID(),
                this,
                System.currentTimeMillis(),
                schemaVersion,
                false,
                updatedAttributes,
                previousValues
            )
            track(event)
        }
    }

    fun resendUserAttributes() {
        if (config.resendUserAttributesIntervalInSeconds < 1) {
            return
        }
        var schemaVersion = "1.0"
        val updatedAttributes = mutableMapOf<String, Any?>()
        synchronized(userAttributesCacheLock) {
            userAttributesCache?.mapValues?.let { mapValues ->
                for ((key, value) in mapValues) {
                    try {
                        val jsonValue = JSONObject(value)
                        val lastSent = jsonValue.optLong("lastSent", 0)
                        if (lastSent > 0 && ((System.currentTimeMillis() - lastSent) / 1000) > config.resendUserAttributesIntervalInSeconds) {
                            val innerValue: Any? = jsonValue.opt("value")
                            innerValue?.let {
                                updatedAttributes[key] = it
                                jsonValue.put("lastSent", System.currentTimeMillis())
                                userAttributesCache?.setValue(key, jsonValue.toString())
                                jsonValue.optString("schemaVersion", "1.0")?.let { jsonSchemaVersion ->
                                    if (jsonSchemaVersion.toDouble() > schemaVersion.toDouble()){
                                        schemaVersion = jsonSchemaVersion
                                    }
                                }
                            }
                        }
                    } catch (jsonException: JSONException) {
                        //old format attributes have "flat" value without support of resend
                    }
                }
            }
        }
        if (updatedAttributes.isNotEmpty()) {
            val event = ALEvent(
                "user-attributes",
                UUID.randomUUID(),
                this,
                System.currentTimeMillis(),
                schemaVersion,
                false,
                updatedAttributes
            )
            track(event)
        }
    }

    /**
     * Method that updates the environment
     * When updating the environment configuration this method should be called
     * If the relevant configuration was not changed - it will not do anything
     */
    fun update(
        enableClientSideValidation: Boolean,
        sessionExpirationInSeconds: Int,
        providerConfigs: List<ALProviderConfig>,
        eventConfigs: List<ALEventConfig>? = null
    ) {

        config.enableClientSideValidation = enableClientSideValidation
        config.sessionExpirationInSeconds.set(sessionExpirationInSeconds)

        val deletedProviders = providers.keys.toMutableList()
        val updatedProviders: MutableList<ALProviderConfig> = ArrayList()
        val deletedEvents = eventsMap.keys.toMutableList()
        val addedProviders: MutableList<ALProviderConfig> = ArrayList()

        for (providerConfig in providerConfigs) {
            if (!providers.containsKey(providerConfig.id)) {
                addedProviders.add(providerConfig)
                providers[providerConfig.id] = providerConfig
            } else {
                deletedProviders.remove(providerConfig.id)
                if (!providerConfig.equals(providers[providerConfig.id])) {
                    updatedProviders.add(providerConfig)
                }
            }
        }

        if (eventConfigs != null) {
            for (eventConfig in eventConfigs) {
                eventsMap[eventConfig.name] = eventConfig
                deletedEvents.remove(eventConfig.name)
            }
        }
        if (deletedProviders.isNotEmpty()) {
            for (provider in deletedProviders) {
                deleteProvider(provider)
            }
        }
        if (deletedEvents.isNotEmpty()) {
            for (deleted in deletedEvents) {
                eventsMap.remove(deleted)
            }
        }
        if (updatedProviders.isNotEmpty()) {
            for (provider in updatedProviders) {
                updateProvider(provider)
            }
        }
    }

    /**
     * Method to check if provider supports sending this kind of event
     */
    private fun shouldProviderAcceptEvent(
        providerConfig: ALProviderConfig,
        event: ALEvent
    ): Boolean {
        //check if accept all
        if (providerConfig.acceptAllEvents) {
            return true
        }
        //check if event name is in the list of accepted by provider
        if (true == providerConfig.eventConfigs?.containsKey(event.name)) {
            //check if filter works
            if (providerConfig.filter.isNotEmpty()) {
                //For now client side validation is not operational yet....
                //        if (config.enableClientSideValidation){
                //            if (!AL.validateEventSchema(event, eventsMap[event.name])){
                //                //validation warning logged by validate method
                //                return
                //            }
                //        }
                if (!JSRunner.runJSBooleanCondition("event=$event", providerConfig.filter)) {
                    return false
                }
            }
            return true
        }
        return false
    }

    fun clearSessionEvents (uuid: UUID) {
        for (provider in providerHandlers.values){
            provider.clearSessionEvents(uuid)
        }
    }


    /**
     * Method to send an event to all existing providers
     */
    fun track(event: ALEvent) {
        //check if this event is a known event on SDK
        val eventConfig: ALEventConfig? = eventsMap[event.name]
        if (eventConfig != null) {
            event.config = eventConfig
            if (event.sessionId == null) {
                event.sessionId = SessionsManager.getSessionId(config.name)
            }
            if (!(event.name.startsWith(SessionsManager.SESSION_PREFIX) || event.name.startsWith(
                    NOTIFICATION_PREFIX) || event.name == USER_ATTRIBUTES)){
                SessionsManager.updateLatestSessionActivity(config.name)
            }
            if (!JSRunner.runJSBooleanCondition("event=$event", event.config.jsValidationRule)) {
                return
            }
            if (event.name == "purchase-attempted") {
                event.attributes["price"]?.let {
                    val amount: Float = ((it as? Long ?: 0)).toFloat() / 1000000
                    event.attributes["price"] = amount
                }
            }
            if (event.name == "notification-received") {
                event.sessionId = null
                event.sessionStartTime = null
            }
            //temporary work around for converting field to String
            if (event.name == "location-viewed") {
                event.attributes["dmaCode"]?.let {
                    event.attributes["dmaCode"] = (it as? Int ?: -1).toString()
                }
                if (!event.attributes.containsKey("type")){
                    event.attributes["type"] = null
                }
            }

            customDimensionsList?.let {
                for (attribute in it) {
                    val doSetValue = event.config.customDimensionsOverride?.contains(attribute)
                        ?: true
                    if (doSetValue) {
                        userAttributesCache?.getValue(attribute)?.let { cacheValue ->
                            var attributeName = attribute
                            userAttributesNameOverrideMap[attribute]?.let {overriddenName ->
                                if (overriddenName.isNotEmpty()) {
                                    attributeName = overriddenName
                                }
                            }
                            if (!attributeName.isEmpty()) {
                                event.customDimensions[attributeName] =
                                    JSONObject(cacheValue).opt("value")
                            }
                        }
                    }
                }
            }
            val providers = getSupportedProvidersOfEvent(event.config)
            providers.forEach {
                if (shouldProviderAcceptEvent(it.first, event)) {
                    it.second.send(event)
                }
            }
        }
    }

    fun getSessionId(): UUID? {
        return SessionsManager.getSessionId(config.name)
    }

    fun getSessionStartTime(): Long? {
        return SessionsManager.getSessionStartTime(config.name)
    }

    /**
     * Method to call the sendEventsWhenGoingToBackground of providers when going to background
     */
    fun sendEventsWhenGoingToBackground() {
        for (provider in providerHandlers.values) {
            provider.sendEventsWhenGoingToBackground()
        }
    }

    /**
     * Method for deleting provider (on case provider was deleted from configuration)
     */
    fun deleteProvider(id: String) {
        providers.remove(id)
    }

    /**
     * Method for updating provider (on case provider was updated from configuration)
     */
    private fun updateProvider(provider: ALProviderConfig) {
        provider.environmentId = this.config.name
        providers[provider.id] = provider
        providerHandlers[provider.id]?.interval(provider)
    }

    fun disableEnvironment() {
        SessionsManager.forceCloseSession(config.name)
    }

    fun getProvider(name: String): ALProvider? {
        return providerHandlers[name]
    }

    fun setProviderEnable(enable: Boolean, type: String): Context? {
        for (providerConfig in providers.values) {
            if (providerConfig.type == type) {
                providers[providerConfig.id]?.acceptAllEvents = enable
                break
            }
        }
        var providerContext = context
        if (!enable) {
            providerContext = null
        }
        return providerContext
    }

    companion object {
        const val NOTIFICATION_PREFIX = "notification-"
        const val USER_ATTRIBUTES = "user-attributes"
    }
}
