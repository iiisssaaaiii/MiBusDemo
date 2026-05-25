package com.example.mibusdemo.domain.model

// Clase ruta para representar una ruta
data class Ruta(
    val id: String,
    val nombre: String,
    val coordenadas: List<Pair<Double, Double>>
)