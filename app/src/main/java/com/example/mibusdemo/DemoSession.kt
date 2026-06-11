package com.example.mibusdemo

import android.content.Context
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class DemoSession {
    companion object {
        private const val PREFS_NAME = "demo_session_prefs"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_FAVORITE_ROUTES = "rutas_favoritas"

        fun isLoggedIn(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        }

<<<<<<< HEAD
    fun isLoggedIn(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_LOGGED_IN, false)
    }

    fun addFavorite(context: Context, name: String, lat: Double, lng: Double) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val current = prefs.getStringSet(KEY_FAVORITES, emptySet()).orEmpty().toMutableSet()
        current.add("$name|$lat|$lng")
        prefs.edit().putStringSet(KEY_FAVORITES, current).apply()

        // Sincronizar con Firestore si el usuario está autenticado
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val db = FirebaseFirestore.getInstance()
            val favoriteData = mapOf(
                "name" to name,
                "lat" to lat,
                "lng" to lng,
                "timestamp" to System.currentTimeMillis()
            )

            db.collection("users")
                .document(user.uid)
                .collection("favorites")
                .document(name) // Usamos el nombre como ID para evitar duplicados
                .set(favoriteData, SetOptions.merge())
                .addOnFailureListener {
                    Toast.makeText(context, "Error al sincronizar con la nube", Toast.LENGTH_SHORT).show()
                }
        }
    }

    fun favorites(context: Context): List<FavoritePlace> {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY_FAVORITES, emptySet())
            .orEmpty()
            .mapNotNull { raw ->
                val parts = raw.split("|")
                if (parts.size != 3) return@mapNotNull null
                FavoritePlace(parts[0], parts[1].toDoubleOrNull() ?: 0.0, parts[2].toDoubleOrNull() ?: 0.0)
=======
        fun login(context: Context, email: String, password: String): Boolean {
            if (email == "demo@mibus.com" && password == "123456") {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().putBoolean(KEY_IS_LOGGED_IN, true).apply()
                return true
>>>>>>> master
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
