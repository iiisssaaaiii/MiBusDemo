package com.example.mibusdemo.domain.model

import com.example.mibusdemo.data.dto.RutaDto

// Convertir la rutaDto a un objeto Ruta
// Esto porque por ahora los datos son geojson pero en un futuro pueden ser de una BD o
// de una API externa
fun RutaDto.toDomain(): Ruta {
    return Ruta(
        id = this.trazadoGeojson.features[0].properties.id,
        nombre = this.trazadoGeojson.features[0].properties.name,
        coordenadas = this.trazadoGeojson.features[0].geometry.coordinates.map { it[1] to it[0] }
    )
}