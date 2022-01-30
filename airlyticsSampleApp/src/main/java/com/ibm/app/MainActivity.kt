package com.ibm.app

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.ibm.airlock.common.AirlockCallback
import com.ibm.airlock.common.data.Feature
import com.ibm.airlock.sdk.AirlockManager
import com.ibm.airlytics.AL
import com.ibm.airlytics.AL.Companion.setUserAttributeGroups
import com.ibm.airlytics.environments.ALEnvironment
import com.ibm.airlytics.environments.ALEnvironmentConfig
import com.ibm.airlytics.events.ALEvent
import com.ibm.airlytics.events.ALEventConfig
import com.ibm.airlytics.providers.data.ALProviderConfig
import org.json.JSONArray
import org.json.JSONObject
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {

    private var installedDate: Long = 0

    companion object {
        lateinit var environment: ALEnvironment
        var providerConfigs: MutableList<ALProviderConfig> = ArrayList()
        val eventConfigs: MutableList<ALEventConfig> = ArrayList()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        try {
            AirlockManager.getInstance()
                .initSDK(applicationContext, R.raw.al_airlock_defaults, "10.17")
            AirlockManager.getInstance().deviceUserGroups = listOf("QA")
            AL.registerProviderHandlers(
                mapOf(
                    "REST_EVENT_PROXY" to "com.ibm.airlytics.providers.RestEventProxyProvider",
                    "DEBUG_BANNERS" to "com.ibm.airlytics.providers.DebugBannersProvider",
                    "EVENT_LOG" to "com.ibm.airlytics.providers.EventLogProvider"
                )
            )
            AirlockManager.getInstance().pullFeatures(object : AirlockCallback {
                override fun onFailure(e: Exception) {
                    e.message
                }

                override fun onSuccess(s: String) {
                    AirlockManager.getInstance()
                        .calculateFeatures(null as JSONObject?, null as JSONObject?)
                    AirlockManager.getInstance().syncFeatures()
                    loadFeaturesFromAirlockAndUpdateAirlytics()
                    val attributeGroupsFeature: Feature =
                        AirlockManager.getInstance().getFeature("analytics.User Attributes Grouping")
                    val attributeGroupsConfig =
                        attributeGroupsFeature.configuration
                    var groupsArray: JSONArray? = null
                    if (attributeGroupsConfig != null) {
                        groupsArray = attributeGroupsConfig.optJSONArray("userAttributesGrouping")
                    }
                    groupsArray?.let {
                        setUserAttributeGroups(it)
                    }
                    environment.setUserAttributes(
                        mapOf(
                            "premium" to true,
                            "upsId" to "ups12345",
                            "devUser" to true
                        ),"1.0"
                    )
                    environment.setUserAttributes(
                        mapOf(
                            "premium" to false,
                            "upsId" to "ups12345",
                            "devUser" to true
                        ),"1.0"
                    )
                }
            })


        } catch (e: Exception) {
            println(e.message)
        }
    }

    fun track(view: View) {
//        environment.track()
//        if (installedDate == 0L) {
//            installedDate = System.currentTimeMillis() - 1000000
//        }
        AL.verifyLifecycleStarted()
//        var providers: MutableList<ALProviderConfig> = environment.providers.values.toMutableList()
//        var providers : MutableList<ALProviderConfig> = ArrayList(environment.providers.values)
//        var providers: MutableList<ALProviderConfig> = ArrayList()
//        for (providerItem in environment.providers.values){
//            providers.add(providerItem.clone())
//        }
//        var events: MutableList<ALEventConfig> = environment.eventsMap.values.toMutableList()
//        providers.removeAt(0)
//        providers[0].trackingPolicyConfigs?.intervals?.set("fast", 3)
//        events.removeAt(0)
//
//        environment = AL.updateEnvironment(environment.config, providers, events, "e79603a0-314b-4642-a765-8ece6732caf3", UUID.fromString("e79603a0-314b-4642-a765-8ece6732caf3"), this@MainActivity, mapOf("REST_EVENT_PROXY" to "com.ibm.airlytics.providers.RestEventProxyProvider"), null, true)
//
//        environment.disableEnvironment()
//        environment.resendUserAttributes()

        /*
                name: String,
        attributes: MutableMap<String, Any?>,
        eventTime: Long,
        environment: ALEnvironment,
        schemaVersion: String?
        */

        var attributes: MutableMap<String, Any?> = mutableMapOf(
            "assetId" to "ba8e470b-7ab9-4c62-8e8d-50a15032ae56",
        "source" to "videoCard",
        "pos" to 2,
        "title" to "Dog Sliding Down a Snowy Hill Is Everything You Need",
        "titleType" to "teaser",
        "displayedTitle" to "This Dog in the Snow Is What We Need Right Now",
        "timeInView" to 5305
        )

        val numberOfThreads = 10
        val service: ExecutorService = Executors.newFixedThreadPool(numberOfThreads)
        for (i in 0 until 1500) {
            service.execute {
                environment.track(ALEvent("asset-viewed", attributes, System.currentTimeMillis(), environment, "5.0"))
            }
        }

        environment.sendEventsWhenGoingToBackground()

//        val testObject = JSONObject()
//        testObject.put("strA",  "g")
//        testObject.put("strb",  "123")
//        testObject.put("numA",  3)
//        testObject.put("booleanC",  true)
//
//        environment.setUserAttributes(
//            mapOf(
//                "test1Attribute" to testObject,
//                "nullValue" to null
//            ),"1.0"
//        )
//
//        environment.setUserAttributes(
//            mapOf(
//                "a" to 1,
//                "b" to 2,
//                "c" to 3,
//                "d" to 4
//            ),"1.0"
//        )
//
//        environment.setUserAttributes(
//            mapOf(
//                "a" to 99
//            ),"1.0"
//        )
    }

    fun loadFeaturesFromAirlockAndUpdateAirlytics() {
        val providersFeature: Feature =
            AirlockManager.getInstance().getFeature(AirlockConstants.Constants.Analytics.PROVIDERS)
        val providers = providersFeature.children
        for (providerFeature in providers) {
            var config: JSONObject? = providerFeature.configuration
            config?.put("debugUser", true)
            if (config != null) {
                var events = config.optJSONArray("events")
                if (events != null) {
                    for (i in 0 until events.length()) {
                        var event = events.getJSONObject(i)
                        if (event.optString("name") == "asset-viewed") {
                            events.getJSONObject(i).put("realTime", true)
                        }
                    }
                }
                providerConfigs.add(
                    ALProviderConfig(
                        config
                    )
                )
            }
        }

        //read and initialize events
        val eventsFeature: Feature =
            AirlockManager.getInstance().getFeature(AirlockConstants.Constants.Analytics.EVENTS)
        val events = eventsFeature.children
        for (eventFeature in events) {
            var config: JSONObject? = eventFeature.configuration
            if (config != null) {
                eventConfigs.add(ALEventConfig(config))
            }
        }

        //read and initialize environments
        val environmentFeature: Feature =
            AirlockManager.getInstance()
                .getFeature(AirlockConstants.Constants.Analytics.ENVIRONMENTS)
        val environments = environmentFeature.children
        for (environmentFeatureItem in environments) {
            val config: JSONObject? = environmentFeatureItem.configuration
            if (config != null) {
//                            AL.registerProviderHandlers(mapOf("REST_EVENT_PROXY" to "com.ibm.airlytics.providers.RestEventProxyProvider"))
//                val userAttributes = mapOf("isPremium" to false, "pushToken" to "TestId999 : optional. can be a very long string, no specific format. It can also be null in case the pushId has been cancelled" ,
//                    "upsId" to "ups12345",
//                    "installDate" to System.currentTimeMillis() - 1000000)
                var envConfig = ALEnvironmentConfig(config)
                if (envConfig.name != "external-dev") {
                    continue
                }
                envConfig.isDebugUser = true

                environment = AL.createEnvironment(
                    envConfig,
                    providerConfigs,
                    eventConfigs,
                    "e79603a0-314b-4642-a765-8ece6732ceee",
                    UUID.fromString("bc479db5-ff58-4138-b5e4-a8400a1f78d5"),
                    "10.27",
                    this@MainActivity
                )
            }
        }
    }
}
