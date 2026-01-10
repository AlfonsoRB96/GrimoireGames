package com.trunder.grimoiregames.data.repository

import com.trunder.grimoiregames.data.dao.GameDao // Asegúrate de que este import es correcto (o .local.GameDao)
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

    suspend fun addGame(game: Game) {
        gameDao.insertGame(game)
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

    // --- PARTE REMOTA (INTERNET) ---

    // 👇 AQUÍ ESTÁ EL CAMBIO CRÍTICO 👇
    // Hemos quitado el try-catch. Ahora si falla, el ViewModel se entera.
    suspend fun searchGames(query: String): List<GameDto> {
        // Llamamos a la API directamente
        val response = api.searchGames(
            apiKey = Constants.API_KEY,
            query = query // Usamos "query" (o "search" si tu interfaz lo pide así)
        )
        return response.results // Devolvemos la lista limpia
    }

    // Esta función la dejamos con try-catch porque corre en segundo plano
    // y no queremos que moleste si falla al actualizar detalles.
    suspend fun fetchAndSaveGameDetails(game: Game) {
        try {
            // Usamos rawgId si existe (asegúrate de que tu Entity Game tiene este campo)
            val details = api.getGameDetails(
                id = game.rawgId,
                apiKey = Constants.API_KEY
            )

            // Creamos una copia del juego con los datos nuevos
            val enrichedGame = game.copy(
                description = details.description,
                metacritic = details.metacritic,
                releaseDate = details.released,
                genre = details.genres?.joinToString(", ") { it.name },
                developer = details.developers?.joinToString(", ") { it.name },
                publisher = details.publishers?.joinToString(", ") { it.name }
            )

            // Guardamos en base de datos
            gameDao.updateGame(enrichedGame)

        } catch (e: Exception) {
            e.printStackTrace()
            // Si falla silenciosamente, no pasa nada, se queda con los datos básicos
        }
    }
}