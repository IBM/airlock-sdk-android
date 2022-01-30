package com.ibm.airlytics.providers

import com.ibm.airlytics.providers.data.ALProviderConfig
import org.json.JSONObject
import org.junit.Test

import org.junit.Assert.*

class ALProviderConfigTest {

    @Test
    fun setAndGetTest() {
        try {
            val jsonObj = JSONObject()
            jsonObj.put(ALProviderConfig.ID, "name1")
            jsonObj.put(ALProviderConfig.TYPE, "type1")
            jsonObj.put(ALProviderConfig.CONNECTION, JSONObject("{\"url\":\"aaa\"}"))
            jsonObj.put(ALProviderConfig.DESCRIPTION, "desc1")
            val config =
                ALProviderConfig(jsonObj)
            assertEquals("name1", config.id)
            assertEquals("type1", config.type)
            assertEquals("{\"url\":\"aaa\"}", config.connection.toString())
            assertEquals("desc1", config.description)
            val resultJsonObj = config.toJSON()
            assertEquals("name1", resultJsonObj.optString(ALProviderConfig.ID))
            assertEquals("type1", resultJsonObj.optString(ALProviderConfig.TYPE))
            assertEquals(
                "{\"url\":\"aaa\"}",
                resultJsonObj.getJSONObject(ALProviderConfig.CONNECTION).toString()
            )
            assertEquals("desc1", resultJsonObj.optString(ALProviderConfig.DESCRIPTION))
        }catch (ex : Exception){
            fail(ex.message)
        }
    }
}