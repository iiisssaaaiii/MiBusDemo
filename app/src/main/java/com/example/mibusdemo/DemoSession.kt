package com.example.mibusdemo

import android.content.Context

object DemoSession {
    private const val PREFS = "mi_bus_demo_session"
    private const val KEY_NAME = "name"
    private const val KEY_EMAIL = "email"
    private const val KEY_PASSWORD = "password"
    private const val KEY_LOGGED_IN = "logged_in"
    private const val KEY_FAVORITES = "favorites"

    fun register(context: Context, name: String, email: String, password: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_NAME, name)
            .putString(KEY_EMAIL, email)
            .putString(KEY_PASSWORD, password)
            .putBoolean(KEY_LOGGED_IN, true)
            .apply()
    }

    fun login(context: Context, email: String, password: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val valid = prefs.getString(KEY_EMAIL, null) == email &&
            prefs.getString(KEY_PASSWORD, null) == password

        if (valid) {
            prefs.edit().putBoolean(KEY_LOGGED_IN, true).apply()
        }
        return valid
    }

    fun isLoggedIn(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_LOGGED_IN, false)
    }

    fun addFavorite(context: Context, name: String, lat: Double, lng: Double) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val favorites = prefs.getStringSet(KEY_FAVORITES, emptySet()).orEmpty().toMutableSet()
        favorites.add("$name|$lat|$lng")
        prefs.edit().putStringSet(KEY_FAVORITES, favorites).apply()
    }
}
