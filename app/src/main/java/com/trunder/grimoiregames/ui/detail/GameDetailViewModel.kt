package com.trunder.grimoiregames.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trunder.grimoiregames.data.entity.Game
import com.trunder.grimoiregames.data.repository.GameRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

@HiltViewModel
class GameDetailViewModel @Inject constructor(
    private val repository: GameRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val gameId: Int = checkNotNull(savedStateHandle["gameId"])

    // Observamos el juego.
    // Al usar 'onEach', cada vez que cargamos el juego, comprobamos si le faltan datos.
    val game: Flow<Game> = repository.getGameById(gameId).onEach { game ->
        // ESTRATEGIA DE CACHÉ:
        // Si el juego existe pero no tiene descripción, significa que es la versión "resumida".
        // ¡Lanzamos la petición para enriquecerlo!
        if (game.description.isNullOrBlank()) {
            fetchDetails(game)
        }
    }

    // --- LÓGICA DE TRADUCCIÓN ---

    // Estado que guarda el texto ya traducido (o null si aún no se ha traducido)
    private val _translatedDescription = MutableStateFlow<String?>(null)
    val translatedDescription = _translatedDescription.asStateFlow()

    // Estado para mostrar la ruedita de carga mientras traduce
    private val _isTranslating = MutableStateFlow(false)
    val isTranslating = _isTranslating.asStateFlow()

    fun translateDescription(text: String?) {
        // Si el texto es nulo o vacío, no hacemos nada
        if (text.isNullOrBlank()) return

        // 1. Detectar idioma del dispositivo ("es", "fr", "it"...)
        val deviceLangCode = Locale.getDefault().language

        // 2. Si el móvil ya está en inglés, asumimos que no hace falta traducir (IGDB suele venir en inglés)
        if (deviceLangCode == "en") {
            _translatedDescription.value = text
            return
        }

        // 3. Comprobar si Google ML Kit soporta tu idioma
        val targetLang = TranslateLanguage.fromLanguageTag(deviceLangCode)

        if (targetLang == null) {
            _translatedDescription.value = "Lo siento, tu idioma ($deviceLangCode) no está disponible para traducción offline."
            return
        }

        viewModelScope.launch {
            _isTranslating.value = true

            try {
                // 4. Configurar el traductor (De Inglés -> A tu idioma)
                val options = TranslatorOptions.Builder()
                    .setSourceLanguage(TranslateLanguage.ENGLISH)
                    .setTargetLanguage(targetLang)
                    .build()

                val translator = Translation.getClient(options)

                // 5. Condiciones de descarga (WiFi recomendado para no gastar datos)
                val conditions = DownloadConditions.Builder()
                    .requireWifi()
                    .build()

                // 6. Descargar modelo (si no lo tienes) y luego traducir
                translator.downloadModelIfNeeded(conditions)
                    .addOnSuccessListener {
                        // ¡Modelo listo! Traducimos...
                        translator.translate(text)
                            .addOnSuccessListener { translatedText ->
                                _translatedDescription.value = translatedText
                                _isTranslating.value = false
                                translator.close() // Cerramos para liberar memoria
                            }
                            .addOnFailureListener {
                                _translatedDescription.value = "Error al procesar la traducción."
                                _isTranslating.value = false
                                translator.close()
                            }
                    }
                    .addOnFailureListener { exception ->
                        // Falló la descarga (probablemente sin internet/wifi)
                        exception.printStackTrace()
                        _translatedDescription.value = "Error: Se requiere WiFi para descargar el idioma por primera vez."
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

    fun updateStatus(game: Game, newStatus: String) {
        viewModelScope.launch {
            repository.updateGame(game.copy(status = newStatus))
        }
    }

    fun updateRating(game: Game, newRating: Int) {
        viewModelScope.launch {
            repository.updateGame(game.copy(rating = newRating))
        }
    }

    fun updateHours(game: Game, newHours: Int) {
        viewModelScope.launch {
            repository.updateGame(game.copy(hoursPlayed = newHours))
        }
    }
}