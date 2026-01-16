package com.trunder.grimoiregames.data.remote

import com.trunder.grimoiregames.data.remote.dto.AgeRatingDto
import com.trunder.grimoiregames.data.remote.dto.IgdbGameDto
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface IgdbApi {
    // Para buscar juegos
    @POST("games")
    suspend fun getGames(
        @Header("Client-ID") clientId: String,
        @Header("Authorization") authorization: String,
        @Body query: RequestBody
    ): List<IgdbGameDto>

    // Para obtener los detalles de la edad (El "Francotirador")
    @POST("age_ratings")
    suspend fun getAgeRatingDetails(
        @Header("Client-ID") clientId: String,
        @Header("Authorization") authorization: String,
        @Body query: RequestBody
    ): List<AgeRatingDto>
}