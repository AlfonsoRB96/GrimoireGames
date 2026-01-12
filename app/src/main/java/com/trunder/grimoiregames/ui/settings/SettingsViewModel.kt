package com.trunder.grimoiregames.ui.settings

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.trunder.grimoiregames.data.entity.Game
import com.trunder.grimoiregames.data.repository.GameRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: GameRepository,
    @ApplicationContext private val context: Context // Necesario para leer/escribir archivos
) : ViewModel() {

    // --- EXPORTAR (CREAR BACKUP) ---
    fun backupData(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Conseguimos la lista de juegos
                val games = repository.getAllGamesSync()

                // 2. Convertimos a JSON (Texto)
                val gson = Gson()
                val jsonString = gson.toJson(games)

                // 3. Escribimos en el archivo seleccionado
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(jsonString.toByteArray())
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "¡Copia de seguridad guardada con éxito!", Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error al guardar: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // --- IMPORTAR (RESTAURAR BACKUP) ---
    fun restoreData(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Leer archivo
                val inputStream = context.contentResolver.openInputStream(uri)
                val reader = BufferedReader(InputStreamReader(inputStream))
                val jsonString = reader.readText()
                reader.close()

                val gson = Gson()
                val listType = object : TypeToken<List<Game>>() {}.type
                val backupGames: List<Game> = gson.fromJson(jsonString, listType)

                // 2. Obtener juegos actuales para no duplicar
                val currentGames = repository.getAllGamesSync()

                var addedCount = 0

                // 3. Lógica de Fusión Inteligente
                backupGames.forEach { backupGame ->
                    // Comprobamos si ya tenemos este juego (mismo ID de IGDB y misma Plataforma)
                    val exists = currentGames.any {
                        it.rawgId == backupGame.rawgId && it.platform == backupGame.platform
                    }

                    if (!exists) {
                        // ¡Truco! Ponemos id = 0 para que Room le asigne un ID nuevo y libre
                        // Así evitamos sobreescribir otros juegos.
                        val newGameEntry = backupGame.copy(id = 0)
                        repository.insertGame(newGameEntry) // Asegúrate de tener insertGame público o usa insertAll([newGameEntry])
                        addedCount++
                    }
                }

                withContext(Dispatchers.Main) {
                    if (addedCount > 0) {
                        Toast.makeText(context, "Se han añadido $addedCount juegos nuevos desde la copia.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Tu biblioteca ya tenía todos esos juegos.", Toast.LENGTH_SHORT).show()
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error al restaurar: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}