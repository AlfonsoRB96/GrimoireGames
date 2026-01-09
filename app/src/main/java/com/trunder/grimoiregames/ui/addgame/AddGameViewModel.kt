package com.trunder.grimoiregames.ui.addgame

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trunder.grimoiregames.data.entity.Game
import com.trunder.grimoiregames.data.remote.dto.GameDto
import com.trunder.grimoiregames.data.repository.GameRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddGameViewModel @Inject constructor(
    private val repository: GameRepository
) : ViewModel() {

    // ESTADO DEL BUSCADOR
    var query by mutableStateOf("") // Lo que escribe el usuario
        private set

    var searchResults by mutableStateOf<List<GameDto>>(emptyList()) // La lista de la API
        private set

    var isLoading by mutableStateOf(false) // Para mostrar una barrita de carga
        private set

    // ACCIÓN: ESCRIBIR Y BUSCAR
    fun onQueryChange(newQuery: String) {
        query = newQuery
        // Solo buscamos si ha escrito más de 2 letras para no saturar
        if (newQuery.length > 2) {
            searchGames(newQuery)
        }
    }

    private fun searchGames(query: String) {
        viewModelScope.launch {
            isLoading = true
            // ¡Llamada a Internet! 🌍
            searchResults = repository.searchGames(query)
            isLoading = false
        }
    }

    // ACCIÓN: GUARDAR JUEGO (Al hacer click en un resultado)
    fun onGameSelected(dto: GameDto, selectedPlatform: String) {
        viewModelScope.launch {
            val newGame = Game(
                title = dto.name,
                platform = selectedPlatform, // <--- ¡USAMOS LA REAL!
                status = "Backlog"
                // (Más adelante añadiremos la imagen aquí)
            )
            repository.addGame(newGame)
        }
    }
}