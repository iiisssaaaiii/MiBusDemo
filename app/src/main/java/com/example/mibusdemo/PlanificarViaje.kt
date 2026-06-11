package com.example.mibusdemo

import android.Manifest
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import org.json.JSONArray
import java.util.Locale

class PlanificarViaje : AppCompatActivity() {

    private var destinoNombre: String? = null
    private var destinoLat: Double? = null
    private var destinoLng: Double? = null
    
    private var origenLat: Double? = null
    private var origenLng: Double? = null

    private var sugerencias = mutableListOf<DestinoSugerido>()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var placesClient: PlacesClient
    private val predictionsMap = mutableMapOf<String, String>() // Nombre visible -> PlaceId

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_planificar_viaje)

        inicializarPlaces()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        sugerencias.addAll(cargarSugerenciasLocales())
        NavigationHelper.setupBottomNavigation(this, R.id.nav_rutas)
        configurarDestino()
        configurarAcciones()
        
        obtenerUbicacionActual()
        
        findViewById<TextInputLayout>(R.id.tilOrigen).setEndIconOnClickListener {
            obtenerUbicacionActual()
        }
    }

    private fun inicializarPlaces() {
        try {
            val ai: ApplicationInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            val apiKey = ai.metaData.getString("com.google.android.geo.API_KEY") ?: ""
            if (!Places.isInitialized()) {
                Places.initialize(applicationContext, apiKey)
            }
            placesClient = Places.createClient(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun obtenerUbicacionActual() {
        val etOrigen = findViewById<TextInputEditText>(R.id.etOrigen)
        
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1001
            )
            return
        }

        etOrigen.setText("Localizando...")

        fusedLocationClient.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    origenLat = location.latitude
                    origenLng = location.longitude
                    
                    Thread {
                        try {
                            val geocoder = Geocoder(this, Locale.getDefault())
                            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                            runOnUiThread {
                                if (addresses != null && addresses.isNotEmpty()) {
                                    val address = addresses[0].getAddressLine(0)
                                    etOrigen.setText(address)
                                } else {
                                    etOrigen.setText("Mi ubicación actual")
                                }
                            }
                        } catch (e: Exception) {
                            runOnUiThread { etOrigen.setText("Mi ubicación actual") }
                        }
                    }.start()
                } else {
                    etOrigen.setText("Ubicación no disponible")
                    Toast.makeText(this, "Asegúrate de tener el GPS encendido", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun configurarDestino() {
        val etDestino = findViewById<AutoCompleteTextView>(R.id.etDestino)
        val adapter = ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, mutableListOf())
        etDestino.setAdapter(adapter)
        etDestino.threshold = 1

        etDestino.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                if (query.length >= 3) {
                    buscarPredicciones(query, adapter)
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        etDestino.setOnItemClickListener { _, _, position, _ ->
            val seleccionado = adapter.getItem(position) ?: return@setOnItemClickListener
            val placeId = predictionsMap[seleccionado]
            
            if (placeId != null) {
                obtenerCoordenadasGoogle(placeId, seleccionado)
            } else {
                val local = sugerencias.firstOrNull { it.nombre == seleccionado }
                if (local != null) seleccionarDestino(local)
            }
        }
    }

    private fun buscarPredicciones(query: String, adapter: ArrayAdapter<String>) {
        // Restricción geográfica para Xalapa, Veracruz
        val xalapaBounds = RectangularBounds.newInstance(
            com.google.android.gms.maps.model.LatLng(19.4716, -96.9781), // Suroeste
            com.google.android.gms.maps.model.LatLng(19.6132, -96.8441)  // Noreste
        )

        val request = FindAutocompletePredictionsRequest.builder()
            .setQuery(query)
            .setLocationRestriction(xalapaBounds)
            .setCountries("MX")
            .build()

        placesClient.findAutocompletePredictions(request).addOnSuccessListener { response ->
            val listaFinal = mutableListOf<String>()
            
            listaFinal.addAll(sugerencias.filter { it.nombre.contains(query, ignoreCase = true) }.map { it.nombre })
            
            for (prediction in response.autocompletePredictions) {
                val text = prediction.getFullText(null).toString()
                listaFinal.add(text)
                predictionsMap[text] = prediction.placeId
            }

            adapter.clear()
            adapter.addAll(listaFinal.distinct())
            adapter.notifyDataSetChanged()
        }.addOnFailureListener {
            val locales = sugerencias.filter { it.nombre.contains(query, ignoreCase = true) }.map { it.nombre }
            adapter.clear()
            adapter.addAll(locales)
            adapter.notifyDataSetChanged()
        }
    }

    private fun obtenerCoordenadasGoogle(placeId: String, nombre: String) {
        val fields = listOf(Place.Field.LAT_LNG)
        val request = FetchPlaceRequest.newInstance(placeId, fields)

        placesClient.fetchPlace(request).addOnSuccessListener { response ->
            val latLng = response.place.latLng
            if (latLng != null) {
                seleccionarDestino(DestinoSugerido(nombre, latLng.latitude, latLng.longitude))
            }
        }.addOnFailureListener {
            Toast.makeText(this, "No se pudieron obtener las coordenadas", Toast.LENGTH_SHORT).show()
        }
    }

    private fun configurarAcciones() {
        findViewById<Button>(R.id.btn_iniciar_viaje).setOnClickListener {
            val textoDestino = findViewById<AutoCompleteTextView>(R.id.etDestino).text.toString().trim()
            if (destinoNombre == null && textoDestino.isNotBlank()) {
                val destino = sugerencias.firstOrNull { it.nombre.equals(textoDestino, ignoreCase = true) }
                if (destino != null) seleccionarDestino(destino)
            }
            abrirDetalleTrayecto()
        }

        findViewById<Button>(R.id.btn_anadir_favoritos).setOnClickListener {
            val nombre = destinoNombre
            val origen = findViewById<TextInputEditText>(R.id.etOrigen).text.toString().trim()
            
            if (nombre == null) {
                Toast.makeText(this, "Primero selecciona un destino de la lista", Toast.LENGTH_SHORT).show()
            } else {
                DemoSession.addFavoriteRoute(this, if (origen.isEmpty()) "Mi ubicación" else origen, nombre!!)
                Toast.makeText(this, "Ruta guardada en favoritos", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<TextView>(R.id.tvHistorialEconomia).setOnClickListener {
            seleccionarDestino(DestinoSugerido("Economia", 19.5429, -96.9181))
        }
        findViewById<TextView>(R.id.tvHistorialFcas).setOnClickListener {
            seleccionarDestino(DestinoSugerido("FCAS", 19.5438, -96.9272))
        }
    }

    private fun seleccionarDestino(destino: DestinoSugerido) {
        destinoNombre = destino.nombre
        destinoLat = destino.lat
        destinoLng = destino.lng
        findViewById<AutoCompleteTextView>(R.id.etDestino).setText(destino.nombre, false)
        Toast.makeText(this, "Destino seleccionado: ${destino.nombre}", Toast.LENGTH_SHORT).show()
    }

    private fun abrirDetalleTrayecto() {
        val nombre = destinoNombre
        val dLat = destinoLat
        val dLng = destinoLng

        if (nombre == null || dLat == null || dLng == null) {
            Toast.makeText(this, "Selecciona un destino de la lista o del historial", Toast.LENGTH_SHORT).show()
            return
        }

        val oNombre = findViewById<TextInputEditText>(R.id.etOrigen).text.toString()
        val oLat = origenLat ?: 0.0
        val oLng = origenLng ?: 0.0

        startActivity(
            Intent(this, DetalleTrayectoActivity::class.java).apply {
                putExtra(DetalleTrayectoActivity.EXTRA_ORIGEN_NOMBRE, oNombre)
                putExtra(DetalleTrayectoActivity.EXTRA_ORIGEN_LAT, oLat)
                putExtra(DetalleTrayectoActivity.EXTRA_ORIGEN_LNG, oLng)
                putExtra(DetalleTrayectoActivity.EXTRA_DESTINO_NOMBRE, nombre)
                putExtra(DetalleTrayectoActivity.EXTRA_DESTINO_LAT, dLat)
                putExtra(DetalleTrayectoActivity.EXTRA_DESTINO_LNG, dLng)
            }
        )
    }



    private fun cargarSugerenciasLocales(): List<DestinoSugerido> {
        val destinos = linkedMapOf(
            "Economia" to DestinoSugerido("Economia", 19.5429, -96.9181),
            "FCAS" to DestinoSugerido("FCAS", 19.5438, -96.9272),
            "Plaza Crystal" to DestinoSugerido("Plaza Crystal", 19.5338, -96.9154)
        )

        try {
            val json = assets.open("middleware_base_rutas.json").bufferedReader().use { it.readText() }
            val rutas = JSONArray(json)
            for (i in 0 until rutas.length()) {
                val features = rutas.getJSONObject(i)
                    .optJSONObject("paradas_geojson")
                    ?.optJSONArray("features")
                    ?: continue

                for (j in 0 until features.length()) {
                    val feature = features.getJSONObject(j)
                    val id = feature.optJSONObject("properties")?.optString("id").orEmpty()
                    val coords = feature.getJSONObject("geometry").getJSONArray("coordinates")
                    val nombre = "Parada $id"
                    destinos.putIfAbsent(nombre, DestinoSugerido(nombre, coords.getDouble(1), coords.getDouble(0)))
                }
            }
        } catch (_: Exception) {
        }
        return destinos.values.toList()
    }

    private data class DestinoSugerido(
        val nombre: String,
        val lat: Double,
        val lng: Double
    )

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            obtenerUbicacionActual()
        }
    }
}
