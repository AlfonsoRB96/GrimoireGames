package com.trunder.grimoiregames.data.remote.dto

import com.google.gson.annotations.SerializedName

data class AgeRatingDto(
    @SerializedName("id") val id: Long? = null,

    // IGDB a veces usa "category" y a veces "organization"
    @SerializedName("category") val category: Int? = null,
    @SerializedName("organization") val organization: Int? = null,

    // IGDB a veces usa "rating" y a veces "rating_category"
    @SerializedName("rating") val rating: Int? = null,
    @SerializedName("rating_category") val ratingCategory: Int? = null
)