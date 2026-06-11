package com.example.mibusdemo

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.json.JSONArray
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class DetalleTrayectoActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var destinoNombre: String
    private var destinoLat: Double = 19.5438
    private var destinoLng: Double = -96.9101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_trayecto)

        destinoNombre = intent.getStringExtra(EXTRA_DESTINO_NOMBRE) ?: "Destino"
        destinoLat = intent.getDoubleExtra(EXTRA_DESTINO_LAT, destinoLat)
        destinoLng = intent.getDoubleExtra(EXTRA_DESTINO_LNG, destinoLng)

        findViewById<TextView>(R.id.tvDestinoDetalle).text = destinoNombre
        findViewById<TextView>(R.id.tvOrigenDetalle).text =
            intent.getStringExtra(EXTRA_ORIGEN_NOMBRE) ?: "Tu ubicacion actual"
        findViewById<Button>(R.id.btnCancelarViaje).setOnClickListener { finish() }

        configurarMenuInferior()

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapDetalle) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun configurarMenuInferior() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_rutas
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_inicio -> {
                    startActivity(Intent(this, ParadasCercanasActivity::class.java))
                    true
                }
                R.id.nav_rutas -> {
                    startActivity(Intent(this, TodasLasRutas::class.java))
                    true
                }
                R.id.nav_favoritos -> {
                    startActivity(Intent(this, AltertasFavs::class.java))
                    true
                }
                else -> false
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        val destino = LatLng(destinoLat, destinoLng)
        val ruta = cargarRutaMasCercana(destino)

        googleMap.addMarker(MarkerOptions().position(destino).title(destinoNombre))

        if (ruta == null || ruta.puntos.isEmpty()) {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(destino, 15f))
            findViewById<TextView>(R.id.tvRutaResumen).text = "No se encontro una ruta cercana"
            return
        }

        googleMap.addPolyline(
            PolylineOptions()
                .addAll(ruta.puntos)
                .color(Color.rgb(30, 136, 229))
                .width(10f)
        )

        val paradaDestino = ruta.paradas.minByOrNull { distanciaMetros(it.ubicacion, destino) }
        if (paradaDestino != null) {
            googleMap.addMarker(
                MarkerOptions()
                    .position(paradaDestino.ubicacion)
                    .title("Bajar en parada ${paradaDestino.id}")
            )
        }

        val bounds = LatLngBounds.builder().apply {
            ruta.puntos.forEach { include(it) }
            include(destino)
        }.build()
        googleMap.setOnMapLoadedCallback {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 80))
        }

        val minutosRuta = estimarMinutosRuta(ruta.puntos)
        val distanciaCaminata = paradaDestino?.let { distanciaMetros(it.ubicacion, destino) } ?: 80.0
        val minutosCaminata = estimarMinutosCaminata(distanciaCaminata)

        findViewById<TextView>(R.id.tvViajeTotal).text =
            "Viaje Total: ${minutosRuta + minutosCaminata} min Aprox."
        findViewById<TextView>(R.id.tvRutaResumen).text = "${ruta.nombre} | ${ruta.descripcion}"
        findViewById<TextView>(R.id.tvFrecuenciaDetalle).text =
            "Frecuencia aprox: ${ruta.frecuenciaMin} min"
        findViewById<TextView>(R.id.tvPaso1).text = "1  Ve a la parada mas cercana"
        findViewById<TextView>(R.id.tvPaso2).text = "2  Aborda ${ruta.nombre}"
        findViewById<TextView>(R.id.tvPaso3).text =
            "3  Baja en parada ${paradaDestino?.id ?: "sugerida"}"
        findViewById<TextView>(R.id.tvPaso4).text =
            "4  Camina $minutosCaminata min hacia $destinoNombre"
    }

    private fun cargarRutaMasCercana(destino: LatLng): RutaMapa? {
        val rutas = mutableListOf<RutaMapa>()
        val json = assets.open("middleware_base_rutas.json").bufferedReader().use { it.readText() }
        val rutasJson = JSONArray(json)

        for (i in 0 until rutasJson.length()) {
            val rutaJson = rutasJson.getJSONObject(i)
            val rutaId = rutaJson.optString("id_ruta")
            val trazado = rutaJson.optJSONObject("trazado_geojson")
                ?.optJSONArray("features")
                ?.optJSONObject(0)
                ?: continue

            val props = trazado.optJSONObject("properties")
            val nombre = props?.optString("notes").orEmpty().ifBlank { "Ruta $rutaId" }
            val descripcion = props?.optString("desc").orEmpty().ifBlank { "Recorrido local" }
            val frecuencia = props?.optInt("midday", 10) ?: 10
            val coords = trazado.getJSONObject("geometry").getJSONArray("coordinates")
            val puntos = mutableListOf<LatLng>()

            for (j in 0 until coords.length()) {
                val punto = coords.getJSONArray(j)
                puntos.add(LatLng(punto.getDouble(1), punto.getDouble(0)))
            }

            rutas.add(RutaMapa(nombre, descripcion, frecuencia, puntos, extraerParadas(rutaJson)))
        }

        return rutas.minByOrNull { ruta ->
            ruta.puntos.minOfOrNull { distanciaMetros(it, destino) } ?: Double.MAX_VALUE
        }
    }

    private fun extraerParadas(rutaJson: org.json.JSONObject): List<ParadaMapa> {
        val features = rutaJson.optJSONObject("paradas_geojson")
            ?.optJSONArray("features")
            ?: return emptyList()

        val paradas = mutableListOf<ParadaMapa>()
        for (i in 0 until features.length()) {
            val feature = features.getJSONObject(i)
            val id = feature.optJSONObject("properties")?.optString("id").orEmpty()
            val coords = feature.getJSONObject("geometry").getJSONArray("coordinates")
            paradas.add(ParadaMapa(id, LatLng(coords.getDouble(1), coords.getDouble(0))))
        }
        return paradas
    }

    private fun estimarMinutosRuta(puntos: List<LatLng>): Int {
        if (puntos.size < 2) return 8
        val distancia = puntos.zipWithNext().sumOf { (a, b) -> distanciaMetros(a, b) }
        return (distancia / 300.0).toInt().coerceAtLeast(8)
    }

    private fun estimarMinutosCaminata(distancia: Double): Int {
        return (distancia / 80.0).toInt().coerceAtLeast(1)
    }

    private fun distanciaMetros(a: LatLng, b: LatLng): Double {
        val radioTierra = 6371000.0
        val dLat = Math.toRadians(b.latitude - a.latitude)
        val dLng = Math.toRadians(b.longitude - a.longitude)
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)
        val h = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLng / 2).pow(2)
        return 2 * radioTierra * atan2(sqrt(h), sqrt(1 - h))
    }

    private data class RutaMapa(
        val nombre: String,
        val descripcion: String,
        val frecuenciaMin: Int,
        val puntos: List<LatLng>,
        val paradas: List<ParadaMapa>
    )

    private data class ParadaMapa(val id: String, val ubicacion: LatLng)

    companion object {
        const val EXTRA_ORIGEN_NOMBRE = "extra_origen_nombre"
        const val EXTRA_DESTINO_NOMBRE = "extra_destino_nombre"
        const val EXTRA_DESTINO_LAT = "extra_destino_lat"
        const val EXTRA_DESTINO_LNG = "extra_destino_lng"
    }
}
