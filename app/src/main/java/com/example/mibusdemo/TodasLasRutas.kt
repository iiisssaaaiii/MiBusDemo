package com.example.mibusdemo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.json.JSONArray

class TodasLasRutas : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_todas_las_rutas)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.nav_rutas

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_inicio -> {
                    startActivity(Intent(this, principal::class.java))
                    true
                }
                R.id.nav_rutas -> true
                R.id.nav_favoritos -> {
                    startActivity(Intent(this, AltertasFavs::class.java))
                    true
                }
                else -> false
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        Log.d("DEBUG_MAP", "Mapa listo, iniciando carga de JSON...")
        cargarParadasDesdeJson()
    }

    private fun cargarParadasDesdeJson() {
        try {
            val inputStream = assets.open("middleware_base_rutas.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonString)

            val boundsBuilder = LatLngBounds.Builder()
            var contadorTotal = 0

            for (i in 0 until jsonArray.length()) {
                val routeObj = jsonArray.getJSONObject(i)
                val trazado = routeObj.getJSONObject("trazado_geojson")
                val features = trazado.getJSONArray("features")

                for (j in 0 until features.length()) {
                    val feature = features.getJSONObject(j)
                    val geometry = feature.getJSONObject("geometry")

                    if (geometry.getString("type") == "Point") {
                        val coords = geometry.getJSONArray("coordinates")
                        val lng = coords.getDouble(0)
                        val lat = coords.getDouble(1)
                        val posicion = LatLng(lat, lng)

                        mMap.addMarker(MarkerOptions().position(posicion).title("Parada"))
                        boundsBuilder.include(posicion)
                        contadorTotal++
                    }
                }
            }

            if (contadorTotal > 0) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100))
            }
        } catch (e: Exception) {
            Log.e("DEBUG_MAP", "ERROR FATAL: " + e.message)
            e.printStackTrace()
        }
    }
}
