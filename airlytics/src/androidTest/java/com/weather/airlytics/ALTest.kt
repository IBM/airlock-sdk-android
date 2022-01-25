package com.weather.airlytics

import android.content.Context
import com.weather.airlytics.environments.ALEnvironment
import com.weather.airlytics.environments.ALEnvironmentConfig
import com.weather.airlytics.events.ALEvent
import com.weather.airlytics.events.ALEventConfig
import com.weather.airlytics.providers.data.ALProviderConfig
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test
import org.mockito.Mockito.mock
import java.util.*

class ALTest {

    @Test
    fun testSchemaValidation() {
        val schema = JSONObject(
            "{" +
                    "\"\$schema\":\"http://json-schema.org/draft-04/schema#\"," +
                    "\"definitions\":{" +
                    "}," +
                    "\"id\":\"http://example.com/example.json\"," +
                    "\"properties\":{" +
                    "\"attr1\":{" +
                    "\"id\":\"/attributes/analyticsEventsEnabled\"," +
                    "\"type\":\"string\"" +
                    "}," +
                    "\"attr2\":{" +
                    "\"id\":\"/attributes/networkRequestsEnabled\"," +
                    "\"type\":\"boolean\"" +
                    "}" +
                    "}," +
                    "\"type\":\"object\"" +
                    "}"
        )

        val config = ALEventConfig()
        config.jsonSchema = schema
//        if (!AL.validateEvent(createEvent(mapOf("attr1" to "value1")), config)){
//            fail("validation was expected to pass")
//        }
//        if (AL.validateEvent(createEvent(mapOf("attr2" to "value1")), config)){
//            fail("validation was expected to fail")
//        }
    }


    @Suppress("SameParameterValue")
    private fun createEvent(attributes: Map<String, Any>?, name: String): ALEvent {
        val eventJson = JSONObject()
        val jsonAttributes = JSONObject()

        attributes?.let {
            for (attribute in it) {
                jsonAttributes.put(attribute.key, attribute.value)
            }
        }

        eventJson.put("attributes", jsonAttributes)
        val uuidString = UUID.randomUUID().toString()
        eventJson.put("eventId", uuidString)
        eventJson.put("name", name)
        eventJson.put("eventTime", System.currentTimeMillis())
        eventJson.put("sessionId", uuidString)
        eventJson.put("userId", "user1")
        return ALEvent(eventJson)
    }

