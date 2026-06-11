package com.example.mibusdemo

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView

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

<<<<<<< HEAD
        NavigationHelper.setupBottomNavigation(this, R.id.nav_favoritos)
=======
        configurarListaFavoritos()
        configurarListaAlertas()
        configurarMenuInferior()
    }

    private fun configurarListaFavoritos() {
        val listView = findViewById<ListView>(R.id.listView)
        val rutasFavoritas = DemoSession.getFavoriteRoutes(this)

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            rutasFavoritas
        )
        listView.adapter = adapter
    }

    private fun configurarListaAlertas() {
        val listViewAlertas = findViewById<ListView>(R.id.listViewAlertas)
        val alertas = listOf("Reparacion en tesorería", "Reparacion en Av. Xalapa")

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            alertas
        )
        listViewAlertas.adapter = adapter
    }

    private fun configurarMenuInferior() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.nav_favoritos

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_inicio -> {
                    startActivity(Intent(this, ParadasCercanasActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    })
                    true
                }
                R.id.nav_rutas -> {
                    startActivity(Intent(this, TodasLasRutas::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    })
                    true
                }
                R.id.nav_favoritos -> true
                else -> false
            }
        }
>>>>>>> master
    }
}
