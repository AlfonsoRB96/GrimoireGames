package com.trunder.grimoiregames.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trunder.grimoiregames.data.entity.Game
import com.trunder.grimoiregames.data.repository.GameRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.Normalizer
import javax.inject.Inject

enum class SortOption {
    TITLE,
    PLATFORM,
    STATUS,
    TIER,
    HOURS,
    FRANCHISE
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
    val tiers: Set<String> = emptySet(),
    val hourRanges: Set<String> = emptySet(),
    val franchises: Set<String> = emptySet()
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

    private fun calculateTier(rating: Int?): String {
        return when (rating) {
            10 -> "S+"
            9 -> "S"
            8 -> "A"
            7 -> "B"
            5 -> "C"
            3 -> "D"
            null -> "Sin Puntuación"
            else -> "F"
        }
    }

    // Esta función la usamos tanto para Agrupar como para Filtrar
    private fun getHourGroupLabel(hours: Int): String {
        return when {
            hours >= 1000 -> "+1000 horas"
            hours >= 500 -> "+500 horas"
            hours >= 100 -> "+100 horas"
            hours >= 50 -> "+50 horas"
            hours >= 25 -> "+25 horas"
            hours >= 10 -> "+10 horas"
            hours >= 5 -> "+5 horas"
            hours >= 4 -> "+4 horas"
            hours >= 3 -> "+3 horas"
            hours >= 2 -> "+2 horas"
            hours >= 1 -> "+1 hora"
            else -> "Sin empezar"
        }
    }

