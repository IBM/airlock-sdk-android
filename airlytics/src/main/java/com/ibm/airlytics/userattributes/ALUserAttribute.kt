package com.ibm.airlytics.userattributes

import org.json.JSONObject

class ALUserAttribute() {

    companion object {

        const val NAME = "name"
        const val NAME_OVERRIDE = "nameOverride"
        const val SEND_AS_CUSTOM_DIMENTION = "sendAsCustomDimension"
        const val SEND_AS_USER_ATTRIBUTE = "sendAsUserAttribute"
        const val VALIDATION_RULE = "validationRule"

    }


    var name: String = ""
    var nameOverride: String = ""
    var sendAsCustomDimension: Boolean = false
    var sendAsUserAttribute: Boolean = false
    var validationRule: Boolean = false


    constructor(
        name: String,
        nameOverride: String,
        sendAsCustomDimension: Boolean,
        sendAsUserAttribute: Boolean,
        validationRule: Boolean
    ) : this() {
        this.name = name
        this.nameOverride = nameOverride
        this.sendAsCustomDimension = sendAsCustomDimension
        this.sendAsUserAttribute = sendAsUserAttribute
        this.validationRule = validationRule
    }

    constructor(json: JSONObject) : this() {
        name = json.optString(NAME)
        nameOverride = json.optString(NAME_OVERRIDE)
        sendAsCustomDimension = json.optBoolean(SEND_AS_CUSTOM_DIMENTION, false)
        sendAsUserAttribute = json.optBoolean(SEND_AS_USER_ATTRIBUTE, false)
        validationRule = json.optBoolean(VALIDATION_RULE, false)
    }
}