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

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, 0)
            insets
        }

        findViewById<EditText>(R.id.etSearch).setOnClickListener {
            startActivity(Intent(this, PlanificarViaje::class.java))
        }

        NavigationHelper.setupBottomNavigation(this, R.id.nav_inicio)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }



    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = false
        moverCamaraAXalapa()
        verificarPermisosYUbicar()
        cargarParadasDesdeJson()
    }

    private fun verificarPermisosYUbicar() {
        val fineLocationGranted = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineLocationGranted) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST
            )
            return
        }

        mMap.isMyLocationEnabled = true
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val miUbicacion = LatLng(location.latitude, location.longitude)
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(miUbicacion, 15f))
            }
        }
    }

    private fun moverCamaraAXalapa() {
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(XALAPA_CENTRO, 13f))
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
                    val geometry = feature.getJSONObject("geometry")
                    if (geometry.optString("type") != "Point") continue

                    val coords = geometry.getJSONArray("coordinates")
                    val lat = coords.getDouble(1)
                    val lng = coords.getDouble(0)
                    val idParada = feature.optJSONObject("properties")?.optString("id").orEmpty()
                    val nombreParada = "Parada $idParada"
                    val posicion = LatLng(lat, lng)

                    listaDeParadas.add(Parada(nombreParada, lat, lng))
                    mMap.addMarker(MarkerOptions().position(posicion).title(nombreParada))
                }
            }

            findViewById<RecyclerView>(R.id.rvParadas).apply {
                layoutManager = LinearLayoutManager(this@ParadasCercanasActivity)
                adapter = ParadaAdapter(listaDeParadas.take(20))
            }
        } catch (e: Exception) {
            Toast.makeText(this, "No se pudieron cargar las paradas", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 1000
        private val XALAPA_CENTRO = LatLng(19.5438, -96.9101)
    }
}
