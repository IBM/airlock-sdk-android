package com.weather.airlytics.environments

import com.weather.airlytics.AL
import com.weather.airlytics.events.ALEventConfig
import com.weather.airlytics.persistence.ALCache
import com.weather.airlytics.providers.EventLogProvider
import com.weather.airlytics.providers.data.ALProviderConfig
import org.json.JSONObject
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class ALEnvironmentTest {

    companion object{
        private var environmentConfig = ALEnvironmentConfig()
        private val cache =  ALCache("test")
        var environment = ALEnvironment(ALEnvironmentConfig(), null, cache)
        @BeforeClass
        @JvmStatic fun setup() {
            AL.clearProviderHandlers()
            AL.registerProviderHandlers(mapOf("kafka" to "com.weather.airlytics.providers.implementation.TestProviderHandler", "kafka1" to "com.weather.airlytics.providers.implementation.TestProviderHandler", "file" to "com.weather.airlytics.providers.implementation.TestProviderHandler",
                "EVENT_LOG" to "com.weather.airlytics.providers.EventLogProvider"))
            environmentConfig.name = "name1"
            environmentConfig.description = "desc1"
            environmentConfig.enableClientSideValidation = true
            environment.config = environmentConfig
            environment.registerEvents(listOf(ALEventConfig().apply {
                name = "startSession"
                jsValidationRule = "true"
                description = "desc1"
                jsonSchema = JSONObject()
                schemaVersion = "1.0"
            }, ALEventConfig().apply {
                name = "user-attributes"
                jsValidationRule = "true"
                description = "desc1"
                jsonSchema = JSONObject()
                schemaVersion = "1.0"
            }))
            try{
                environment.initProviders(
                    createProviderConfigs(
                        listOf("kafka", "kafka1", "EVENT_LOG")
                    )
                )
            }catch (ex : Exception){
                if (!ex.message?.contains("already exists")!!){
                    Assert.fail("test failed with ${ex.message}")
                }
            }
        }
        private fun createProviderConfigs(types: List<String>): MutableList<ALProviderConfig> {
            val providerConfigs : MutableList<ALProviderConfig> = ArrayList()

            for (type in types){
                val jsonObj = JSONObject()
                jsonObj.put(ALProviderConfig.TYPE, type)
                jsonObj.put(ALProviderConfig.ID, "${type}-name")
                jsonObj.put(ALProviderConfig.DESCRIPTION, "${type}-desc1")
                jsonObj.put(ALProviderConfig.BUILTIN_EVENTS, false)
                jsonObj.put(ALProviderConfig.CONNECTION, JSONObject("{\"url\":\"aaa\"}"))
                jsonObj.put(ALProviderConfig.ADDITIONAL_INFO, JSONObject())
                jsonObj.put(ALProviderConfig.ACCEPT_ALL_EVENTS, true)
                jsonObj.put(ALProviderConfig.TRACKING_CONFIGS, JSONObject())
                jsonObj.put(ALProviderConfig.EVENT_CONFIGS, JSONObject("{\"id\":\"event1\", \"realTime\": true}"))
                providerConfigs.add(
                    ALProviderConfig(
                        jsonObj
                    )
                )
            }

            return providerConfigs
        }
    }

    @Test
    fun containsPreviousValues(){
        environment.setUserAttributes(
            mapOf(
                "a" to 1,
                "b" to 2,
                "c" to 3,
                "d" to 4
            ),"1.0"
        )

        environment.setUserAttributes(
            mapOf(
                "a" to 99
            ),"1.0"
        )
        val events = (environment.getProvider("EVENT_LOG") as EventLogProvider).getAllCachedEvents()
        for (event in events){
            if (event.attributes.size == 4){
                Assert.assertTrue(event.attributes["a"] == 1)
                Assert.assertTrue(event.attributes["b"] == 2)
                Assert.assertTrue(event.attributes["c"] == 3)
                Assert.assertTrue(event.attributes["d"] == 4)
                Assert.assertTrue(event.previousValues == null || event.previousValues!!.isEmpty())
            }else{
                Assert.assertTrue(event.attributes["a"] == 99)
                Assert.assertTrue(event.previousValues != null && event.previousValues!!.size == 1 && event.previousValues!!["a"] == 1)
            }
        }
    }

    @Test
    fun testCreation(){
        environment.setUserAttributes(mapOf("pushToken" to "TestId999 : optional. can be a very long string, no specific format. It can also be null in case the pushid has been cancelled" ,
            "premium" to true,
            "upsId" to "ups12345",
            "installDate" to System.currentTimeMillis() - 1000000))
    }

    @Test
    fun testProviders(){
        environment.setUserAttributes(mapOf("pushToken" to "TestId999 : optional. can be a very long string, no specific format. It can also be null in case the pushid has been cancelled" ,
            "premium" to true,
            "upsId" to "ups12345",
            "installDate" to System.currentTimeMillis() - 1000000))
        environment.deleteProvider("kafka1")
        environment.setUserAttributes(mapOf("pushToken" to "TestId999 : optional. can be a very long string, no specific format. It can also be null in case the pushid has been cancelled" ,
            "premium" to true,
            "upsId" to "ups12345",
            "installDate" to System.currentTimeMillis() - 1000000))

    }

    @Test
    fun testSetUserAttributes300(){
        for (value in 1..300){
            environment.setUserAttributes(mapOf("pushToken" to "TestId $value"))
        }
    }
}