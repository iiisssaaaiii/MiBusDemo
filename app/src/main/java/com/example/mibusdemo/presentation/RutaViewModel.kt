package com.example.mibusdemo.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mibusdemo.domain.RutaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RutaViewModel(private val repository: RutaRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<RutaUiState>(RutaUiState.Loading)
    val uiState: StateFlow<RutaUiState> = _uiState.asStateFlow()

    init {
        cargarRutas()
    }

    private fun cargarRutas() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Llamamos al repositorio que creamos antes
                val rutas = repository.obtenerRutas()
                _uiState.value = RutaUiState.Success(rutas)
            } catch (e: Exception) {
                _uiState.value = RutaUiState.Error(e.message ?: "Error desconocido")
            }
        }
    }
}