package com.weather.airlytics.persistence

interface Cache {
    fun setValue(key: String, value: String)
    fun getValue(key: String): String?
    fun getAllValues(): Map<String, String>
    fun remove(key: String)
    fun clear()
    fun containsKey(key: String): Boolean {
        return true
    }
}