    @Test
    fun createEnvironment() {
        AL.clearProviderHandlers()
        val context = mock(Context::class.java)
        val providerConfigs: MutableList<ALProviderConfig> = ArrayList()
        val eventsConfigs: MutableList<ALEventConfig> = ArrayList()

        try {
            AL.registerProviderHandlers(
                mapOf(
                    //do not add rest provider unless you have mocked context ....
                    "EVENT_LOG" to "com.weather.airlytics.providers.EventLogProvider"
                )
            )
        } catch (th: Throwable) {
            Assert.fail("Provider Handlers registration failed: ${th.message}")
        }

        try {
            AL.createEnvironment(
                ALEnvironmentConfig(JSONObject()),
                providerConfigs,
                null,
                null,
                null,
                null,
                context
            )
        } catch (th: Throwable) {
            Assert.fail("environment creation failed")
        }

        providerConfigs.add(
            ALProviderConfig(
                JSONObject("{\"type\":\"EVENT_LOG\",\"id\":\"event-log\",\"description\":\"\",\"acceptAllEvents\":true,\"builtInEvents\":true,\"filter\":\"true\",\"additionalInfo\":{\"maxEventsAgeInSeconds\":15},\"debugUser\":true}")
            )
        )
        providerConfigs.add(
            ALProviderConfig(
                JSONObject("{\"type\":\"DEBUG_BANNERS\",\"id\":\"debug-banners\",\"description\":\"\",\"acceptAllEvents\":true,\"builtInEvents\":true,\"filter\":\"true\",\"debugUser\":true}")
            )
        )

        eventsConfigs.add(ALEventConfig(JSONObject("{\"name\":\"session-start\",\"description\":\"\",\"schemaVersion\":\"1.0\",\"jsonSchema\":{},\"validationRule\":\"true\"}")))
        eventsConfigs.add(ALEventConfig(JSONObject("{\"name\":\"session-end\",\"description\":\"\",\"schemaVersion\":\"1.0\",\"jsonSchema\":{},\"validationRule\":\"true\"}")))
        eventsConfigs.add(ALEventConfig(JSONObject("{\"name\":\"user-attributes\",\"description\":\"\",\"schemaVersion\":\"1.0\",\"jsonSchema\":{},\"validationRule\":\"true\"}")))
        eventsConfigs.add(ALEventConfig(JSONObject("{\"name\":\"first-time-launch\",\"description\":\"\",\"schemaVersion\":\"1.0\",\"jsonSchema\":{},\"validationRule\":\"true\"}")))
        eventsConfigs.add(ALEventConfig(JSONObject("{\"name\":\"app-launch\",\"description\":\"\",\"schemaVersion\":\"1.0\",\"jsonSchema\":{},\"validationRule\":\"true\"}")))
        eventsConfigs.add(ALEventConfig(JSONObject("{\"name\":\"app-crash\",\"description\":\"\",\"schemaVersion\":\"1.0\",\"jsonSchema\":{},\"validationRule\":\"true\"}")))
        eventsConfigs.add(ALEventConfig(JSONObject("{\"name\":\"stream-results\",\"description\":\"\",\"schemaVersion\":\"1.0\",\"jsonSchema\":{},\"validationRule\":\"true\"}")))

        var envCreated: ALEnvironment? = null

        try {
            envCreated = AL.createEnvironment(
                ALEnvironmentConfig(JSONObject("{\"name\":\"prod1Environment\",\"tags\":[\"PROD\"],\"description\":\"\",\"providers\":[\"rest-proxy-dev\",\"event-log\",\"streams-events\",\"debug-banners\"],\"enableClientSideValidation\":false,\"sessionExpirationInSeconds\":5,\"lastSeenTimeInterval\":1,\"streamResults\":true}")),
                providerConfigs,
                eventsConfigs,
                null,
                null,
                null,
                mock(Context::class.java)
            )
        } catch (th: Throwable) {
            Assert.fail("environment creation failed")
        }
        if (envCreated == null) {
            Assert.fail("env Not Created")
        }
        val env = AL.getEnvironment("prod1Environment")

        if (env == null) {
            Assert.fail("env Not Retrieved")
        }
        if (!environmentsAreEqual(envCreated!!, env!!)) {
            Assert.fail("env are not equal")
        }

        for (i in 1..400) {
            env.track(createEvent(null, "user-attributes"))
        }

        AL.clearProviderHandlers()
    }

    private fun environmentsAreEqual(envCreated: ALEnvironment, env: ALEnvironment): Boolean {
        if (envCreated.config.name != env.config.name ||
            envCreated.config.isDebugUser != env.config.isDebugUser ||
            envCreated.config.description != env.config.description
        ) {
            return false
        }
        if (envCreated.userId != env.userId ||
            envCreated.productId != env.productId ||
            envCreated.providers.size != env.providers.size
        ) {
            return false
        }
        return true
    }

    @Test
    fun registerProviderHandlers() {
        AL.clearProviderHandlers()

        try {
            AL.registerProviderHandlers(
                mapOf(
                    //do not add rest provider unless you have mocked context ....
                    "DEBUG_BANNERS" to "com.weather.airlytics.providers.DebugBannersProvider",
                    "EVENT_LOG" to "com.weather.airlytics.providers.EventLogProvider"
                )
            )
        } catch (th: Throwable) {
            Assert.fail("Provider Handlers registration failed")

        }
        AL.clearProviderHandlers()
        AL.registerProviderHandlers(
            mapOf(
                "DEBUG_BANNERS" to "com.weather.airlytics.providers.DebugBannersProvider",
                "EVENT_LOG" to "com.weather.airlytics.providers.EventLogProvider"
            )
        )

        try {
            AL.registerProviderHandlers(
                mapOf(
                    "DEBUG_BANNERS" to "com.weather.airlytics.providers.DebugBannersProvider",
                    "EVENT_LOG" to "com.weather.airlytics.providers.EventLogProvider"
                )
            )
        } catch (th: Throwable) {
            return
        }
        Assert.fail("expected to ge an exception")
    }
}