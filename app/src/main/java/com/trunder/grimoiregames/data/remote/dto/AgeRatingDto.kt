package com.trunder.grimoiregames.data.remote.dto

import com.google.gson.annotations.SerializedName

data class AgeRatingDto(
    @SerializedName("id") val id: Long? = null,

    // Puede venir null, pero no pasa nada
    @SerializedName("organization") val organizationId: Int? = null,

    // La nota en sí (PEGI 18, ESRB T...)
    @SerializedName("rating_category") val ratingId: Int? = null
) {
    // Convertimos IDs a Enums
    val rating: AgeRatingCategory?
        get() = AgeRatingCategory.fromId(ratingId)

    // LÓGICA MEJORADA:
    // 1. Si la API nos dice la organización, genial.
    // 2. Si no (null), la deducimos de la nota (ej: si es "PEGI 3", la org es PEGI).
    val organization: AgeRatingOrganization?
        get() {
            // Prioridad 1: Deducir del rating (es más fiable porque es único)
            rating?.let { return it.org }

            // Prioridad 2: Usar el campo organization explicito
            return AgeRatingOrganization.fromId(organizationId)
        }
}