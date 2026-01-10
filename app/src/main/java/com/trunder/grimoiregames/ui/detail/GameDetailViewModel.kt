package com.trunder.grimoiregames.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trunder.grimoiregames.data.entity.Game
import com.trunder.grimoiregames.data.repository.GameRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GameDetailViewModel @Inject constructor(
    private val repository: GameRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val gameId: Int = checkNotNull(savedStateHandle["gameId"])

    // Observamos el juego.
    // Al usar 'onEach', cada vez que cargamos el juego, comprobamos si le faltan datos.
    val game: Flow<Game> = repository.getGameById(gameId).onEach { game ->
        // ESTRATEGIA DE CACHÉ:
        // Si el juego existe pero no tiene descripción, significa que es la versión "resumida".
        // ¡Lanzamos la petición para enriquecerlo!
        if (game.description.isNullOrBlank()) {
            fetchDetails(game)
        }
    }

    private fun fetchDetails(game: Game) {
        viewModelScope.launch {
            repository.fetchAndSaveGameDetails(game)
        }
    }

    fun updateGameStats(currentGame: Game, newRating: Int, newHours: Int, newStatus: String) {
        viewModelScope.launch {
            val updatedGame = currentGame.copy(
                rating = newRating,
                hoursPlayed = newHours,
                status = newStatus
            )
            repository.updateGame(updatedGame)
        }
    }
}