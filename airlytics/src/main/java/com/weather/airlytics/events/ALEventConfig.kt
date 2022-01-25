package com.weather.airlytics.events

import org.json.JSONObject

/**
 * Class the represents the event configuration
 */

class ALEventConfig() {


    companion object {
        const val NAME = "name"
        const val DESCRIPTION = "description"
        const val JSON_SCHEMA = "jsonSchema"
        const val SCHEMA_VERSION = "schemaVersion"
        const val JS_VALIDATION_RULE = "validationRule"
        const val CUSTOM_DIMENSIONS_OVERRIDE = "customDimensionsOverride"
    }


    var name: String = ""
    var description: String = ""
    var jsonSchema: JSONObject? = null
    var customDimensionsOverride: MutableSet<String>? = null
    var schemaVersion: String = ""
    var jsValidationRule: String = ""

    constructor(config: JSONObject) : this() {
        name = config.optString(NAME)
        description = config.optString(DESCRIPTION)
        jsonSchema = config.optJSONObject(JSON_SCHEMA)
        jsValidationRule = config.optString(JS_VALIDATION_RULE)
        schemaVersion = config.optString(SCHEMA_VERSION)
        val customDimensionsOverrideArray = config.optJSONArray(CUSTOM_DIMENSIONS_OVERRIDE)
        customDimensionsOverrideArray?.let {
            customDimensionsOverride = HashSet()
            for(i in 0 until it.length()){
                customDimensionsOverride?.add(it[i] as String)
            }
        }
    }
}