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
    private val api: RawgApi // <--- ¡AQUÍ INYECTAMOS INTERNET!
) {

    // --- PARTE LOCAL (ROOM) ---
    val allGames: Flow<List<Game>> = gameDao.getAllGames()

    suspend fun addGame(game: Game) {
        gameDao.insertGame(game)
    }

    suspend fun deleteGame(game: Game) {
        gameDao.deleteGame(game)
    }

    // --- PARTE REMOTA (INTERNET) ---
    // Esta función busca juegos en RAWG
    suspend fun searchGames(query: String): List<GameDto> {
        return try {
            val response = api.searchGames(
                apiKey = Constants.API_KEY,
                query = query
            )
            response.results // Devolvemos la lista limpia
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList() // Si falla, devolvemos lista vacía para no romper la app
        }
    }
}