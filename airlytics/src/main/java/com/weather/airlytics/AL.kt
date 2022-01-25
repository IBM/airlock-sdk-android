package com.weather.airlytics

import android.content.Context
import android.content.Intent
import com.weather.airlytics.environments.ALEnvironment
import com.weather.airlytics.environments.ALEnvironmentConfig
import com.weather.airlytics.events.ALEventConfig
import com.weather.airlytics.location.FusedLocationService
import com.weather.airlytics.persistence.ALAndroidCache
import com.weather.airlytics.providers.ALProviderException
import com.weather.airlytics.providers.data.ALProviderConfig
import com.weather.airlytics.sessions.SessionsManager
import com.weather.airlytics.userattributes.ALUserAttribute
import com.weather.airlytics.utils.ALCrashTracker
import com.weather.airlytics.utils.CrashHandler
import com.weather.airlytics.utils.RestFailuresAlerter
import org.jetbrains.annotations.TestOnly
import org.json.JSONArray
import java.lang.Thread.getDefaultUncaughtExceptionHandler
import java.lang.Thread.setDefaultUncaughtExceptionHandler
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet

/**
 * General class for basic SDK abilities as creating environment, registering provider handlers,
 */
@Suppress("unused")
class AL {

    companion object {
        var handlersMap: HashMap<String, String> = HashMap() // maps id with its configuration
        private var userAttributesCache: ALAndroidCache? = null
        var userAttributeGroupsMap: MutableMap<String,Int> = HashMap()
        var groupedAttributes: MutableList<List<String>> = ArrayList()
        var context: Context? = null

        /**
         * Creates an environment object
         */
        fun createEnvironment(
            environmentConfig: ALEnvironmentConfig,
            providerConfigs: List<ALProviderConfig>,
            eventConfigs: List<ALEventConfig>? = null,
            userAttributrsConfigs: List<ALUserAttribute>? = null,
            userID: String? = null,
            productID: UUID? = null,
            appVersion: String? = null,
            context: Context
        ): ALEnvironment {
            if (userAttributesCache == null) {
                userAttributesCache = ALAndroidCache("userAttributes", context)
            }
            this.context = context
            val environment = ALEnvironment(environmentConfig, context, userAttributesCache)

            if (userID != null) {
                environment.userId = userID
            } else {
                environment.userId = UUID.randomUUID().toString()
            }
            if (environment.productId == null && productID != null) {
                environment.productId = productID
            }
            if (environment.appVersion == null && appVersion != null) {
                environment.appVersion = appVersion
            }

            for (providerConfig in providerConfigs) {
                val providerId = providerConfig.id
                val providerAllowed = environmentConfig.providers?.contains(providerId) ?: false
                if (!environment.providers.containsKey(providerId) && providerAllowed) {
                    environment.providers[providerConfig.id] = providerConfig
                }
            }

            if (eventConfigs != null) {
                for (eventConfig in eventConfigs) {
                    environment.eventsMap[eventConfig.name] = eventConfig
                }
            }

            if (userAttributrsConfigs != null) {
                environment.userAttributesNameOverrideMap = HashMap()
                environment.customDimensionsList = ArrayList()
                for (userAttribute in userAttributrsConfigs) {
                    if (userAttribute.sendAsUserAttribute){
                        environment.userAttributesNameOverrideMap.put(userAttribute.name,userAttribute.nameOverride)
                    }
                    if (userAttribute.sendAsCustomDimension){
                        environment.customDimensionsList?.add(userAttribute.name)
                    }
                }
            }

            val crashDetails = ALCrashTracker.initTrackerAndGetLastSeenTime(context)
            if (handlersMap.isNotEmpty()) {
                environment.initProviderHandlers()
            }

            setUncaughtException()

            SessionsManager.addEnvironment(environment, context.applicationContext, crashDetails)

            return environment
        }

        fun createEnvironment(
            environmentConfig: ALEnvironmentConfig,
            providerConfigs: List<ALProviderConfig>,
            eventConfigs: List<ALEventConfig>? = null,
            userID: String? = null,
            productID: UUID? = null,
            appVersion: String? = null,
            context: Context
        ): ALEnvironment {
            return createEnvironment(environmentConfig, providerConfigs, eventConfigs, null, userID, productID, appVersion, context)
        }

        /**
         * Sets debugging Toast functionality
         */
        fun setDebugEnable(enable: Boolean, providerType: String) {
            var debugContext: Context? = null
            val environments = SessionsManager.getEnvironments()
            for (environment in environments){
                debugContext = environment.setProviderEnable(enable,providerType)
            }
            RestFailuresAlerter.init(debugContext)
        }

        fun setRestAlerterEnabled(){

        }

        /**
         * Method for setting flag on the app crash
         */
        fun notifyCrashHandler() {
            ALCrashTracker.writeIsCrashValue(true)
        }

        private fun setUncaughtException() {
            val defaultUncaughtExceptionHandler = getDefaultUncaughtExceptionHandler()
            val uncaughtExceptionHandler = CrashHandler(defaultUncaughtExceptionHandler)
            setDefaultUncaughtExceptionHandler(uncaughtExceptionHandler)
        }

        /**
         * Method for registering the provider handlers
         */
        fun registerProviderHandlers(classHandlers: Map<String, String>) {

            val exceptions = StringBuilder()
            for (pair in classHandlers) {
                if (handlersMap.containsKey(pair.key)) {
                    exceptions.append("provider of type \"${pair.key}\" already exists;\n")
                } else {
                    handlersMap[pair.key] = pair.value
                }
            }

            if (exceptions.isNotEmpty()) {
                throw ALProviderException(exceptions.toString())
            }
        }

        /**
         * Method for clearing out th provider handlers - used for Unit tests
         */
        @TestOnly
        fun clearProviderHandlers() {
            handlersMap.clear()
        }

        /**
         * method to return the current environment
         */
        fun getEnvironment(name: String): ALEnvironment? {
            return SessionsManager.getEnvironment(name)
        }


        /**
         * method to return a specific environment log entries
         */
        fun getEnvironmentLogEvents(context: Context, name: String): MutableMap<String, *>? {
            return context.getSharedPreferences("ALLoggerCache_$name", Context.MODE_PRIVATE).all
        }

        fun updateUserId(userId: String) {
            SessionsManager.updateUserId(userId)
        }

        fun verifyLifecycleStarted(){
            SessionsManager.verifyStarted()
        }

        fun setUserAttributeGroups(groups: JSONArray) {
            for (i in 0 until groups.length()) {
                val groupList: MutableList<String> = ArrayList()
                val groupValuesArray = groups.optJSONArray(i)
                for (j in 0 until groupValuesArray.length()) {
                    val attributeName = groupValuesArray.optString(j)
                    this.userAttributeGroupsMap[attributeName] = i
                    groupList.add(attributeName)
                }
                groupedAttributes.add(i, groupList)
            }
        }

        fun getAttributeGroupList(name: String): List<String>? {
            var groupList : List<String>? = null
            userAttributeGroupsMap[name]?.let {
                groupList = groupedAttributes[it]
            }
            return groupList
        }

//        fun validateEventSchema(
//            event: ALEvent,
//            eventConfig: ALEventConfig?
//        ): Boolean {
//            try{
//                val jsonToValidate = event.toJSONForSend().optJSONObject("attributes")
//                val schema = eventConfig?.jsonSchema
//                if (jsonToValidate != null && schema != null){
//                    val schema = SchemaLoader.load(jsonToValidate)
//                    val jsonToValidate = event.toJSONForSend().optJSONObject("attributes")
//                    schema.validate(jsonToValidate)
//                }
//            }catch (ex: ValidationException){
//                return false
//                //TODO log
//            }
//            return true
//        }
    }


}