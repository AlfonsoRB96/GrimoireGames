package com.trunder.grimoiregames.data.remote

import com.trunder.grimoiregames.data.remote.dto.GameSearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface RawgApi {

    // Definimos un endpoint HTTP GET
    // La URL base será "https://api.rawg.io/api/"
    // Así que esto llama a: "https://api.rawg.io/api/games?key=TU_KEY&search=Zelda"
    @GET("games")
    suspend fun searchGames(
        @Query("key") apiKey: String,
        @Query("search") query: String,
        @Query("page_size") pageSize: Int = 10 // Limitamos a 10 resultados para no saturar
    ): GameSearchResponse
}