    // 👇 GENERADOR DE OPCIONES (Aquí es donde decimos qué filtros existen)
    val availableData = repository.allGames.map { games ->
        mapOf(
            "Plataforma" to games.map { it.platform }.distinct().sorted(),
            "Género" to games.mapNotNull { it.genre }.filter { it.isNotBlank() }.distinct().sorted(),
            "Estado" to listOf("Playing", "Completed", "Backlog"),
            // 🟢 AÑADIDO: FRANQUICIA A LA LISTA DE FILTROS DISPONIBLES
            "Franquicia" to games.mapNotNull { it.franchise }.filter { it.isNotBlank() }.distinct().sorted(),
            "Desarrolladora" to games.mapNotNull { it.developer }.filter { it.isNotBlank() }.distinct().sorted(),
            "Distribuidora" to games.mapNotNull { it.publisher }.filter { it.isNotBlank() }.distinct().sorted(),
            "Clasificación por edades" to games.mapNotNull { it.ageRating }.filter { it.isNotBlank() }.distinct().sorted(),
            "Tier Personal" to listOf("S+", "S", "A", "B", "C", "D", "Sin Puntuación"),
            "Metacritic" to listOf("90+ (Obra Maestra)", "75-89 (Muy Bueno)", "50-74 (Mixto)", "0-49 (Malo)", "N/A (Sin puntuación)"),
            "Año de Lanzamiento" to games.mapNotNull { it.releaseDate?.take(4) }.distinct().sortedDescending(),
            "Horas de Juego" to listOf(
                "+1000 horas", "+500 horas", "+100 horas", "+50 horas",
                "+25 horas", "+10 horas", "+5 horas", "+4 horas",
                "+3 horas", "+2 horas", "+1 hora", "Sin empezar"
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    // ... (Dialogo update omitido por brevedad, no cambia) ...
    // Solo pongo el refreshLibrary si lo necesitas, pero no cambia.
    private val _showUpdateDialog = MutableStateFlow(false)
    val showUpdateDialog = _showUpdateDialog.asStateFlow()
    private val _updateProgress = MutableStateFlow(0)
    val updateProgress = _updateProgress.asStateFlow()
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    fun refreshLibrary() {
        viewModelScope.launch {
            // 1. Encendemos el indicador de carga (Activa el spinner del PullToRefresh)
            _isRefreshing.value = true
            _showUpdateDialog.value = true

            // Opcional: Si quieres que TAMBIÉN salga el diálogo flotante, descomenta esto:
            // _showUpdateDialog.value = true

            try {
                // 2. Llamamos a la artillería pesada del Repositorio
                repository.updateAllGames()
                    .collect { percentage ->
                        // Aquí recibimos el progreso (0%, 10%... 100%)
                        _updateProgress.value = percentage

                        // Si usas logs, puedes ver el avance:
                        // android.util.Log.d("GrimoireUpdate", "Progreso: $percentage%")
                    }

                // 3. ¡Misión Cumplida!
                // Si llegamos aquí es que el Flow terminó exitosamente.

            } catch (e: Exception) {
                e.printStackTrace()
                // Aquí podrías guardar un mensaje de error en un StateFlow para mostrar un Snackbar
            } finally {
                // 4. Apagamos las luces al salir (Siempre se ejecuta, haya error o no)
                _isRefreshing.value = false
                _showUpdateDialog.value = false // Ocultamos diálogo si lo usaste
                _updateProgress.value = 0       // Reseteamos contador
            }
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

        // 2. FILTROS (Estándar)
        if (currentFilters.platforms.isNotEmpty()) result = result.filter { it.platform in currentFilters.platforms }
        if (currentFilters.genres.isNotEmpty()) result = result.filter { it.genre != null && it.genre in currentFilters.genres }
        if (currentFilters.statuses.isNotEmpty()) result = result.filter { it.status in currentFilters.statuses }
        if (currentFilters.franchises.isNotEmpty()) result = result.filter { it.franchise != null && it.franchise in currentFilters.franchises }
        if (currentFilters.developers.isNotEmpty()) result = result.filter { it.developer != null && it.developer in currentFilters.developers }
        if (currentFilters.publishers.isNotEmpty()) result = result.filter { it.publisher != null && it.publisher in currentFilters.publishers }
        if (currentFilters.ageRatings.isNotEmpty()) result = result.filter { it.ageRating != null && it.ageRating in currentFilters.ageRatings }
        if (currentFilters.tiers.isNotEmpty()) {
            result = result.filter { calculateTier(it.userRating) in currentFilters.tiers }
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
            result = result.filter { (it.releaseDate?.take(4)) in currentFilters.releaseYears }
        }

        // 🆕 3. FILTRO DE HORAS (LÓGICA ACUMULATIVA / UMBRALES)
        if (currentFilters.hourRanges.isNotEmpty()) {
            result = result.filter { game ->
                val hours = game.hoursPlayed ?: 0

                // El juego pasa si cumple CUALQUIERA de los umbrales seleccionados
                currentFilters.hourRanges.any { filterLabel ->
                    if (filterLabel == "Sin empezar") {
                        // Caso especial: Queremos exactamente 0 horas
                        hours == 0
                    } else {
                        // Caso acumulativo: "+10 horas" significa ">= 10"
                        // Truco: Extraemos solo los dígitos del texto ("+50 horas" -> "50")
                        val minHours = filterLabel.filter { it.isDigit() }.toIntOrNull() ?: Int.MAX_VALUE

                        // La condición matemática: ¿Mis horas superan este umbral?
                        hours >= minHours
                    }
                }
            }
        }

        // 4. ORDENACIÓN
        val sortedList = when (option) {
            SortOption.TITLE -> result.sortedBy { it.title }
            SortOption.PLATFORM -> result.sortedBy { it.platform }
            SortOption.STATUS -> result.sortedByDescending { it.status == "Playing" }
            SortOption.TIER -> result.sortedByDescending { it.userRating ?: -1 }
            SortOption.HOURS -> result.sortedByDescending { it.hoursPlayed ?: 0 }
            SortOption.FRANCHISE -> result.sortedBy { it.franchise ?: "ZZZ" }
        }

        // 5. AGRUPACIÓN
        when (option) {
            SortOption.TITLE -> sortedList.groupBy { it.title.firstOrNull()?.toString()?.uppercase() ?: "#" }
            SortOption.PLATFORM -> sortedList.groupBy { it.platform }
            SortOption.STATUS -> sortedList.groupBy { it.status }
            SortOption.TIER -> sortedList.groupBy { calculateTier(it.userRating) }
            SortOption.HOURS -> sortedList.groupBy { getHourGroupLabel(it.hoursPlayed ?: 0) }
            SortOption.FRANCHISE -> sortedList.groupBy { it.franchise ?: "Sin Franquicia" }
        }

    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    // --- ACCIONES DE UI ---

    fun onSearchTextChange(text: String) { _searchText.value = text }
    fun onSortChange(option: SortOption) { _sortOption.value = option }
    fun deleteGame(game: Game) { viewModelScope.launch { repository.deleteGame(game) } }
    fun clearAllFilters() { _filters.value = GameFilters() }

    fun toggleFilter(category: String, value: String) {
        val current = _filters.value
        _filters.value = when (category) {
            "Plataforma" -> current.copy(platforms = toggleSet(current.platforms, value))
            "Género" -> current.copy(genres = toggleSet(current.genres, value))
            "Estado" -> current.copy(statuses = toggleSet(current.statuses, value))
            "Desarrolladora" -> current.copy(developers = toggleSet(current.developers, value))
            "Distribuidora" -> current.copy(publishers = toggleSet(current.publishers, value))
            "Clasificación por edades" -> current.copy(ageRatings = toggleSet(current.ageRatings, value))
            "Tier Personal" -> current.copy(tiers = toggleSet(current.tiers, value))
            "Metacritic" -> current.copy(metacriticRanges = toggleSet(current.metacriticRanges, value))
            "Año de Lanzamiento" -> current.copy(releaseYears = toggleSet(current.releaseYears, value))
            "Horas de Juego" -> current.copy(hourRanges = toggleSet(current.hourRanges, value)) // 🆕
            else -> current
        }
    }

    private fun toggleSet(set: Set<String>, item: String): Set<String> {
        return if (set.contains(item)) set - item else set + item
    }

    private fun String.removeAccents(): String {
        return Normalizer.normalize(this, Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
    }
}