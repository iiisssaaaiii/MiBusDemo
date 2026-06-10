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

class ParadasCercanasActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_paradas_cercanas)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Configurar clic en barra de búsqueda para ir a Planificar
        findViewById<EditText>(R.id.etSearch).setOnClickListener {
            startActivity(Intent(this, PlanificarViaje::class.java))
        }

        // Tarea 1.2: Inicializar cliente de ubicación
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Tarea 3.1: Inicializar Mapa
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
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
            return
        }

        mMap.isMyLocationEnabled = true
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val miUbicacion = LatLng(location.latitude, location.longitude)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(miUbicacion, 15f))
            }
        }
    }

    private fun cargarParadasDesdeJson() {
        val listaDeParadas = mutableListOf<Parada>() // Creamos una lista vacía
        try {
            val jsonString =
                assets.open("middleware_base_rutas.json").bufferedReader().use { it.readText() }
            val rutasArray = org.json.JSONArray(jsonString)

            for (i in 0 until rutasArray.length()) {
                val ruta = rutasArray.getJSONObject(i)
                if (!ruta.isNull("paradas_geojson")) {
                    val features = ruta.getJSONObject("paradas_geojson").getJSONArray("features")
                    for (j in 0 until features.length()) {
                        val feature = features.getJSONObject(j)
                        val coords = feature.getJSONObject("geometry").getJSONArray("coordinates")
                        val lat = coords.getDouble(1)
                        val lng = coords.getDouble(0)
                        val idParada = feature.getJSONObject("properties").getString("id")

                        // 1. Creamos el objeto Parada y lo metemos a la lista
                        listaDeParadas.add(Parada("Parada $idParada", lat, lng))

                        // 2. Ponemos el marcador en el mapa
                        mMap.addMarker(
                            MarkerOptions().position(LatLng(lat, lng)).title("ID: $idParada")
                        )
                    }
                }
            }

            // 3. CONECTAMOS CON EL RECYCLERVIEW
            val rv = findViewById<RecyclerView>(R.id.rvParadas)
            rv.layoutManager = LinearLayoutManager(this) // Sirve para que la lista sea vertical
            rv.adapter = ParadaAdapter(listaDeParadas)    // Le pasamos los datos al adaptador

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

