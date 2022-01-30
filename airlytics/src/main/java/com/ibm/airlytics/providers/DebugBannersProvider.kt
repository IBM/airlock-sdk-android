package com.ibm.airlytics.providers

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.ibm.airlytics.events.ALEvent
import com.ibm.airlytics.providers.data.ALProviderConfig


/**
 * A Provider that shows the event name on a Toast
 * This provider Is for debug
 */
class DebugBannersProvider(private var providerConfig: ALProviderConfig) : ALProvider {

    var context : Context? = null
    override fun init(context: Context?) {
        this.context = context
    }

    override fun send(event: ALEvent): Boolean {
            if (context == null){
                return true
            }

            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context,"\""+ event.name +"\" Tracked",Toast.LENGTH_LONG).show()
            }
        return true
    }

}