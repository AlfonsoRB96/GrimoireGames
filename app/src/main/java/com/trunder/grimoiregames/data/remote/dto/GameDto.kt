package com.trunder.grimoiregames.data.remote.dto

import com.google.gson.annotations.SerializedName

// 1. La respuesta principal de la API (es una lista de resultados)
data class GameSearchResponse(
    val count: Int,
    val results: List<GameDto>
)

// 2. Cada juego individual que viene de internet
data class GameDto(
    val id: Int,
    val name: String,
    // RAWG usa snake_case (guiones bajos), pero Kotlin usa camelCase.
    // @SerializedName hace la traducción.
    @SerializedName("background_image")
    val backgroundImage: String?,
    @SerializedName("released")
    val releaseDate: String?,
    val rating: Double?,
    val platforms: List<PlatformWrapper>?
)

// Clases auxiliares para "desenvolver" el JSON anidado de RAWG
data class PlatformWrapper(
    val platform: PlatformInfo
)

data class PlatformInfo(
    val name: String
)