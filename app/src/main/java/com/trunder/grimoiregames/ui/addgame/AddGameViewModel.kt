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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddGameViewModel @Inject constructor(
    private val repository: GameRepository
) : ViewModel() {

    var query by mutableStateOf("")
        private set

    var searchResults by mutableStateOf<List<GameDto>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    private var searchJob: Job? = null

    fun onQueryChange(newQuery: String) {
        query = newQuery
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(500L)

            if (query.isNotEmpty()) {
                isLoading = true
                errorMessage = null // Limpiamos errores previos

                try {
                    val result = repository.searchGames(query)
                    searchResults = result

                } catch (e: Exception) {
                    e.printStackTrace()
                    searchResults = emptyList() // Limpiamos la lista para que no se mezcle

                    val errorTexto = e.toString().lowercase()

                    if (errorTexto.contains("json") || errorTexto.contains("malformed")) {
                        errorMessage = "🚫 ¡Bloqueo detectado (HTML)!\nTu operador bloquea la conexión.\npor culpa del bloqueo de parte de Javier Tebas\ny la Liga de Fútbol Profesiona.\n"
                    } else if (errorTexto.contains("ssl") || errorTexto.contains("certpath") || errorTexto.contains("handshake")) {
                        errorMessage = "🔒 Error de Seguridad (SSL)\nTu operador impide la conexión segura\npor culpa del bloqueo de parte de Javier Tebas\ny la Liga de Fútbol Profesiona.\nUsa VPN."
                    } else {
                        errorMessage = "⚠️ Error: ${e.localizedMessage}"
                    }

                } finally {
                    isLoading = false
                    // Confirmación final
                }
            } else {
                searchResults = emptyList()
                errorMessage = null
                isLoading = false
            }
        }
    }

    fun onGameSelected(dto: GameDto, selectedPlatform: String) {
        viewModelScope.launch {
            val newGame = Game(
                rawgId = dto.id,
                title = dto.name,
                platform = selectedPlatform,
                status = "Backlog",
                imageUrl = dto.backgroundImage
            )
            repository.addGame(newGame)
        }
    }
}