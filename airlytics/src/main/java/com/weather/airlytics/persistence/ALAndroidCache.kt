package com.weather.airlytics.persistence

import android.content.Context
import android.content.SharedPreferences

class ALAndroidCache(name: String, context: Context?) : ALCache(name) {
    private var prefs: SharedPreferences? = null

    init {
        prefs = context?.getSharedPreferences("ALCache_$name", Context.MODE_PRIVATE)
        loadFromPrefToMemory()
    }

    private fun loadFromPrefToMemory() {
        prefs?.all?.forEach {
            val key = it.key
            val value = it.value as? String
            if (value != null) {
                mapValues[key] = value
            }
        }
    }

    override fun remove(key: String) {
        super.remove(key)
        prefs?.edit()?.remove(key)?.apply()
    }

    override fun setValue(key: String, value: String) {
        super.setValue(key, value)
        prefs?.edit()?.putString(key, value)?.apply()
    }

}