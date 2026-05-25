package com.example.mibusdemo.data.dto

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.example.mibusdemo.data.dto.RutaDto

class LocalRouteDataSource(private val context: Context) {

    fun getRutasFromJson(): List<RutaDto> {
        val jsonString = context.assets.open("middleware_base_rutas.json")
            .bufferedReader()
            .use { it.readText() }

        // Esta línea le dice a Gson que convierta el texto en una Lista de RutaDto
        val listType = object : TypeToken<List<RutaDto>>() {}.type
        return Gson().fromJson(jsonString, listType)
    }
}