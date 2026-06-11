package com.example.mibusdemo

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.textfield.TextInputEditText
import org.json.JSONArray

class PlanificarViaje : AppCompatActivity() {

    private var destinoNombre: String? = null
    private var destinoLat: Double? = null
    private var destinoLng: Double? = null
    private var sugerencias = emptyList<DestinoSugerido>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_planificar_viaje)

        sugerencias = cargarSugerenciasLocales()
        configurarMenuInferior()
        configurarDestino()
        configurarAcciones()
    }

    private fun configurarDestino() {
        val etDestino = findViewById<AutoCompleteTextView>(R.id.etDestino)
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            sugerencias.map { it.nombre }
        )
        etDestino.setAdapter(adapter)
        etDestino.threshold = 0
        etDestino.setOnClickListener { etDestino.showDropDown() }
        etDestino.setOnItemClickListener { _, _, position, _ ->
            val nombre = adapter.getItem(position).orEmpty()
            val destino = sugerencias.firstOrNull { it.nombre == nombre }
            if (destino != null) {
                seleccionarDestino(destino)
            }
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
            val lat = destinoLat
            val lng = destinoLng
            if (nombre == null || lat == null || lng == null) {
                Toast.makeText(this, "Primero selecciona un destino", Toast.LENGTH_SHORT).show()
            } else {
                DemoSession.addFavorite(this, nombre, lat, lng)
                Toast.makeText(this, "$nombre agregado a favoritos", Toast.LENGTH_SHORT).show()
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
        val lat = destinoLat
        val lng = destinoLng

        if (nombre == null || lat == null || lng == null) {
            Toast.makeText(this, "Selecciona un destino de la lista o del historial", Toast.LENGTH_SHORT).show()
            return
        }

        val origen = findViewById<TextInputEditText>(R.id.etOrigen).text.toString()
        startActivity(
            Intent(this, DetalleTrayectoActivity::class.java).apply {
                putExtra(DetalleTrayectoActivity.EXTRA_ORIGEN_NOMBRE, origen)
                putExtra(DetalleTrayectoActivity.EXTRA_DESTINO_NOMBRE, nombre)
                putExtra(DetalleTrayectoActivity.EXTRA_DESTINO_LAT, lat)
                putExtra(DetalleTrayectoActivity.EXTRA_DESTINO_LNG, lng)
            }
        )
    }

    private fun configurarMenuInferior() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavPlanificar)
        bottomNav.selectedItemId = R.id.nav_rutas
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_inicio -> {
                    startActivity(Intent(this, ParadasCercanasActivity::class.java))
                    true
                }
                R.id.nav_rutas -> true
                R.id.nav_favoritos -> {
                    Toast.makeText(this, "Favoritos y alertas lo esta trabajando tu companero", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }
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
            // Las sugerencias base siguen disponibles aunque el JSON no cargue.
        }

        return destinos.values.take(40)
    }

    private data class DestinoSugerido(
        val nombre: String,
        val lat: Double,
        val lng: Double
    )
}
