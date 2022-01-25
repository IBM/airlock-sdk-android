package com.weather.airlytics.providers

import com.weather.airlytics.AL
import com.weather.airlytics.environments.ALEnvironment
import com.weather.airlytics.environments.ALEnvironmentConfig
import com.weather.airlytics.providers.data.ALProviderConfig
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Test
import java.lang.ClassCastException

class ALProviderHandlersTest {

    companion object{
        var environment: ALEnvironment = ALEnvironment(ALEnvironmentConfig())
        @BeforeClass @JvmStatic fun setup() {
            AL.clearProviderHandlers()
            AL.registerProviderHandlers(mapOf("kafka" to "com.weather.airlytics.providers.implementation.TestProviderHandler", "file" to "com.weather.airlytics.providers.implementation.TestProviderHandler"))
        }
    }

    @After
    fun tearDown() {
        environment.clearProviders()
    }

    @Test
    fun registerAndInitTest() {
        try{
            environment.initProviders(createProviderConfig("kafka"))
        }catch (ex : Exception){
            fail("test failed with ${ex.message}")
        }
    }

    @Test
    fun registerAlreadyExists() {
        //register type that already exists
        try{
            AL.registerProviderHandlers(mapOf("kafka" to "com.weather.airlytics.providers.implementation.TestProviderHandler"))
        }catch (ex : Exception){
            assertEquals("provider of type \"kafka\" already exists;\n", ex.message)
            return
        }
        fail("test failed expected exception about provider already exists")
    }

    @Test
    fun providerImplementationNotFound() {
        try{
            AL.registerProviderHandlers(mapOf("type1" to "com.weather.airlytics.providers.ClassDoNotExist"))
            environment.initProviders(createProviderConfig("type1"))
        }catch (ex : Exception){
            assertTrue("Expected to get ClassNotFoundException", ex is ClassNotFoundException)
            return
        }
        fail("test failed Expected to get ClassNotFoundException")
    }

    @Test
    fun providerTypeNotFound() {
        try{
            environment.initProviders(createProviderConfig("unknownType"))
        }catch (ex : Exception){
            assertEquals("handler of type unknownType was not found;\n", ex.message)
            return
        }
        fail("test failed Expected to get ClassNotFoundException")
    }

    @Test
    fun providerDoNotImplementCorrectly() {
        try{
            AL.registerProviderHandlers(mapOf("type2" to "com.weather.airlytics.providers.implementation.TestWrongProviderHandler"))
            environment.initProviders(createProviderConfig("type2"))
        }catch (ex : Exception){
            assertTrue("Expected to get ClassCastException", ex is ClassCastException)
            return
        }
        fail("test failed Expected to get ClassCastException")
    }

    private fun createProviderConfig(type: String): MutableList<ALProviderConfig> {
        val providerConfigs : MutableList<ALProviderConfig> = ArrayList()
        val jsonObj = JSONObject()
        jsonObj.put(ALProviderConfig.ID, "${type}-name")
        jsonObj.put(ALProviderConfig.TYPE, type)
        jsonObj.put(ALProviderConfig.CONNECTION, JSONObject("{\"url\":\"aaa\"}"))
        jsonObj.put(ALProviderConfig.DESCRIPTION, "${type}-desc1")
        providerConfigs.add(
            ALProviderConfig(
                jsonObj
            )
        )
        return providerConfigs
    }
}