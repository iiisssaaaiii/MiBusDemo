package com.example.mibusdemo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng

// Implementar OnMapReadyCallback
class principal : AppCompatActivity(), OnMapReadyCallback {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_principal)

        // Obtener el mapa desde el fragmento definido en el XML
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment

        // Solicitar la carga del mapa
        mapFragment.getMapAsync(this)
    }

    // 4. Este metodo es llamado cuando el mapa esta listo para ser utilizado
    override fun onMapReady(googleMap: GoogleMap) {
        // Mover la vita del mapa a Xalapa ------- PROBAR Y VALIDARRR ----------
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(LatLng(19.54, -96.91)))
        // Aumentar el zoom del mapa
        googleMap.animateCamera(CameraUpdateFactory.zoomTo(15f))

        // Mostrar icono de parada en todas las paradas

    }

    private fun mostrarParadasEnMapa(nMapa: GoogleMap) {


    }
}