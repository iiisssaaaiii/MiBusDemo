package com.example.mibusdemo

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
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
import kotlin.math.*

class TodasLasRutas : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private var allRoutes: List<RutaDto> = emptyList()
    private var currentFilteredRoutes: List<RutaDto> = emptyList()
    private lateinit var searchAdapter: RouteSearchAdapter
    private var currentSelectedRoute: RutaDto? = null
    
    private lateinit var etSearch: EditText
    private lateinit var rvSearchResults: RecyclerView
    private lateinit var cvResultInfo: MaterialCardView
    private lateinit var tvLocationName: TextView
    private lateinit var tvLocationAddress: TextView
    private lateinit var ivClose: ImageView
    private lateinit var btnFiltrar: Button

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
        btnFiltrar = findViewById(R.id.btnFiltrar)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        NavigationHelper.setupBottomNavigation(this, R.id.nav_rutas)
        loadRoutesFromJson()
        setupSearch()
        setupFilterMenu()
    }

    private fun loadRoutesFromJson() {
        try {
            val inputStream = assets.open("middleware_base_rutas.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val listType = object : TypeToken<List<RutaDto>>() {}.type
            allRoutes = Gson().fromJson(jsonString, listType)
            currentFilteredRoutes = allRoutes
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
                    currentFilteredRoutes = allRoutes.filter { route ->
                        val props = route.trazadoGeojson.features.firstOrNull()?.properties
                        val nameMatch = props?.name?.contains(query, ignoreCase = true) == true
                        val descMatch = props?.desc?.contains(query, ignoreCase = true) == true
                        
                        val paradaMatch = route.paradasGeojson?.features?.any { 
                            it.properties.name?.contains(query, ignoreCase = true) == true ||
                            it.properties.desc?.contains(query, ignoreCase = true) == true
                        } == true
                        
                        nameMatch || descMatch || paradaMatch
                    }
                    searchAdapter.updateList(currentFilteredRoutes)
                    rvSearchResults.visibility = if (currentFilteredRoutes.isNotEmpty()) View.VISIBLE else View.GONE
                    ivClose.visibility = View.VISIBLE
                } else {
                    currentFilteredRoutes = allRoutes
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
            currentSelectedRoute = null
            mMap.clear()
            mostrarTodasLasRutas()
        }
    }

    private fun setupFilterMenu() {
        btnFiltrar.setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            popup.menu.add(0, 1, 0, "Menor tiempo de recorrido")
            popup.menu.add(0, 2, 1, "Menor número de transbordos")
            
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> applyOptimization("tiempo")
                    2 -> applyOptimization("transbordos")
                }
                true
            }
            popup.show()
        }
    }

    private fun applyOptimization(type: String) {
        if (currentFilteredRoutes.isEmpty()) {
            Toast.makeText(this, "No hay rutas disponibles para optimizar", Toast.LENGTH_SHORT).show()
            return
        }

        val bestRoute = when (type) {
            "tiempo" -> {
                // Menor tiempo basado en distancia total y frecuencia (midday)
                currentFilteredRoutes.minByOrNull { route ->
                    val dist = calcularDistanciaTotalRuta(route)
                    val freq = route.trazadoGeojson.features.firstOrNull()?.properties?.midday ?: 15
                    // Tiempo est. = (Distancia / 300 m/min) + (frecuencia / 2)
                    (dist / 300.0) + (freq / 2.0)
                }
            }
            "transbordos" -> {
                // Al ser rutas directas, si hay una seleccionada es la más óptima (0 transbordos)
                if (currentSelectedRoute != null && currentFilteredRoutes.any { it.idRuta == currentSelectedRoute?.idRuta }) {
                    currentSelectedRoute
                } else {
                    currentFilteredRoutes.firstOrNull()
                }
            }
            else -> null
        }

        if (bestRoute != null) {
            if (currentSelectedRoute != null && bestRoute.idRuta == currentSelectedRoute?.idRuta) {
                Toast.makeText(this, "actualmente esta es la ruta mas optima", Toast.LENGTH_SHORT).show()
            } else {
                showSingleRoute(bestRoute)
                val label = if (type == "tiempo") "Menor tiempo" else "Menor transbordos"
                Toast.makeText(this, "Optimizado por $label", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun calcularDistanciaTotalRuta(route: RutaDto): Double {
        var totalDist = 0.0
        route.trazadoGeojson.features.forEach { feature ->
            if (feature.geometry.type == "LineString") {
                @Suppress("UNCHECKED_CAST")
                val coords = feature.geometry.coordinates as? List<List<Double>>
                coords?.let {
                    for (i in 0 until it.size - 1) {
                        totalDist += haversine(it[i][1], it[i][0], it[i+1][1], it[i+1][0])
                    }
                }
            }
        }
        return totalDist
    }

    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0 // Radio Tierra en metros
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 2 * r * atan2(sqrt(a), sqrt(1 - a))
    }

    private fun showSingleRoute(route: RutaDto) {
        currentSelectedRoute = route
        rvSearchResults.visibility = View.GONE
        etSearch.clearFocus()
        
        val props = route.trazadoGeojson.features.firstOrNull()?.properties
        tvLocationName.text = props?.name ?: "Ruta seleccionada"
        tvLocationAddress.text = props?.desc ?: "Recorrido optimizado"
        cvResultInfo.visibility = View.VISIBLE

        mMap.clear()
        val boundsBuilder = LatLngBounds.Builder()
        var hayDatos = false

        route.trazadoGeojson.features.forEach { feature ->
            if (feature.geometry.type == "LineString") {
                val polyOptions = PolylineOptions().width(14f).color(Color.RED).geodesic(true)
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
        mMap.uiSettings.isZoomControlsEnabled = true
        mostrarTodasLasRutas()
    }

    private fun mostrarTodasLasRutas() {
        if (allRoutes.isEmpty()) return
        val boundsBuilder = LatLngBounds.Builder()
        var hayPuntos = false

        for (ruta in allRoutes) {
            ruta.trazadoGeojson.features.forEach { feature ->
                if (feature.geometry.type == "LineString") {
                    val semiTransparentBlue = Color.argb(128, 0, 0, 255)
                    val polylineOptions = PolylineOptions().width(6f).color(semiTransparentBlue)
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
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 120))
        }
    }
}
