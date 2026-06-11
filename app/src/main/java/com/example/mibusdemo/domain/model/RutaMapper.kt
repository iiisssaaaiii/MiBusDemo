package com.example.mibusdemo.domain.model

import com.example.mibusdemo.data.dto.RutaDto

/**
 * Convierte un objeto RutaDto (proveniente del JSON) al modelo de dominio Ruta.
 * Maneja la nulabilidad de los campos y el casteo de las coordenadas GeoJSON.
 */
fun RutaDto.toDomain(): Ruta {
    // Buscamos la feature que contiene el trazado (LineString) o tomamos la primera por defecto
    val feature = this.trazadoGeojson.features.firstOrNull { it.geometry.type == "LineString" }
        ?: this.trazadoGeojson.features[0]

    val props = feature.properties
    
    // Casteamos 'coordinates' (que es Any) a una lista de listas para poder usar .map
    @Suppress("UNCHECKED_CAST")
    val coords = feature.geometry.coordinates as? List<List<Double>> ?: emptyList()

    return Ruta(
        // Usamos el operador Elvis ?: para dar valores por defecto si los datos son nulos
        id = props.id ?: this.idRuta,
        nombre = props.name ?: "Sin nombre",
        // Convertimos de [lng, lat] (GeoJSON) a Pair(lat, lng) para el modelo Ruta
        coordenadas = coords.map { point ->
            val lng = point.getOrNull(0) ?: 0.0
            val lat = point.getOrNull(1) ?: 0.0
            lat to lng
        }
    )
}
