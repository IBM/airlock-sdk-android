package com.ibm.airlytics.persistence.utils

import com.ibm.airlytics.events.ALEvent
import com.ibm.airlytics.utils.JSRunner
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.util.*

class JSRunnerTest{

@Test
fun testTrueResult() {
    val attributes: Map<String, Any> = mapOf("env1" to JSONObject("{}"))
    val event = createEvent("app-start", "user1", attributes)
    if (!JSRunner.runJSBooleanCondition("event=$event", "true")) {
        fail("expected to get result true")
    }
    if (JSRunner.runJSBooleanCondition("event=$event", "false")) {
        fail("expected to get result false")
    }
    if (!JSRunner.runJSBooleanCondition("event=$event", "event.name === 'app-start' ")) {
        fail("expected to get result true")
    }
    if (JSRunner.runJSBooleanCondition("event=$event", "event.name === 'video-played' ")) {
        fail("expected to get result false")
    }
    if (!JSRunner.runJSBooleanCondition("event=$event", "event.userId === 'user1' ")) {
        fail("expected to get result true")
    }
    if (JSRunner.runJSBooleanCondition("event=$event", "event.userId === 'admin' ")) {
        fail("expected to get result false")
    }
}


    private fun createEvent(eventName: String, userId: String, attributes: Map<String, Any>): ALEvent{
        val eventJson = JSONObject()
        val jsonAttributes = JSONObject()
        for (attribute in attributes){
            jsonAttributes.put(attribute.key, attribute.value)
        }
        eventJson.put("attributes",jsonAttributes)
        val uuidString = UUID.randomUUID().toString()
        eventJson.put("eventId",uuidString)
        eventJson.put("name",eventName)
        eventJson.put("eventTime",System.currentTimeMillis())
        eventJson.put("sessionId",uuidString)
        eventJson.put("userId",userId)
        return ALEvent(eventJson)
    }
}