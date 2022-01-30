package com.ibm.app

import android.content.Context
import com.ibm.airlytics.events.ALEvent
import com.ibm.airlytics.providers.ALProvider
import com.ibm.airlytics.providers.data.ALProviderConfig

class FileSystemProviderHandler(private var providerConfig: ALProviderConfig) :
    ALProvider {

    override fun init(context: Context?) {
        println("init called name: ${providerConfig.id} type: ${providerConfig.type} connection: ${providerConfig.connection} description: ${providerConfig.description}")
    }

    override fun send(event: ALEvent): Boolean {
        println("file system send called")
        return true
    }
}