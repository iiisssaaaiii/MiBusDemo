package com.example.mibusdemo

import android.content.Context
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

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
        val savedEmail = prefs.getString(KEY_EMAIL, null)
        val savedPassword = prefs.getString(KEY_PASSWORD, null)
        val valid = savedEmail == email && savedPassword == password
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
            }
            .sortedBy { it.name }
    }
}

data class FavoritePlace(
    val name: String,
    val lat: Double,
    val lng: Double
)
