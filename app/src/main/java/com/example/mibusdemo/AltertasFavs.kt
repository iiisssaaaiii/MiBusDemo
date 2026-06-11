package com.example.mibusdemo

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class AltertasFavs : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_altertas_favs)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        configurarListaFavoritos()
        configurarListaAlertas()
        
        NavigationHelper.setupBottomNavigation(this, R.id.nav_favoritos)
    }

    private fun configurarListaFavoritos() {
        val listView = findViewById<ListView>(R.id.listView)
        
        // Combinamos favoritos de lugares (lat/lng) y rutas guardadas
        val lugaresFavoritos = DemoSession.favorites(this).map { it.name }
        val rutasFavoritas = DemoSession.getFavoriteRoutes(this)
        val listaTotal = lugaresFavoritos + rutasFavoritas

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            listaTotal
        )
        listView.adapter = adapter
    }

    private fun configurarListaAlertas() {
        val listViewAlertas = findViewById<ListView>(R.id.listViewAlertas)
        val alertas = listOf("Reparación en tesorería", "Reparación en Av. Xalapa", "Bloqueo en Zona UV")

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            alertas
        )
        listViewAlertas.adapter = adapter
    }
}
