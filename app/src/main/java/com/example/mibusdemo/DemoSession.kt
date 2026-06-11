package com.example.mibusdemo

import android.content.Context

class DemoSession {
    companion object {
        private const val PREFS_NAME = "demo_session_prefs"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_FAVORITE_ROUTES = "rutas_favoritas"

        fun isLoggedIn(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        }

        fun login(context: Context, email: String, password: String): Boolean {
            if (email == "demo@mibus.com" && password == "123456") {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().putBoolean(KEY_IS_LOGGED_IN, true).apply()
                return true
            }
            return false
        }

        fun register(context: Context, name: String, email: String, password: String) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putString("name", name)
                .putString("email", email)
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .apply()
        }

        fun addFavorite(context: Context, name: String, lat: Double, lng: Double) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val favorites = prefs.getStringSet("favorites", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
            favorites.add("$name|$lat|$lng")
            prefs.edit().putStringSet("favorites", favorites).apply()
        }

        fun addFavoriteRoute(context: Context, origen: String, destino: String) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val favorites = prefs.getStringSet(KEY_FAVORITE_ROUTES, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
            favorites.add("$origen -> $destino")
            prefs.edit().putStringSet(KEY_FAVORITE_ROUTES, favorites).apply()
        }

        fun getFavoriteRoutes(context: Context): List<String> {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getStringSet(KEY_FAVORITE_ROUTES, emptySet())?.toList() ?: emptyList()
        }

        fun logout(context: Context) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_IS_LOGGED_IN, false).apply()
        }
    }
}
