package com.trunder.grimoiregames.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import java.net.URLEncoder

object OpenCriticScraper {

    suspend fun getScore(gameTitle: String): Int? = withContext(Dispatchers.IO) {
        try {
            // 1. CORRECCIÓN CLAVE: OpenCritic odia los '+' en la URL. Necesita '%20'.
            val query = URLEncoder.encode(gameTitle, "UTF-8").replace("+", "%20")

            val searchUrl = "https://api.opencritic.com/api/game/search?criteria=$query"

            Log.d("ORACLE_NET", "📡 Hackeando OpenCritic API: $searchUrl")

            // 2. AÑADIMOS MÁS CABECERAS para parecer un navegador real y evitar el Error 400
            val response = Jsoup.connect(searchUrl)
                .ignoreContentType(true) // Aceptar JSON
                .ignoreHttpErrors(true)  // Para leer el cuerpo del error si falla
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept", "application/json, text/plain, */*")
                .header("Referer", "https://opencritic.com/") // ¡Importante!
                .header("Origin", "https://opencritic.com")
                .timeout(10000)
                .execute()

            // Comprobamos si sigue dando error 400 a pesar del arreglo
            if (response.statusCode() != 200) {
                Log.e("ORACLE_NET", "💥 OpenCritic rechazó la conexión: ${response.statusCode()} - ${response.statusMessage()}")
                return@withContext null
            }

            // 3. Parsear JSON
            val jsonBody = response.body()
            val results = JSONArray(jsonBody)

            if (results.length() > 0) {
                // Buscamos el mejor match.
                // A veces devuelve juegos con nombres parecidos.
                // Vamos a coger el primero, que suele ser el más relevante.
                val firstMatch = results.getJSONObject(0)
                val gameId = firstMatch.getInt("id")

                // Intento 1: ¿Viene la nota directa en la búsqueda?
                if (firstMatch.has("topCriticScore") && !firstMatch.isNull("topCriticScore")) {
                    val score = firstMatch.getInt("topCriticScore")
                    // -1 significa "sin nota"
                    if (score > 0) return@withContext score
                }

                // Intento 2: Si no viene nota, pedimos detalle con el ID
                return@withContext fetchGameDetails(gameId)

            } else {
                Log.d("ORACLE_NET", "⚠️ No se encontraron resultados en OpenCritic para: $gameTitle")
                return@withContext null
            }

        } catch (e: Exception) {
            Log.e("ORACLE_NET", "💥 Excepción en OpenCritic: ${e.message}")
            return@withContext null
        }
    }

    private fun fetchGameDetails(gameId: Int): Int? {
        try {
            val detailsUrl = "https://api.opencritic.com/api/game/$gameId"

            // También aplicamos las cabeceras aquí por si acaso
            val response = Jsoup.connect(detailsUrl)
                .ignoreContentType(true)
                .ignoreHttpErrors(true)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Referer", "https://opencritic.com/")
                .execute()

            if (response.statusCode() == 200) {
                val json = JSONObject(response.body())
                if (json.has("topCriticScore") && !json.isNull("topCriticScore")) {
                    val score = json.getInt("topCriticScore")
                    return if (score > 0) score else null
                }
            }
        } catch (e: Exception) {
            Log.e("ORACLE_NET", "💥 Error obteniendo detalles del ID $gameId: ${e.message}")
        }
        return null
    }
}