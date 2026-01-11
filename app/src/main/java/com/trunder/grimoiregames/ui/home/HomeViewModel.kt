package com.trunder.grimoiregames.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trunder.grimoiregames.data.entity.Game
import com.trunder.grimoiregames.data.repository.GameRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.Normalizer
import javax.inject.Inject

enum class SortOption {
    TITLE,
    PLATFORM,
    STATUS
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: GameRepository
) : ViewModel() {

    private val _searchText = MutableStateFlow("")
    val searchText = _searchText.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.TITLE)
    val sortOption = _sortOption.asStateFlow()

    // Lógica principal de datos
    val games = combine(
        repository.allGames,
        _sortOption,
        _searchText
    ) { gamesList, option, text ->

        // PASO A: FILTRADO POR TÍTULO (Solo nombre del juego)
        val filteredList = if (text.isBlank()) {
            gamesList
        } else {
            // Normalizamos la búsqueda (quitar tildes)
            val query = text.removeAccents()

            gamesList.filter { game ->
                // 🔍 AHORA SOLO BUSCAMOS EN EL TÍTULO
                game.title.removeAccents().contains(query, ignoreCase = true)
            }
        }

        // PASO B: ORDENACIÓN
        val sortedList = when (option) {
            SortOption.TITLE -> filteredList.sortedBy { it.title }
            SortOption.PLATFORM -> filteredList.sortedBy { it.platform }
            SortOption.STATUS -> filteredList.sortedByDescending { it.status == "Playing" }
        }

        // PASO C: AGRUPACIÓN (Cabeceras)
        when (option) {
            SortOption.TITLE -> sortedList.groupBy {
                it.title.firstOrNull()?.toString()?.uppercase() ?: "#"
            }
            SortOption.PLATFORM -> sortedList.groupBy { it.platform }
            SortOption.STATUS -> sortedList.groupBy { it.status }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    // Actualizar texto de búsqueda
    fun onSearchTextChange(text: String) {
        _searchText.value = text
    }

    // Cambiar orden
    fun onSortChange(option: SortOption) {
        _sortOption.value = option
    }

    // Borrar juego
    fun deleteGame(game: Game) {
        viewModelScope.launch {
            repository.deleteGame(game)
        }
    }

    // Función auxiliar para normalizar texto (quitar tildes)
    private fun String.removeAccents(): String {
        return Normalizer.normalize(this, Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
    }
}