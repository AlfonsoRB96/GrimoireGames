package com.trunder.grimoiregames.ui.addgame

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trunder.grimoiregames.data.entity.Game
import com.trunder.grimoiregames.data.remote.dto.IgdbGameDto // 👈 CAMBIO A IGDB
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

    // 👇 AHORA LA LISTA ES DE IGDB
    var searchResults by mutableStateOf<List<IgdbGameDto>>(emptyList())
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
            delay(800L) // Un poco más de delay para no saturar a IGDB mientras escribes

            if (query.isNotEmpty()) {
                isLoading = true
                errorMessage = null

                try {
                    // El repository ya se encarga de la magia de IGDB
                    val result = repository.searchGames(query)
                    searchResults = result

                } catch (e: Exception) {
                    e.printStackTrace()
                    searchResults = emptyList()

                    val errorTexto = e.toString().lowercase()
                    // Mantenemos tus mensajes de error personalizados ;)
                    if (errorTexto.contains("json") || errorTexto.contains("malformed")) {
                        errorMessage = "🚫 ¡Bloqueo detectado!\nTu operador bloquea la conexión.\npor culpa del bloqueo de Javier Tebas.\n"
                    } else if (errorTexto.contains("ssl") || errorTexto.contains("certpath")) {
                        errorMessage = "🔒 Error de Seguridad (SSL)\nBloqueo de operadora detectado.\nUsa VPN o Datos Móviles."
                    } else {
                        errorMessage = "⚠️ Error: ${e.localizedMessage}"
                    }

                } finally {
                    isLoading = false
                }
            } else {
                searchResults = emptyList()
                errorMessage = null
                isLoading = false
            }
        }
    }

    // 👇 AÑADIMOS UN PARAMETRO NUEVO: "onSuccess"
    // Es una función que ejecutaremos SOLO cuando hayamos terminado de guardar.
    fun onGameSelected(dto: IgdbGameDto, selectedPlatform: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                // Hacemos el trabajo pesado
                repository.addGame(dto.id, selectedPlatform)

                // 👇 ¡AQUÍ ESTÁ LA MAGIA!
                // Esperamos a que termine addGame y ENTONCES llamamos a onSuccess
                onSuccess()

            } catch (e: Exception) {
                e.printStackTrace()
                // Aquí podrías manejar un error, pero de momento con el log nos vale
            }
        }
    }

}