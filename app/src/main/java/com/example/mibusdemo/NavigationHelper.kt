package com.example.mibusdemo

import android.app.Activity
import android.content.Intent
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * Ayudante para centralizar la navegación y asegurar que el menú sea igual en todas las pantallas.
 */
object NavigationHelper {

    fun setupBottomNavigation(activity: Activity, selectedItemId: Int) {
        val bottomNav = activity.findViewById<BottomNavigationView>(R.id.bottom_navigation) ?: return
        
        // Forzamos un estilo consistente
        bottomNav.selectedItemId = selectedItemId
        bottomNav.setBackgroundResource(android.R.color.white)

        bottomNav.setOnItemSelectedListener { item ->
            if (item.itemId == selectedItemId) return@setOnItemSelectedListener true

            val targetActivity = when (item.itemId) {
                R.id.nav_inicio -> principal::class.java
                R.id.nav_rutas -> TodasLasRutas::class.java
                R.id.nav_favoritos -> AltertasFavs::class.java
                else -> null
            }

            targetActivity?.let {
                val intent = Intent(activity, it)
                // FLAG_ACTIVITY_REORDER_TO_FRONT evita crear múltiples instancias y mantiene el estado
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                activity.startActivity(intent)
                
                // Quitamos las animaciones para que la transición sea instantánea y parezca la misma app
                activity.overridePendingTransition(0, 0)
                true
            } ?: false
        }
    }
}
