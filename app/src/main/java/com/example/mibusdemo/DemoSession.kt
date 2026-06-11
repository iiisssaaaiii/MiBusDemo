package com.example.mibusdemo

import android.content.Context
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

/**
 * Gestión de sesión y datos persistentes del usuario.
 * Se unifica la lógica de SharedPreferences local con la sincronización en Firebase.
 */
object DemoSession {
    private const val PREFS_NAME = "demo_session_prefs"
    private const val KEY_NAME = "name"
    private const val KEY_EMAIL = "email"
    private const val KEY_PASSWORD = "password"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_FAVORITES = "favorites"
    private const val KEY_FAVORITE_ROUTES = "rutas_favoritas"

    fun isLoggedIn(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun login(context: Context, email: String, password: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        // Soporte para usuario demo
        if (email == "demo@mibus.com" && password == "123456") {
            prefs.edit().putBoolean(KEY_IS_LOGGED_IN, true).apply()
            return true
        }

        // Lógica de validación local
        val savedEmail = prefs.getString(KEY_EMAIL, null)
        val savedPassword = prefs.getString(KEY_PASSWORD, null)
        val valid = savedEmail == email && savedPassword == password
        
        if (valid) {
            prefs.edit().putBoolean(KEY_IS_LOGGED_IN, true).apply()
        }
        return valid
    }

    fun register(context: Context, name: String, email: String, password: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_NAME, name)
            .putString(KEY_EMAIL, email)
            .putString(KEY_PASSWORD, password)
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .apply()
    }

    fun addFavorite(context: Context, name: String, lat: Double, lng: Double) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getStringSet(KEY_FAVORITES, emptySet()).orEmpty().toMutableSet()
        current.add("$name|$lat|$lng")
        prefs.edit().putStringSet(KEY_FAVORITES, current).apply()

        // Sincronizar con Firestore
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
                .document(name)
                .set(favoriteData, SetOptions.merge())
                .addOnFailureListener {
                    Toast.makeText(context, "Error al sincronizar con la nube", Toast.LENGTH_SHORT).show()
                }
        }
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

    fun favorites(context: Context): List<FavoritePlace> {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getStringSet(KEY_FAVORITES, emptySet())
            .orEmpty()
            .mapNotNull { raw ->
                val parts = raw.split("|")
                if (parts.size != 3) return@mapNotNull null
                FavoritePlace(parts[0], parts[1].toDoubleOrNull() ?: 0.0, parts[2].toDoubleOrNull() ?: 0.0)
            }
            .sortedBy { it.name }
    }

    fun logout(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_IS_LOGGED_IN, false).apply()
    }
}

data class FavoritePlace(
    val name: String,
    val lat: Double,
    val lng: Double
)
