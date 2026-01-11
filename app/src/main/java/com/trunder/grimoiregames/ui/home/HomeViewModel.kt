package com.trunder.grimoiregames.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trunder.grimoiregames.data.entity.Game
import com.trunder.grimoiregames.data.repository.GameRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SortOption {
    TITLE,
    PLATFORM,
    STATUS
}

// @HiltViewModel: ¡Hey Hilt! Prepara esta clase para inyectarla en la vista.
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: GameRepository
) : ViewModel() {

    private val _sortOption = MutableStateFlow(SortOption.TITLE)
    val sortOption = _sortOption.asStateFlow()

    // Cambiamos el tipo de retorno: Ahora es un Map<String, List<Game>>
    val games = combine(
        repository.allGames,
        _sortOption
    ) { gamesList, option ->
        // 1. Primero filtramos/ordenamos la lista base para asegurar el orden correcto
        val sortedList = when (option) {
            SortOption.TITLE -> gamesList.sortedBy { it.title }
            SortOption.PLATFORM -> gamesList.sortedBy { it.platform }
            SortOption.STATUS -> gamesList.sortedByDescending { it.status == "Playing" } // Playing primero
        }

        // 2. Ahora AGRUPAMOS según el criterio elegido
        when (option) {
            SortOption.TITLE -> sortedList.groupBy {
                // Agrupar por la primera letra (A, B, C...)
                it.title.firstOrNull()?.toString()?.uppercase() ?: "#"
            }
            SortOption.PLATFORM -> sortedList.groupBy {
                // Agrupar por nombre de consola exacto
                it.platform
            }
            SortOption.STATUS -> sortedList.groupBy {
                // Agrupar por estado
                it.status
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap() // 👈 OJO: Ahora el valor inicial es un mapa vacío
    )

    // 4. Función para cambiar el orden desde la UI
    fun onSortChange(option: SortOption) {
        _sortOption.value = option
    }

    // ACCIÓN: BORRAR JUEGO
    fun deleteGame(game: Game) {
        viewModelScope.launch {
            repository.deleteGame(game)
        }
    }
}