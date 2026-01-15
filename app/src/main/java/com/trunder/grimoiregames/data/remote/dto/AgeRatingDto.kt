package com.trunder.grimoiregames.data.remote.dto

import com.google.gson.annotations.SerializedName

data class AgeRatingDto(
    @SerializedName("id") val id: Long? = null,

    // --- ORGANIZACIÓN (Sistema: PEGI, ESRB...) ---
    // 'organization': El nuevo estándar de IGDB (ID 8).
    // 'category': El antiguo estándar (ID 2).
    // 'rating_category': A veces usado para el sistema (ID 9).
    @SerializedName("organization") val organization: Int? = null,
    @SerializedName("category") val category: Int? = null,
    @SerializedName("rating_category") val ratingCategory: Int? = null,

    // --- NOTA (Valor: 3, 7, E, T...) ---
    // 'rating': El valor numérico de la nota (ID 4).
    @SerializedName("rating") val rating: Int? = null
)