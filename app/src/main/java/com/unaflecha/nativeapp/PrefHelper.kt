package com.unaflecha.nativeapp

import android.content.Context

object PrefHelper {
    fun setMonitorEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(Constants.PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(Constants.KEY_MONITOR_ENABLED, enabled).apply()
    }

    fun isMonitorEnabled(context: Context): Boolean =
        context.getSharedPreferences(Constants.PREFS, Context.MODE_PRIVATE)
            .getBoolean(Constants.KEY_MONITOR_ENABLED, false)

    fun getLastToken(context: Context): String =
        context.getSharedPreferences(Constants.PREFS, Context.MODE_PRIVATE)
            .getString(Constants.KEY_LAST_TOKEN, "") ?: ""

    fun setLastToken(context: Context, token: String) {
        context.getSharedPreferences(Constants.PREFS, Context.MODE_PRIVATE)
            .edit().putString(Constants.KEY_LAST_TOKEN, token).apply()
    }

    fun getLastTripId(context: Context): Int =
        context.getSharedPreferences(Constants.PREFS, Context.MODE_PRIVATE)
            .getInt(Constants.KEY_LAST_TRIP_ID, 0)

    fun setLastTripId(context: Context, tripId: Int) {
        context.getSharedPreferences(Constants.PREFS, Context.MODE_PRIVATE)
            .edit().putInt(Constants.KEY_LAST_TRIP_ID, tripId).apply()
    }
}
