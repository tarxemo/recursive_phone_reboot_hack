package com.example.phonerebooter.util

import android.content.Context
import android.content.SharedPreferences

class PreferenceHelper(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("PhoneRebooterPrefs", Context.MODE_PRIVATE)

    var isServiceEnabled: Boolean
        get() = prefs.getBoolean("service_enabled", false)
        set(value) = prefs.edit().putBoolean("service_enabled", value).apply()

    var lastResetTime: Long
        get() = prefs.getLong("last_reset_time", System.currentTimeMillis())
        set(value) = prefs.edit().putLong("last_reset_time", value).apply()

    fun resetTimer() {
        lastResetTime = System.currentTimeMillis()
    }
}
