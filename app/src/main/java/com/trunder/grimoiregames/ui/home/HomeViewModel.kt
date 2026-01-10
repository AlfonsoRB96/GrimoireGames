package com.trunder.grimoiregames.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trunder.grimoiregames.data.entity.Game
import com.trunder.grimoiregames.data.repository.GameRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// @HiltViewModel: ¡Hey Hilt! Prepara esta clase para inyectarla en la vista.
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: GameRepository
) : ViewModel() {

    // ESTRATEGIA DE DATOS (The Feed)
    // Convertimos el Flow del repositorio en un StateFlow.
    // Esto hace que la UI siempre tenga el estado más reciente de la lista.
    // Si añades un juego en la BBDD, esta variable se actualiza SOLA mágicamente.
    val games: StateFlow<List<Game>> = repository.allGames
        .stateIn(
            scope = viewModelScope, // Vive mientras viva el ViewModel
            started = SharingStarted.WhileSubscribed(5000), // Pausa si la app se minimiza > 5s
            initialValue = emptyList() // Estado inicial vacío
        )

    // ACCIÓN: BORRAR JUEGO
    fun deleteGame(game: Game) {
        viewModelScope.launch {
            repository.deleteGame(game)
        }
    }
}