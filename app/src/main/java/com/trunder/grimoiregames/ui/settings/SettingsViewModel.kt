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
                // 1. Leemos el contenido del archivo
                val inputStream = context.contentResolver.openInputStream(uri)
                val reader = BufferedReader(InputStreamReader(inputStream))
                val jsonString = reader.readText()
                reader.close()

                // 2. Convertimos de JSON a Lista de Juegos
                val gson = Gson()
                val listType = object : TypeToken<List<Game>>() {}.type
                val games: List<Game> = gson.fromJson(jsonString, listType)

                // 3. Guardamos en la Base de Datos
                repository.restoreGames(games)

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "¡Juegos restaurados! (${games.size} juegos)", Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "El archivo está dañado o no es válido", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}