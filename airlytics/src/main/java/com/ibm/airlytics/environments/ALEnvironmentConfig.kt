package com.ibm.airlytics.environments

import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicInteger

/**
 * Class that holds the environment configuration
 */
class ALEnvironmentConfig() {
    companion object {
        const val NAME = "name"
        const val DESCRIPTION = "description"
        const val TAGS = "tags"
        const val PROVIDERS = "providers"
        const val ENABLE_CLIENT_SIDE_VALIDATION = "enableClientSideValidation"
        const val SESSION_EXPIRATION_IN_SECONDS = "sessionExpirationInSeconds"
        const val SESSION_EXPIRATION_DEFAULT_VALUE = 5
        const val DEBUG_USER = "debugUser"
        const val SHOULD_SEND_EMPTY_SESSIONS = "shouldSendEmptySessions"
        const val RESEND_USER_ATTRIBUTES_INTERVAL_IN_SECONDS =
            "resendUserAttributesIntervalInSeconds"
    }

    var name: String = ""
    var description: String = ""
    var providers: MutableList<String>? = null
    var tags: MutableSet<String> = HashSet()
    var enableClientSideValidation = true
    var sessionExpirationInSeconds: AtomicInteger = AtomicInteger(SESSION_EXPIRATION_DEFAULT_VALUE)
    var isDebugUser: Boolean = false
    var shouldSendEmptySessions: Boolean = false
    var resendUserAttributesIntervalInSeconds = 0

    init {
        providers = ArrayList()
    }

    //providers,name,description
    constructor(config: JSONObject) : this() {
        name = config.optString(NAME)
        description = config.optString(DESCRIPTION)
        enableClientSideValidation = config.optBoolean(ENABLE_CLIENT_SIDE_VALIDATION)
        sessionExpirationInSeconds.set(
            config.optInt(
                SESSION_EXPIRATION_IN_SECONDS,
                SESSION_EXPIRATION_DEFAULT_VALUE
            )
        )
        val tagsArray: JSONArray? = config.optJSONArray(TAGS)
        if (tagsArray != null) {
            for (x in 0 until tagsArray.length()) {
                tags.add(tagsArray.get(x) as String)
            }
        }

        val providersArray = config.optJSONArray(PROVIDERS)
        if (providersArray != null) {
            for (i in 0 until providersArray.length()) {
                providers?.add(providersArray[i] as String)
            }
        }
        this.isDebugUser = config.optBoolean(DEBUG_USER)
        this.shouldSendEmptySessions = config.optBoolean(SHOULD_SEND_EMPTY_SESSIONS)
        this.resendUserAttributesIntervalInSeconds = config.optInt(
            RESEND_USER_ATTRIBUTES_INTERVAL_IN_SECONDS, 0
        )
    }
}
