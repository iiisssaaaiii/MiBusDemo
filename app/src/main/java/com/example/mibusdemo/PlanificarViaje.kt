package com.example.mibusdemo

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener

class PlanificarViaje : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_planificar_viaje)

        // Inicializar Places SDK (Usa la misma Key del Manifest)
        if (!Places.isInitialized()) {
            val apiKey = packageManager.getApplicationInfo(packageName, android.content.pm.PackageManager.GET_META_DATA)
                .metaData.getString("com.google.android.geo.API_KEY") ?: ""
            Places.initialize(applicationContext, apiKey)
        }

        // Configurar Autocomplete para Destino
        val autocompleteFragment = supportFragmentManager
            .findFragmentById(R.id.autocomplete_fragment) as AutocompleteSupportFragment

        autocompleteFragment.setPlaceFields(listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG))
        autocompleteFragment.setHint("¿A dónde vas?")

        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                // Tarea 2.3: Aquí guardarías el destino para el Intent
                Toast.makeText(this@PlanificarViaje, "Destino: ${place.name}", Toast.LENGTH_SHORT).show()
            }

            override fun onError(status: com.google.android.gms.common.api.Status) {
                Toast.makeText(this@PlanificarViaje, "Error en búsqueda", Toast.LENGTH_SHORT).show()
            }
        })

        findViewById<Button>(R.id.btn_iniciar_viaje).setOnClickListener {
            // Aquí llamarías a DetalleTrayectoActivity (CU-03)
            Toast.makeText(this, "Calculando ruta...", Toast.LENGTH_SHORT).show()
        }
    }
}