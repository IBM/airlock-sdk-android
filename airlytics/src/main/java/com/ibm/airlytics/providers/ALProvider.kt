package com.ibm.airlytics.providers

import android.content.Context
import com.ibm.airlytics.events.ALEvent
import com.ibm.airlytics.providers.data.ALProviderConfig
import java.util.*

/**
 * Provider interface class
 */
interface ALProvider {
    fun init(context: Context?)
    fun send(event: ALEvent): Boolean
    fun reset() {}
    fun sendEventsWhenGoingToBackground() {}
    fun interval(config: ALProviderConfig) {}
    fun clearSessionEvents(sessionUUID: UUID) {}
}