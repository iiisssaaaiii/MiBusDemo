package com.example.mibusdemo

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mibusdemo.data.dto.RutaDto
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class TodasLasRutas : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private var allRoutes: List<RutaDto> = emptyList()
    private lateinit var searchAdapter: RouteSearchAdapter
    
    private lateinit var etSearch: EditText
    private lateinit var rvSearchResults: RecyclerView
    private lateinit var cvResultInfo: MaterialCardView
    private lateinit var tvLocationName: TextView
    private lateinit var tvLocationAddress: TextView
    private lateinit var ivClose: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_todas_las_rutas)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Inicializar componentes de UI
        etSearch = findViewById(R.id.etSearch)
        rvSearchResults = findViewById(R.id.rvSearchResults)
        cvResultInfo = findViewById(R.id.cvResultInfo)
        tvLocationName = findViewById(R.id.tvLocationName)
        tvLocationAddress = findViewById(R.id.tvLocationAddress)
        ivClose = findViewById(R.id.ivClose)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setupBottomNavigation()
        loadRoutesFromJson()
        setupSearch()
    }

    private fun setupBottomNavigation() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.nav_rutas

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_inicio -> {
                    startActivity(Intent(this, principal::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    })
                    true
                }
                R.id.nav_rutas -> true
                R.id.nav_favoritos -> {
                    startActivity(Intent(this, AltertasFavs::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    })
                    true
                }
                else -> false
            }
        }
    }

    private fun loadRoutesFromJson() {
        try {
            val inputStream = assets.open("middleware_base_rutas.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val listType = object : TypeToken<List<RutaDto>>() {}.type
            allRoutes = Gson().fromJson(jsonString, listType)
        } catch (e: Exception) {
            Log.e("DEBUG_MAP", "Error cargando rutas: ${e.message}")
        }
    }

    private fun setupSearch() {
        searchAdapter = RouteSearchAdapter(emptyList()) { selectedRoute ->
            showSingleRoute(selectedRoute)
        }
        rvSearchResults.layoutManager = LinearLayoutManager(this)
        rvSearchResults.adapter = searchAdapter

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                if (query.length >= 2) {
                    val filtered = allRoutes.filter { route ->
                        val trazadoProps = route.trazadoGeojson.features.firstOrNull { it.geometry.type == "LineString" }?.properties
                        val nameMatch = trazadoProps?.name?.contains(query, ignoreCase = true) == true
                        val descMatch = trazadoProps?.desc?.contains(query, ignoreCase = true) == true
                        
                        // Buscar también en paradas
                        val paradaMatch = route.paradasGeojson?.features?.any { 
                            it.properties.name?.contains(query, ignoreCase = true) == true ||
                            it.properties.desc?.contains(query, ignoreCase = true) == true
                        } == true
                        
                        nameMatch || descMatch || paradaMatch
                    }
                    searchAdapter.updateList(filtered)
                    rvSearchResults.visibility = if (filtered.isNotEmpty()) View.VISIBLE else View.GONE
                    ivClose.visibility = View.VISIBLE
                } else {
                    rvSearchResults.visibility = View.GONE
                    ivClose.visibility = if (query.isEmpty()) View.GONE else View.VISIBLE
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        ivClose.setOnClickListener {
            etSearch.text.clear()
            cvResultInfo.visibility = View.GONE
            rvSearchResults.visibility = View.GONE
            mMap.clear()
            mostrarTodasLasRutas()
        }

        findViewById<ImageView>(R.id.ivBack).setOnClickListener {
            if (cvResultInfo.visibility == View.VISIBLE) {
                ivClose.performClick()
            } else {
                onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    private fun showSingleRoute(route: RutaDto) {
        rvSearchResults.visibility = View.GONE
        etSearch.clearFocus()
        
        val props = route.trazadoGeojson.features.firstOrNull { it.geometry.type == "LineString" }?.properties
        tvLocationName.text = props?.name ?: "Ruta seleccionada"
        tvLocationAddress.text = props?.desc ?: ""
        cvResultInfo.visibility = View.VISIBLE

        mMap.clear()
        val boundsBuilder = LatLngBounds.Builder()
        var hayDatos = false

        // Dibujar el trazado de la ruta (ROJO)
        route.trazadoGeojson.features.forEach { feature ->
            if (feature.geometry.type == "LineString") {
                val polyOptions = PolylineOptions().width(12f).color(Color.RED).geodesic(true)
                @Suppress("UNCHECKED_CAST")
                val coords = feature.geometry.coordinates as? List<List<Double>>
                coords?.forEach { coord ->
                    val ll = LatLng(coord[1], coord[0])
                    polyOptions.add(ll)
                    boundsBuilder.include(ll)
                    hayDatos = true
                }
                mMap.addPolyline(polyOptions)
            }
        }
        
        // Dibujar las paradas de la ruta
        route.paradasGeojson?.features?.forEach { feature ->
            if (feature.geometry.type == "Point") {
                @Suppress("UNCHECKED_CAST")
                val coords = feature.geometry.coordinates as? List<Double>
                if (coords != null && coords.size >= 2) {
                    val pos = LatLng(coords[1], coords[0])
                    mMap.addMarker(MarkerOptions().position(pos).title(feature.properties.name ?: "Parada"))
                    boundsBuilder.include(pos)
                    hayDatos = true
                }
            }
        }

        if (hayDatos) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 150))
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mostrarTodasLasRutas()
    }

    private fun mostrarTodasLasRutas() {
        if (allRoutes.isEmpty()) return
        
        val boundsBuilder = LatLngBounds.Builder()
        var hayPuntos = false

        for (ruta in allRoutes) {
            ruta.trazadoGeojson.features.forEach { feature ->
                if (feature.geometry.type == "LineString") {
                    val polylineOptions = PolylineOptions().width(6f).color(Color.BLUE)
                    @Suppress("UNCHECKED_CAST")
                    val coords = feature.geometry.coordinates as? List<List<Double>>
                    coords?.forEach { coord ->
                        val latLng = LatLng(coord[1], coord[0])
                        polylineOptions.add(latLng)
                        boundsBuilder.include(latLng)
                        hayPuntos = true
                    }
                    mMap.addPolyline(polylineOptions)
                }
            }
        }

        if (hayPuntos) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100))
        }
    }
}
