package com.example.mibusdemo

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * Ayudante para centralizar la navegación y asegurar que el menú sea igual en todas las pantallas.
 */
object NavigationHelper {

    fun setupBottomNavigation(activity: Activity, selectedItemId: Int) {
        val bottomNav = activity.findViewById<BottomNavigationView>(R.id.bottom_navigation) ?: return
        
        // --- ESTILO UNIFICADO Y VISIBILIDAD ---
        bottomNav.selectedItemId = selectedItemId
        
        // Fondo blanco sólido para evitar transparencias extrañas
        bottomNav.setBackgroundColor(Color.WHITE)
        
        // Configurar colores para estados (Seleccionado vs No seleccionado)
        val states = arrayOf(
            intArrayOf(android.R.attr.state_selected), // Seleccionado
            intArrayOf(-android.R.attr.state_selected) // No seleccionado
        )
        
        val colors = intArrayOf(
            Color.parseColor("#1E88E5"), // Azul vibrante para el seleccionado
            Color.parseColor("#616161")  // Gris oscuro legible para el no seleccionado
        )
        
        val colorStateList = ColorStateList(states, colors)
        
        // Aplicamos los colores a iconos y textos
        bottomNav.itemIconTintList = colorStateList
        bottomNav.itemTextColor = colorStateList
        
        // Aseguramos que siempre se vea el texto (letras) y ajustamos tamaño
        bottomNav.labelVisibilityMode = BottomNavigationView.LABEL_VISIBILITY_LABELED
        
        // Ajustamos el indicador de selección de Material 3 para que sea sutil
        bottomNav.itemActiveIndicatorColor = ColorStateList.valueOf(Color.parseColor("#E3F2FD"))

        // --- LÓGICA DE NAVEGACIÓN ---
        bottomNav.setOnItemSelectedListener { item ->
            if (item.itemId == selectedItemId) return@setOnItemSelectedListener true

            val targetActivity = when (item.itemId) {
                R.id.nav_inicio -> ParadasCercanasActivity::class.java
                R.id.nav_rutas -> TodasLasRutas::class.java
                R.id.nav_favoritos -> AltertasFavs::class.java
                else -> null
            }

            targetActivity?.let {
                val intent = Intent(activity, it)
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                activity.startActivity(intent)
                activity.overridePendingTransition(0, 0) // Transición instantánea para flujo continuo
                true
            } ?: false
        }
    }
}
