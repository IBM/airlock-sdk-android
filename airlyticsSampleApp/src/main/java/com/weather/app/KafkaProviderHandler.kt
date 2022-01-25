package com.weather.app

import android.content.Context
import android.os.AsyncTask
import com.weather.airlytics.events.ALEvent
import com.weather.airlytics.providers.ALProvider
import com.weather.airlytics.providers.data.ALProviderConfig
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONArray
import org.json.JSONObject


class KafkaProviderHandler(private var providerConfig: ALProviderConfig) :
    ALProvider {

    private var client = OkHttpClient()
    private var urlString = ""
        set(value) {         // setter
            field = value
            urlSetExplicitly = true
        }
    private var apiKey = ""
    private var urlSetExplicitly = false

    override fun init(context: Context?) {
        println("init called name: ${providerConfig.id} type: ${providerConfig.type} connection: ${providerConfig.connection} description: ${providerConfig.description}")
    }

    override fun send(event: ALEvent): Boolean {
        if (providerConfig.connection == null){
            urlString = "http://ec2-3-94-123-14.compute-1.amazonaws.com:8081/eventproxy/track"
            apiKey = "temp"
        }else{
            val connection = providerConfig.connection as JSONObject
            urlString = "http://ec2-3-94-123-14.compute-1.amazonaws.com:8081/eventproxy/track"
            if (!urlString.startsWith("http://")){
                urlString = "http://$urlString"
            }
            apiKey = connection.optString("api-key")
        }

        urlString = urlString.replace("localhost", "192.168.2.17" )
        val eventsJson = JSONObject()
        val eventsArray = JSONArray()
        eventsArray.put(event.toJSONForSend())
//        eventsArray.put(JSONObject())
        eventsJson.put("events", eventsArray)
        val request = Request.Builder()
            .url(urlString)
            .addHeader("x-api-key",apiKey)
            .post(RequestBody
            .create(MediaType
                .parse("application/json"),
                eventsJson.toString()))
            .build()
        AsyncTask.execute {
            client.newCall(request).execute().use {
                    response ->
                println( response.body().string())
            }
        }
        return true
    }
}