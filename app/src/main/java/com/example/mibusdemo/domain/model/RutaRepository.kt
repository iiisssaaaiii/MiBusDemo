package com.example.mibusdemo.domain

import com.example.mibusdemo.domain.model.Ruta

interface RutaRepository {
    fun obtenerRutas(): List<Ruta>
}