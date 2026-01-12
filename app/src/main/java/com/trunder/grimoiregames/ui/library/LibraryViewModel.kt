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
    STATUS
}

// 👇 CLASE DE DATOS PARA ALMACENAR LOS FILTROS ACTIVOS
data class GameFilters(
    val platforms: Set<String> = emptySet(),
    val genres: Set<String> = emptySet(),
    val developers: Set<String> = emptySet(),
    val publishers: Set<String> = emptySet(),
    val statuses: Set<String> = emptySet(),
    val pegis: Set<String> = emptySet(),
    val esrbs: Set<String> = emptySet(),
    val metacriticRanges: Set<String> = emptySet(),
    val releaseYears: Set<String> = emptySet()
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: GameRepository
) : ViewModel() {

    // ESTADOS BÁSICOS
    private val _searchText = MutableStateFlow("")
    val searchText = _searchText.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.TITLE)
    val sortOption = _sortOption.asStateFlow()

    // ESTADO DE FILTROS
    private val _filters = MutableStateFlow(GameFilters())
    val filters = _filters.asStateFlow()

    // 👇 GENERADOR DE OPCIONES DISPONIBLES
    val availableData = repository.allGames.map { games ->
        mapOf(
            "Plataforma" to games.map { it.platform }.distinct().sorted(),
            "Género" to games.mapNotNull { it.genre }.filter { it.isNotBlank() }.distinct().sorted(),
            "Estado" to listOf("Playing", "Completed", "Backlog"),
            "Desarrolladora" to games.mapNotNull { it.developer }.filter { it.isNotBlank() }.distinct().sorted(),
            "Distribuidora" to games.mapNotNull { it.publisher }.filter { it.isNotBlank() }.distinct().sorted(),
            "PEGI" to games.mapNotNull { it.pegi }.filter { it.isNotBlank() }.distinct().sorted(),
            "ESRB" to games.mapNotNull { it.esrb }.filter { it.isNotBlank() }.distinct().sorted(),
            "Metacritic" to listOf("90+ (Obra Maestra)", "75-89 (Muy Bueno)", "50-74 (Mixto)", "0-49 (Malo)", "N/A (Sin puntuación"),
            "Año de Lanzamiento" to games.mapNotNull {
                it.releaseDate?.take(4)
            }.distinct().sortedDescending()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    // --- 🆕 LÓGICA DE ACTUALIZACIÓN (MODAL DE PROGRESO) ---

    // Control del Diálogo Modal (Visible/Invisible)
    private val _showUpdateDialog = MutableStateFlow(false)
    val showUpdateDialog = _showUpdateDialog.asStateFlow()

    // Porcentaje de progreso (0 a 100)
    private val _updateProgress = MutableStateFlow(0)
    val updateProgress = _updateProgress.asStateFlow()

    // (Hemos eliminado _isRefreshing porque ya no usamos la ruleta nativa)

    fun refreshLibrary() {
        viewModelScope.launch {
            _showUpdateDialog.value = true
            _updateProgress.value = 0

            // Escuchamos el Flow del repositorio que emite el % completado
            repository.updateAllGames().collect { percentage ->
                _updateProgress.value = percentage
            }

            // Al terminar (cuando el Flow se cierra)
            _showUpdateDialog.value = false
            _updateProgress.value = 0
        }
    }

    // 🧠 LÓGICA MAESTRA: COMBINAR TODO
    val games = combine(
        repository.allGames,
        _sortOption,
        _searchText,
        _filters
    ) { gamesList, option, text, currentFilters ->

        var result = gamesList

        // 1. FILTRO DE TEXTO
        if (text.isNotBlank()) {
            val query = text.removeAccents()
            result = result.filter {
                it.title.removeAccents().contains(query, ignoreCase = true)
            }
        }

        // 2. FILTROS DINÁMICOS
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

        if (currentFilters.pegis.isNotEmpty()) {
            result = result.filter { it.pegi != null && it.pegi in currentFilters.pegis }
        }

        if (currentFilters.esrbs.isNotEmpty()) {
            result = result.filter { it.esrb != null && it.esrb in currentFilters.esrbs }
        }

        // 3. LÓGICA ESPECIAL PARA METACRITIC
        if (currentFilters.metacriticRanges.isNotEmpty()) {
            result = result.filter { game ->
                val score = game.metacriticPress ?: 0
                currentFilters.metacriticRanges.any { range ->
                    when {
                        range.startsWith("90+") -> score >= 90
                        range.startsWith("75") -> score in 75..89
                        range.startsWith("50") -> score in 50..74
                        else -> score < 50 // Para "0-49"
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
        }

        // 5. AGRUPACIÓN
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
            "PEGI" -> current.copy(pegis = toggleSet(current.pegis, value))
            "ESRB" -> current.copy(esrbs = toggleSet(current.esrbs, value))
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