package com.trunder.grimoiregames.data.repository

import com.trunder.grimoiregames.data.dao.GameDao
import com.trunder.grimoiregames.data.entity.Game
import com.trunder.grimoiregames.data.remote.RawgApi
import com.trunder.grimoiregames.data.remote.dto.GameDto
import com.trunder.grimoiregames.util.Constants
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GameRepository @Inject constructor(
    private val gameDao: GameDao,
    private val api: RawgApi
) {

    // --- PARTE LOCAL (ROOM) ---
    val allGames: Flow<List<Game>> = gameDao.getAllGames()

    // 👇 FUNCIÓN ADAPTADA A TU DTO (CAMELCASE)
    suspend fun addGame(game: Game) {
        try {
            // 1. Pedimos detalles a la API
            val details = api.getGameDetails(
                id = game.rawgId,
                apiKey = Constants.API_KEY
            )

            // 2. Mapeamos usando las variables de TU DTO (GameDetailDto)
            val enrichedGame = game.copy(
                // Tu DTO ya mapea "description_raw" a la variable "description", así que usamos esa:
                description = details.description,

                // Usamos firstOrNull() para coger solo el principal y que quede limpio en la lista
                genre = details.genres?.firstOrNull()?.name ?: "Desconocido",
                developer = details.developers?.firstOrNull()?.name ?: "Desconocido",
                publisher = details.publishers?.firstOrNull()?.name ?: "Desconocido",

                metacritic = details.metacritic,
                releaseDate = details.released, // Tu DTO lo llama "released"

                // Tu DTO lo llama "esrbRating", no "esrb_rating"
                esrb = details.esrbRating?.name,

                // RAWG no suele dar PEGI directo en este endpoint, lo dejamos null por seguridad
                pegi = null
            )

            // 3. Guardamos
            gameDao.insertGame(enrichedGame)

        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback: Si falla, guardamos el básico
            gameDao.insertGame(game)
        }
    }

    suspend fun deleteGame(game: Game) {
        gameDao.deleteGame(game)
    }

    suspend fun updateGame(game: Game) {
        gameDao.updateGame(game)
    }

    fun getGameById(id: Int): Flow<Game> {
        return gameDao.getGameById(id)
    }

    // --- PARTE REMOTA ---

    suspend fun searchGames(query: String): List<GameDto> {
        val response = api.searchGames(
            apiKey = Constants.API_KEY,
            query = query
        )
        return response.results
    }

    // 👇 TAMBIÉN ADAPTADA PARA ACTUALIZAR DATOS ANTIGUOS
    suspend fun fetchAndSaveGameDetails(game: Game) {
        try {
            val details = api.getGameDetails(
                id = game.rawgId,
                apiKey = Constants.API_KEY
            )

            // Usamos joinToString para guardar TODOS los géneros separados por coma si quieres más detalle
            // O firstOrNull() si prefieres solo el principal. Aquí te pongo joinToString por si acaso.
            val enrichedGame = game.copy(
                description = details.description,
                metacritic = details.metacritic,
                releaseDate = details.released,
                genre = details.genres?.joinToString(", ") { it.name },
                developer = details.developers?.joinToString(", ") { it.name },
                publisher = details.publishers?.joinToString(", ") { it.name },
                esrb = details.esrbRating?.name
            )

            gameDao.updateGame(enrichedGame)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}