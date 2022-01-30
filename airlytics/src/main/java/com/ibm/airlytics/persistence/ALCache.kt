package com.ibm.airlytics.persistence

import java.util.concurrent.ConcurrentHashMap

open class ALCache(val name: String) : Cache {

    var mapValues: MutableMap<String, String> = ConcurrentHashMap()

    override fun setValue(key: String, value: String) {
        mapValues[key] = value
    }

    override fun getValue(key: String): String? {
        return mapValues[key]
    }

    override fun getAllValues(): Map<String, String> {
        return mapValues
    }

    override fun remove(key: String) {
        mapValues.remove(key)
    }

    override fun clear() {
        mapValues.clear()
    }

    override fun containsKey(key: String): Boolean {
        return mapValues.containsKey(key)
    }

}