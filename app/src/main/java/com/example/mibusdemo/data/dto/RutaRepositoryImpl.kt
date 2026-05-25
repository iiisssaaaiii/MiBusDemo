package com.example.mibusdemo.data.dto

import com.example.mibusdemo.data.dto.LocalRouteDataSource
import com.example.mibusdemo.domain.RutaRepository
import com.example.mibusdemo.domain.model.Ruta
import com.example.mibusdemo.domain.model.toDomain

class RutaRepositoryImpl(private val dataSource: LocalRouteDataSource) : RutaRepository {

    override fun obtenerRutas(): List<Ruta> {
        // Todo este código debe ir DENTRO de la función, no afuera
        return dataSource.getRutasFromJson().map { it.toDomain() }
    }
}

