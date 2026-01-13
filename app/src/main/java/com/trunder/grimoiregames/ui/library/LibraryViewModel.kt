package com.trunder.grimoiregames.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trunder.grimoiregames.data.entity.Game
import com.trunder.grimoiregames.data.repository.GameRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.Normalizer
import javax.inject.Inject

enum class SortOption {
    TITLE,
    PLATFORM,
    STATUS,
    TIER
}

data class GameFilters(
    val platforms: Set<String> = emptySet(),
    val genres: Set<String> = emptySet(),
    val developers: Set<String> = emptySet(),
    val publishers: Set<String> = emptySet(),
    val statuses: Set<String> = emptySet(),
    val ageRatings: Set<String> = emptySet(),
    val metacriticRanges: Set<String> = emptySet(),
    val releaseYears: Set<String> = emptySet(),
    val tiers: Set<String> = emptySet() // Guardamos las letras: "S+", "S", "A"...
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: GameRepository
) : ViewModel() {

    private val _searchText = MutableStateFlow("")
    val searchText = _searchText.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.TITLE)
    val sortOption = _sortOption.asStateFlow()

    private val _filters = MutableStateFlow(GameFilters())
    val filters = _filters.asStateFlow()

    // 🧠 MAPEO NÚMERO -> LETRA (TIER)
    // Ajusta los rangos si tu sistema era diferente
    private fun calculateTier(rating: Int?): String {
        return when (rating) {
            10 -> "S+"
            9 -> "S"
            8 -> "A"
            7 -> "B"
            5 -> "C"
            3 -> "D"
            null -> "Sin Puntuación"
            else -> "F" // 0, 1, 2, 3, 4
        }
    }

    // 👇 GENERADOR DE OPCIONES
    val availableData = repository.allGames.map { games ->
        mapOf(
            "Plataforma" to games.map { it.platform }.distinct().sorted(),
            "Género" to games.mapNotNull { it.genre }.filter { it.isNotBlank() }.distinct().sorted(),
            "Estado" to listOf("Playing", "Completed", "Backlog"),
            "Desarrolladora" to games.mapNotNull { it.developer }.filter { it.isNotBlank() }.distinct().sorted(),
            "Distribuidora" to games.mapNotNull { it.publisher }.filter { it.isNotBlank() }.distinct().sorted(),
            "Clasificación por edades" to games.mapNotNull { it.ageRating }.filter { it.isNotBlank() }.distinct().sorted(),

            // 🔴 CAMBIO AQUÍ: EN LUGAR DE CALCULARLO, PONEMOS LA LISTA FIJA
            // Así siempre salen todas las opciones, tengas juegos puntuados o no.
            "Tier Personal" to listOf("S+", "S", "A", "B", "C", "D", "Sin Puntuación"),

            "Metacritic" to listOf("90+ (Obra Maestra)", "75-89 (Muy Bueno)", "50-74 (Mixto)", "0-49 (Malo)", "N/A (Sin puntuación)"),
            "Año de Lanzamiento" to games.mapNotNull {
                it.releaseDate?.take(4)
            }.distinct().sortedDescending()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    // --- DIALOGO DE ACTUALIZACIÓN ---
    private val _showUpdateDialog = MutableStateFlow(false)
    val showUpdateDialog = _showUpdateDialog.asStateFlow()

    private val _updateProgress = MutableStateFlow(0)
    val updateProgress = _updateProgress.asStateFlow()

    fun refreshLibrary() {
        viewModelScope.launch {
            _showUpdateDialog.value = true
            _updateProgress.value = 0
            repository.updateAllGames().collect { percentage ->
                _updateProgress.value = percentage
            }
            _showUpdateDialog.value = false
            _updateProgress.value = 0
        }
    }

    // 🧠 LÓGICA MAESTRA
    val games = combine(
        repository.allGames,
        _sortOption,
        _searchText,
        _filters
    ) { gamesList, option, text, currentFilters ->

        var result = gamesList

        // 1. TEXTO
        if (text.isNotBlank()) {
            val query = text.removeAccents()
            result = result.filter {
                it.title.removeAccents().contains(query, ignoreCase = true)
            }
        }

        // 2. FILTROS
        if (currentFilters.platforms.isNotEmpty()) {
            result = result.filter { it.platform in currentFilters.platforms }
        }
        if (currentFilters.genres.isNotEmpty()) {
            result = result.filter { it.genre != null && it.genre in currentFilters.genres }
        }
        if (currentFilters.statuses.isNotEmpty()) {
            result = result.filter { it.status in currentFilters.statuses }
        }
        if (currentFilters.developers.isNotEmpty()) {
            result = result.filter { it.developer != null && it.developer in currentFilters.developers }
        }
        if (currentFilters.publishers.isNotEmpty()) {
            result = result.filter { it.publisher != null && it.publisher in currentFilters.publishers }
        }
        if (currentFilters.ageRatings.isNotEmpty()) {
            result = result.filter { it.ageRating != null && it.ageRating in currentFilters.ageRatings }
        }

        // 🆕 FILTRO DE TIER CORREGIDO
        // Convertimos el número del juego a letra y miramos si esa letra está seleccionada
        if (currentFilters.tiers.isNotEmpty()) {
            result = result.filter { game ->
                val tierLetter = calculateTier(game.userRating)
                tierLetter in currentFilters.tiers
            }
        }

        if (currentFilters.metacriticRanges.isNotEmpty()) {
            result = result.filter { game ->
                val score = game.metacriticPress ?: 0
                currentFilters.metacriticRanges.any { range ->
                    when {
                        range.startsWith("90+") -> score >= 90
                        range.startsWith("75") -> score in 75..89
                        range.startsWith("50") -> score in 50..74
                        else -> score < 50
                    }
                }
            }
        }
        if (currentFilters.releaseYears.isNotEmpty()) {
            result = result.filter { game ->
                val year = game.releaseDate?.take(4)
                year != null && year in currentFilters.releaseYears
            }
        }

        // 4. ORDENACIÓN
        val sortedList = when (option) {
            SortOption.TITLE -> result.sortedBy { it.title }
            SortOption.PLATFORM -> result.sortedBy { it.platform }
            SortOption.STATUS -> result.sortedByDescending { it.status == "Playing" }
            // 🆕 Ordenar por valor numérico (10 es mejor que 9), manejando nulos al final
            SortOption.TIER -> result.sortedByDescending { it.userRating ?: -1 }
        }

        // 5. AGRUPACIÓN
        when (option) {
            SortOption.TITLE -> sortedList.groupBy {
                it.title.firstOrNull()?.toString()?.uppercase() ?: "#"
            }
            SortOption.PLATFORM -> sortedList.groupBy { it.platform }
            SortOption.STATUS -> sortedList.groupBy { it.status }
            // 🆕 Agrupar por la letra calculada
            SortOption.TIER -> sortedList.groupBy { calculateTier(it.userRating) }
        }

    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    // --- ACCIONES DE UI ---

    fun onSearchTextChange(text: String) {
        _searchText.value = text
    }

    fun onSortChange(option: SortOption) {
        _sortOption.value = option
    }

    fun deleteGame(game: Game) {
        viewModelScope.launch {
            repository.deleteGame(game)
        }
    }

    fun toggleFilter(category: String, value: String) {
        val current = _filters.value
        _filters.value = when (category) {
            "Plataforma" -> current.copy(platforms = toggleSet(current.platforms, value))
            "Género" -> current.copy(genres = toggleSet(current.genres, value))
            "Estado" -> current.copy(statuses = toggleSet(current.statuses, value))
            "Desarrolladora" -> current.copy(developers = toggleSet(current.developers, value))
            "Distribuidora" -> current.copy(publishers = toggleSet(current.publishers, value))
            "Clasificación por edades" -> current.copy(ageRatings = toggleSet(current.ageRatings, value))
            "Tier Personal" -> current.copy(tiers = toggleSet(current.tiers, value)) // 🆕
            "Metacritic" -> current.copy(metacriticRanges = toggleSet(current.metacriticRanges, value))
            "Año de Lanzamiento" -> current.copy(releaseYears = toggleSet(current.releaseYears, value))
            else -> current
        }
    }

    fun clearAllFilters() {
        _filters.value = GameFilters()
    }

    private fun toggleSet(set: Set<String>, item: String): Set<String> {
        return if (set.contains(item)) {
            set - item
        } else {
            set + item
        }
    }

    private fun String.removeAccents(): String {
        return Normalizer.normalize(this, Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
    }
}