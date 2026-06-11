package com.example.mibusdemo

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
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
    
    private var origenLat: Double = 0.0
    private var origenLng: Double = 0.0
    
    private var rutaIdSeleccionada: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_trayecto)

        destinoNombre = intent.getStringExtra(EXTRA_DESTINO_NOMBRE) ?: "Destino"
        destinoLat = intent.getDoubleExtra(EXTRA_DESTINO_LAT, destinoLat)
        destinoLng = intent.getDoubleExtra(EXTRA_DESTINO_LNG, destinoLng)
        
        origenLat = intent.getDoubleExtra(EXTRA_ORIGEN_LAT, 0.0)
        origenLng = intent.getDoubleExtra(EXTRA_ORIGEN_LNG, 0.0)
        
        rutaIdSeleccionada = intent.getStringExtra(EXTRA_RUTA_ID)

        findViewById<TextView>(R.id.tvDestinoDetalle).text = "Hacia: $destinoNombre"
        findViewById<TextView>(R.id.tvOrigenDetalle).text =
            "Desde: ${intent.getStringExtra(EXTRA_ORIGEN_NOMBRE) ?: "Tu ubicación"}"
        
        findViewById<Button>(R.id.btnCancelarViaje).setOnClickListener {
            finish()
        }
        
        configurarMenuInferior()

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapDetalle) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        val origen = LatLng(origenLat, origenLng)
        val destino = LatLng(destinoLat, destinoLng)
        
        val ruta = buscarMejorRuta(origen, destino)

        googleMap.uiSettings.isZoomControlsEnabled = true
        
        if (origenLat != 0.0) {
            googleMap.addMarker(
                MarkerOptions()
                    .position(origen)
                    .title("Tu origen")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
            )
        }
        
        googleMap.addMarker(MarkerOptions().position(destino).title(destinoNombre))

        if (ruta == null || ruta.puntos.isEmpty()) {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(destino, 15f))
            findViewById<TextView>(R.id.tvRutaResumen).text = "No se encontró una ruta óptima"
            return
        }

        googleMap.addPolyline(
            PolylineOptions()
                .addAll(ruta.puntos)
                .color(Color.rgb(30, 136, 229))
                .width(12f)
        )

        val paradaSubida = ruta.paradas.minByOrNull { distanciaMetros(it.ubicacion, origen) }
        val paradaBajada = ruta.paradas.minByOrNull { distanciaMetros(it.ubicacion, destino) }

        paradaSubida?.let {
            googleMap.addMarker(MarkerOptions().position(it.ubicacion).title("Subir aquí").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)))
        }
        paradaBajada?.let {
            googleMap.addMarker(MarkerOptions().position(it.ubicacion).title("Bajar aquí").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)))
        }

        val boundsBuilder = LatLngBounds.builder()
        ruta.puntos.forEach { boundsBuilder.include(it) }
        boundsBuilder.include(destino)
        if (origenLat != 0.0) boundsBuilder.include(origen)
        googleMap.setOnMapLoadedCallback {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 120))
        }

        // --- CÁLCULOS REALISTAS ---
        val distCaminataOrigen = if (origenLat != 0.0 && paradaSubida != null) distanciaMetros(origen, paradaSubida.ubicacion) else 0.0
        val distCaminataDestino = paradaBajada?.let { distanciaMetros(it.ubicacion, destino) } ?: 0.0
        
        val minCaminata1 = estimarMinutosCaminata(distCaminataOrigen)
        val minCaminata2 = estimarMinutosCaminata(distCaminataDestino)
        
        // Calcular tiempo de bus solo para el tramo recorrido
        val iSubida = ruta.puntos.indexOf(ruta.puntos.minByOrNull { distanciaMetros(it, paradaSubida?.ubicacion ?: origen) })
        val iBajada = ruta.puntos.indexOf(ruta.puntos.minByOrNull { distanciaMetros(it, paradaBajada?.ubicacion ?: destino) })
        val minutosBus = estimarMinutosTramoBus(ruta.puntos, iSubida, iBajada)
        
        val tiempoEspera = ruta.frecuenciaMin / 2 // Espera promedio
        val tiempoTotal = minCaminata1 + tiempoEspera + minutosBus + minCaminata2 + 2 // +2 min de margen

        findViewById<TextView>(R.id.tvViajeTotal).text = "Tiempo total estimado: $tiempoTotal min"
        findViewById<TextView>(R.id.tvRutaResumen).text = "Ruta: ${ruta.nombre}"
        findViewById<TextView>(R.id.tvFrecuenciaDetalle).text = "Frecuencia: ${ruta.frecuenciaMin} min (Espera est. ${tiempoEspera} min)"
        
        findViewById<TextView>(R.id.tvPaso1).text = "1. Camina $minCaminata1 min a la parada"
        findViewById<TextView>(R.id.tvPaso2).text = "2. Espera y aborda la ${ruta.nombre}"
        findViewById<TextView>(R.id.tvPaso3).text = "3. Viaja en bus aprox. $minutosBus min"
        findViewById<TextView>(R.id.tvPaso4).text = "4. Camina $minCaminata2 min al destino"
    }

    private fun buscarMejorRuta(origen: LatLng, destino: LatLng): RutaMapa? {
        val json = assets.open("middleware_base_rutas.json").bufferedReader().use { it.readText() }
        val rutasJson = JSONArray(json)
        val todasLasRutas = mutableListOf<RutaMapa>()

        for (i in 0 until rutasJson.length()) {
            val rutaJson = rutasJson.getJSONObject(i)
            val rutaId = rutaJson.optString("id_ruta")
            val trazado = rutaJson.optJSONObject("trazado_geojson")?.optJSONArray("features")?.optJSONObject(0) ?: continue
            val props = trazado.optJSONObject("properties")
            val nombre = props?.optString("notes").orEmpty().ifBlank { props?.optString("name").orEmpty().ifBlank { "Ruta $rutaId" } }
            val puntos = mutableListOf<LatLng>()
            val coords = trazado.getJSONObject("geometry").getJSONArray("coordinates")
            for (j in 0 until coords.length()) {
                val p = coords.getJSONArray(j)
                puntos.add(LatLng(p.getDouble(1), p.getDouble(0)))
            }
            todasLasRutas.add(RutaMapa(rutaId, nombre, props?.optInt("midday", 10) ?: 10, puntos, extraerParadas(rutaJson)))
        }

        if (rutaIdSeleccionada != null) return todasLasRutas.firstOrNull { it.id == rutaIdSeleccionada }

        return todasLasRutas.minByOrNull { ruta ->
            val iO = ruta.puntos.indexOf(ruta.puntos.minByOrNull { distanciaMetros(it, origen) })
            val iD = ruta.puntos.indexOf(ruta.puntos.minByOrNull { distanciaMetros(it, destino) })
            if (iO < iD) distanciaMetros(ruta.puntos[iO], origen) + distanciaMetros(ruta.puntos[iD], destino)
            else Double.MAX_VALUE
        }
    }

    private fun extraerParadas(rutaJson: org.json.JSONObject): List<ParadaMapa> {
        val features = rutaJson.optJSONObject("paradas_geojson")?.optJSONArray("features") ?: return emptyList()
        val lista = mutableListOf<ParadaMapa>()
        for (i in 0 until features.length()) {
            val f = features.getJSONObject(i)
            val id = f.optJSONObject("properties")?.optString("id").orEmpty()
            val c = f.getJSONObject("geometry").getJSONArray("coordinates")
            lista.add(ParadaMapa(id, LatLng(c.getDouble(1), c.getDouble(0))))
        }
        return lista
    }

    private fun estimarMinutosTramoBus(puntos: List<LatLng>, start: Int, end: Int): Int {
        if (start >= end || start < 0 || end >= puntos.size) return 5
        var dist = 0.0
        for (i in start until end) {
            dist += distanciaMetros(puntos[i], puntos[i+1])
        }
        return (dist / 180.0).toInt().coerceAtLeast(4) // 180m/min = ~11km/h (Tráfico Xalapa)
    }

    private fun estimarMinutosCaminata(distancia: Double): Int {
        return (distancia / 80.0).toInt().coerceAtLeast(1) // 80m/min caminando
    }

    private fun distanciaMetros(a: LatLng, b: LatLng): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(b.latitude - a.latitude)
        val dLng = Math.toRadians(b.longitude - a.longitude)
        val h = sin(dLat/2).pow(2) + cos(Math.toRadians(a.latitude)) * cos(Math.toRadians(b.latitude)) * sin(dLng/2).pow(2)
        return 2 * r * atan2(sqrt(h), sqrt(1-h))
    }

    private fun configurarMenuInferior() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavDetalle)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_inicio -> { startActivity(Intent(this, ParadasCercanasActivity::class.java)); true }
                R.id.nav_rutas -> { startActivity(Intent(this, TodasLasRutas::class.java)); true }
                R.id.nav_favoritos -> { startActivity(Intent(this, AltertasFavs::class.java)); true }
                else -> false
            }
        }
    }

    private data class RutaMapa(val id: String, val nombre: String, val frecuenciaMin: Int, val puntos: List<LatLng>, val paradas: List<ParadaMapa>)
    private data class ParadaMapa(val id: String, val ubicacion: LatLng)

    companion object {
        const val EXTRA_ORIGEN_NOMBRE = "extra_origen_nombre"
        const val EXTRA_ORIGEN_LAT = "extra_origen_lat"
        const val EXTRA_ORIGEN_LNG = "extra_origen_lng"
        const val EXTRA_DESTINO_NOMBRE = "extra_destino_nombre"
        const val EXTRA_DESTINO_LAT = "extra_destino_lat"
        const val EXTRA_DESTINO_LNG = "extra_destino_lng"
        const val EXTRA_RUTA_ID = "extra_ruta_id"
    }
}
