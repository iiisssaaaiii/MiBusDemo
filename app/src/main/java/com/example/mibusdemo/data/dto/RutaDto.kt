package com.example.mibusdemo.data.dto

import com.google.gson.annotations.SerializedName

data class RutaDto(
    @SerializedName("id_ruta") val idRuta: String,
    @SerializedName("sentido") val sentido: String,
    @SerializedName("trazado_geojson") val trazadoGeojson: FeatureCollectionDto,
    @SerializedName("paradas_geojson") val paradasGeojson: FeatureCollectionDto?
)

data class FeatureCollectionDto(
    @SerializedName("type") val type: String,
    @SerializedName("features") val features: List<FeatureDto>
)

data class FeatureDto(
    @SerializedName("type") val type: String,
    @SerializedName("properties") val properties: PropertiesDto,
    @SerializedName("geometry") val geometry: GeometryDto
)

data class PropertiesDto(
    @SerializedName("id") val id: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("desc") val desc: String?,
    @SerializedName("notes") val notes: String?,
    @SerializedName("peak_am") val peakAm: Int?,
    @SerializedName("midday") val midday: Int?,
    @SerializedName("peak_pm") val peakPm: Int?,
    @SerializedName("night") val night: Int?
)

data class GeometryDto(
    @SerializedName("type") val type: String,
    @SerializedName("coordinates") val coordinates: Any // Can be List<Double> for Point or List<List<Double>> for LineString
)