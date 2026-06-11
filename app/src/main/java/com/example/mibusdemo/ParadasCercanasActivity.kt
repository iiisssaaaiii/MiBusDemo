package com.example.mibusdemo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mibusdemo.data.dto.Parada
import com.example.mibusdemo.data.dto.ParadaAdapter
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.json.JSONArray

class ParadasCercanasActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_paradas_cercanas)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        findViewById<EditText>(R.id.etSearch).setOnClickListener {
            startActivity(Intent(this, PlanificarViaje::class.java))
        }
        configurarMenuInferior()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun configurarMenuInferior() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_inicio
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_inicio -> true
                R.id.nav_rutas -> {
                    true
                }
                R.id.nav_favoritos -> {
                    Toast.makeText(this, "Favoritos y alertas lo esta trabajando tu companero", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        verificarPermisosYUbicar()
        cargarParadasDesdeJson()
    }

    private fun verificarPermisosYUbicar() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1000
            )
            moverCamaraAXalapa()
            return
        }

        mMap.isMyLocationEnabled = true
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val miUbicacion = LatLng(location.latitude, location.longitude)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(miUbicacion, 15f))
            } else {
                moverCamaraAXalapa()
            }
        }
    }

    private fun moverCamaraAXalapa() {
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(19.5438, -96.9101), 13f))
    }

    private fun cargarParadasDesdeJson() {
        val listaDeParadas = mutableListOf<Parada>()
        try {
            val jsonString = assets.open("middleware_base_rutas.json").bufferedReader().use { it.readText() }
            val rutasArray = JSONArray(jsonString)

            for (i in 0 until rutasArray.length()) {
                val ruta = rutasArray.getJSONObject(i)
                val features = ruta.optJSONObject("paradas_geojson")
                    ?.optJSONArray("features")
                    ?: continue

                for (j in 0 until features.length()) {
                    val feature = features.getJSONObject(j)
                    val coords = feature.getJSONObject("geometry").getJSONArray("coordinates")
                    val lat = coords.getDouble(1)
                    val lng = coords.getDouble(0)
                    val idParada = feature.optJSONObject("properties")?.optString("id").orEmpty()
                    val nombreParada = "Parada $idParada"

                    listaDeParadas.add(Parada(nombreParada, lat, lng))
                    mMap.addMarker(MarkerOptions().position(LatLng(lat, lng)).title(nombreParada))
                }
            }

            val rv = findViewById<RecyclerView>(R.id.rvParadas)
            rv.layoutManager = LinearLayoutManager(this)
            rv.adapter = ParadaAdapter(listaDeParadas.take(12))
        } catch (e: Exception) {
            Toast.makeText(this, "No se pudieron cargar las paradas", Toast.LENGTH_SHORT).show()
        }
    }
}
