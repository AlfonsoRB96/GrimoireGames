package com.trunder.grimoiregames.data.remote.dto

import com.google.gson.annotations.SerializedName

data class GameDetailDto(
    val id: Int,
    val name: String,
    @SerializedName("description_raw") // Usamos raw para evitar etiquetas HTML <p><br>
    val description: String?,
    val metacritic: Int?,
    @SerializedName("released")
    val released: String?,
    @SerializedName("background_image")
    val backgroundImage: String?,

    // Arrays de objetos (RAWG devuelve listas para esto)
    val genres: List<ItemDto>?,
    val developers: List<ItemDto>?,
    val publishers: List<ItemDto>?,
    val tags: List<ItemDto>?, // Aquí buscaremos "JRPG", "Open World", etc.

    @SerializedName("esrb_rating")
    val esrbRating: EsrbRatingDto?
)

// Clase auxiliar genérica para (ID + Nombre)
// Sirve para Género, Developer, Publisher y Tag
data class ItemDto(
    val id: Int,
    val name: String
)

data class EsrbRatingDto(
    val id: Int,
    val name: String // Ej: "Mature", "Everyone"
)