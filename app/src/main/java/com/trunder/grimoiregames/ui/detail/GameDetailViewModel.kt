package com.trunder.grimoiregames.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trunder.grimoiregames.data.entity.Game
import com.trunder.grimoiregames.data.repository.GameRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import java.util.Locale

@HiltViewModel
class GameDetailViewModel @Inject constructor(
    private val repository: GameRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val gameId: Int = checkNotNull(savedStateHandle["gameId"])

    // Observamos el juego directamente de la BD (Source of Truth).
    // Si actualizamos la BD con onUserRatingChanged, este Flow emitirá el nuevo valor automáticamente.
    val game: Flow<Game> = repository.getGameById(gameId).onEach { game ->
        if (game.description.isNullOrBlank()) {
            fetchDetails(game)
        }
    }

    // --- LÓGICA DE TRADUCCIÓN ---
    private val _translatedDescription = MutableStateFlow<String?>(null)
    val translatedDescription = _translatedDescription.asStateFlow()

    private val _isTranslating = MutableStateFlow(false)
    val isTranslating = _isTranslating.asStateFlow()

    fun translateDescription(text: String?) {
        if (text.isNullOrBlank()) return

        val deviceLangCode = Locale.getDefault().language
        if (deviceLangCode == "en") {
            _translatedDescription.value = text
            return
        }

        val targetLang = TranslateLanguage.fromLanguageTag(deviceLangCode)
        if (targetLang == null) {
            _translatedDescription.value = "Idioma ($deviceLangCode) no disponible para traducción."
            return
        }

        viewModelScope.launch {
            _isTranslating.value = true
            try {
                val options = TranslatorOptions.Builder()
                    .setSourceLanguage(TranslateLanguage.ENGLISH)
                    .setTargetLanguage(targetLang)
                    .build()
                val translator = Translation.getClient(options)
                val conditions = DownloadConditions.Builder().requireWifi().build()

                translator.downloadModelIfNeeded(conditions)
                    .addOnSuccessListener {
                        translator.translate(text)
                            .addOnSuccessListener { translated ->
                                _translatedDescription.value = translated
                                _isTranslating.value = false
                                translator.close()
                            }
                            .addOnFailureListener {
                                _translatedDescription.value = "Error al traducir."
                                _isTranslating.value = false
                                translator.close()
                            }
                    }
                    .addOnFailureListener {
                        _translatedDescription.value = "Se requiere WiFi para descargar el idioma."
                        _isTranslating.value = false
                    }
            } catch (e: Exception) {
                e.printStackTrace()
                _isTranslating.value = false
            }
        }
    }

    private fun fetchDetails(game: Game) {
        viewModelScope.launch {
            repository.fetchAndSaveGameDetails(game)
        }
    }

    // --- AUTOGUARDADO INMEDIATO (PASO 1 INTEGRADO) ---

    // 1. Guardar Estado (Playing, Backlog...)
    fun updateStatus(game: Game, newStatus: String) {
        viewModelScope.launch {
            repository.updateGame(game.copy(status = newStatus))
        }
    }

    // 2. Guardar Tier Personal (userRating)
    // Se llama directamente desde el click del Chip "S, A, B..."
    fun onUserRatingChanged(game: Game, newRating: Int) {
        viewModelScope.launch {
            // Usamos 'userRating' (tu Tier), no 'rating' (nota de IGDB)
            repository.updateGame(game.copy(userRating = newRating))
            android.util.Log.d("GRIMOIRE", "💾 Tier guardado: $newRating")
        }
    }

    // 3. Guardar Horas de Juego (playTime)
    // Se llama desde el slider o selector de horas
    fun onPlayTimeChanged(game: Game, newHours: Int) {
        viewModelScope.launch {
            // Asegúrate de que en tu Entity se llame 'playTime' o 'hoursPlayed'
            repository.updateGame(game.copy(hoursPlayed = newHours))
            android.util.Log.d("GRIMOIRE", "💾 Horas guardadas: $newHours")
        }
    }
}