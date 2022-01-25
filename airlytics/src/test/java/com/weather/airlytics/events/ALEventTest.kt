package com.weather.airlytics.events

import com.weather.airlytics.environments.ALEnvironment
import com.weather.airlytics.environments.ALEnvironmentConfig
import org.junit.Assert
import org.junit.Test
import java.util.*
import kotlin.collections.HashMap

class ALEventTest {


    @Test
    fun constructEventTest() {
        try {
            val environment = ALEnvironment(ALEnvironmentConfig())
            ALEvent("user-attributes", UUID.randomUUID(), environment, System.currentTimeMillis(), "1.0", false, HashMap())
            ALEvent("user-attributes", HashMap(), System.currentTimeMillis(), environment, "1.0")
        }catch (ex : Exception){
            Assert.fail(ex.message)
        }
    }
}