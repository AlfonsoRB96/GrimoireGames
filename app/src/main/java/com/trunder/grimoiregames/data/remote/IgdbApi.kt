package com.trunder.grimoiregames.data.remote

import com.trunder.grimoiregames.data.remote.dto.AgeRatingDto
import com.trunder.grimoiregames.data.remote.dto.IgdbGameDto
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface IgdbApi {
    @POST("games")
    suspend fun getGames(
        @Header("Client-ID") clientId: String,
        @Header("Authorization") authorization: String, // "Bearer <token>"
        @Body query: RequestBody // Aquí mandaremos el texto "fields name, cover..."
    ): List<IgdbGameDto>

    // Nuevo endpoint para buscar detalles de ratings sueltos
    @POST("age_ratings")
    suspend fun getAgeRatingDetails(
        @Header("Client-ID") clientId: String,
        @Header("Authorization") authorization: String,
        @Body query: RequestBody
    ): List<AgeRatingDto>
}