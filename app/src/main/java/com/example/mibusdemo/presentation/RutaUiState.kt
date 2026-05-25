package com.example.mibusdemo.presentation

import com.example.mibusdemo.domain.model.Ruta

sealed class RutaUiState {
    object Loading : RutaUiState() // Mientras cargam el JSON
    data class Success(val rutas: List<Ruta>) : RutaUiState() // Cuando ya tenemos las rutas
    data class Error(val message: String) : RutaUiState() // Por si algo falla c:
}

