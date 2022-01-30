package com.ibm.airlytics.providers.data

import org.json.JSONObject

/**
 * Class for defining the general event config of a provider
 */
class ALProviderEventConfig (config: JSONObject?) {

    var id: String = ""
    var realtime: Boolean = false

    companion object{
        const val ID = "name"
        const val REALTIME = "realTime"
    }

    init {
        if (config != null){
            fromJSON(config)
        }
    }

    private fun fromJSON(jsonObject: JSONObject) {
        id = jsonObject.optString(ID)
        realtime = jsonObject.optBoolean(REALTIME)
    }

}