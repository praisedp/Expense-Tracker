package com.example.expencetracker.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Utility class for handling app preferences
 */
class PrefUtil(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    
    companion object {
        private const val PREF_NAME = "expense_tracker_prefs"
        private const val FIRST_TIME_LAUNCH = "first_time_launch"
        private const val PIN = "app_pin"
        private const val APP_LOCK = "app_lock_enabled"
    }
    
    /**
     * Set whether this is the first time the app has been launched
     * @param isFirstTime whether this is the first time launch
     */
    fun setFirstTimeLaunch(isFirstTime: Boolean) {
        prefs.edit().putBoolean(FIRST_TIME_LAUNCH, isFirstTime).apply()
    }
    
    /**
     * Check if this is the first time the app has been launched
     * @return true if first time launch, false otherwise
     */
    fun isFirstTimeLaunch(): Boolean {
        return prefs.getBoolean(FIRST_TIME_LAUNCH, true)
    }
    
    /**
     * Save the PIN for app lock
     * @param pin the PIN to save
     */
    fun savePIN(pin: String) {
        prefs.edit().putString(PIN, pin).apply()
    }
    
    /**
     * Get the saved PIN
     * @return the saved PIN or empty string if none
     */
    fun getPIN(): String {
        return prefs.getString(PIN, "") ?: ""
    }
    
    /**
     * Enable or disable app lock
     * @param enabled whether app lock should be enabled
     */
    fun setAppLock(enabled: Boolean) {
        prefs.edit().putBoolean(APP_LOCK, enabled).apply()
    }
    
    /**
     * Check if app lock is enabled
     * @return true if app lock is enabled, false otherwise
     */
    fun isAppLockEnabled(): Boolean {
        return prefs.getBoolean(APP_LOCK, false)
    }
    
    /**
     * Clear all preferences
     */
    fun clearPreferences() {
        prefs.edit().clear().apply()
    }
} 