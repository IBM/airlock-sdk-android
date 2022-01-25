package com.weather.airlytics.providers.implementation

import android.content.Context
import com.weather.airlytics.events.ALEvent
import com.weather.airlytics.providers.ALProvider
import com.weather.airlytics.providers.data.ALProviderConfig

class TestProviderHandler(private var providerConfig: ALProviderConfig) :
    ALProvider {

    override fun init(context: Context?) {
        println("init called name: ${providerConfig.id} type: ${providerConfig.type} connection: ${providerConfig.connection} description: ${providerConfig.description}")
    }

    override fun send(event: ALEvent): Boolean {
        println("send called")
        return true
    }

    override fun reset() {
        println("reset called")
    }

    override fun sendEventsWhenGoingToBackground() {
        println("sendEventsBecauseGoingToBackground called")
    }

}