package com.trunder.grimoiregames.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trunder.grimoiregames.data.entity.Game
import com.trunder.grimoiregames.data.repository.GameRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GameDetailViewModel @Inject constructor(
    private val repository: GameRepository,
    savedStateHandle: SavedStateHandle // <-- Aquí Hilt nos pasa los datos de navegación
) : ViewModel() {

    // 1. OBTENER EL ID
    // La clave "gameId" debe coincidir con lo que pondremos luego en MainActivity
    private val gameId: Int = checkNotNull(savedStateHandle["gameId"])

    // 2. BUSCAR EL JUEGO (Automático)
    // Al ser un Flow, si actualizamos el juego, la pantalla se entera sola
    val game: Flow<Game> = repository.getGameById(gameId)

    // 3. ACTUALIZAR DATOS
    // Esta función recibe el juego actual y los nuevos valores, crea una copia y guarda
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