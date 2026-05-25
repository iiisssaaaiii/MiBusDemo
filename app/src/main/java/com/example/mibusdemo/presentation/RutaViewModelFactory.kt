package com.example.mibusdemo.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.mibusdemo.domain.RutaRepository

class RutaViewModelFactory(private val repository: RutaRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RutaViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RutaViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}