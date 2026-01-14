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
            // 1. URL Encoding correcto (%20)
            val query = URLEncoder.encode(gameTitle, "UTF-8").replace("+", "%20")
            val searchUrl = "https://api.opencritic.com/api/game/search?criteria=$query"

            Log.d("ORACLE_NET", "📡 Hackeando OpenCritic API: $searchUrl")

            // 2. Petición con cabeceras de navegador
            val response = Jsoup.connect(searchUrl)
                .ignoreContentType(true)
                .ignoreHttpErrors(true)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept", "application/json, text/plain, */*")
                .header("Referer", "https://opencritic.com/")
                .timeout(10000)
                .execute()

            if (response.statusCode() != 200) {
                Log.e("ORACLE_NET", "💥 OpenCritic rechazó la conexión: ${response.statusCode()}")
                return@withContext null
            }

            val results = JSONArray(response.body())

            // 3. LÓGICA DETECTIVE: No nos fiamos del primero
            // Revisamos hasta los 5 primeros resultados buscando una nota válida.
            val maxAttempts = minOf(results.length(), 5)

            for (i in 0 until maxAttempts) {
                val candidate = results.getJSONObject(i)
                val id = candidate.getInt("id")
                val name = candidate.getString("name")

                // Filtro anti-basura: Evitamos DLCs obvios si el usuario no buscó explícitamente "DLC"
                if (isSpamResult(name, gameTitle)) {
                    Log.d("ORACLE_NET", "🗑️ Ignorando resultado basura: $name")
                    continue
                }

                // A. Intentamos sacar la nota directa de la búsqueda
                if (candidate.has("topCriticScore") && !candidate.isNull("topCriticScore")) {
                    val score = candidate.getInt("topCriticScore")
                    if (score > 0) {
                        Log.d("ORACLE_NET", "✅ ¡Encontrado en posición $i! ($name) -> Nota: $score")
                        return@withContext score
                    }
                }

                // B. Si parece el juego correcto pero no trae nota, entramos al detalle
                // (Solo si el nombre es muy parecido, para no perder tiempo entrando en 5 fichas)
                if (namesAreSimilar(gameTitle, name)) {
                    val detailsScore = fetchGameDetails(id)
                    if (detailsScore != null) {
                        Log.d("ORACLE_NET", "✅ ¡Encontrado en detalle (Pos $i)! ($name) -> Nota: $detailsScore")
                        return@withContext detailsScore
                    }
                }
            }

            Log.d("ORACLE_NET", "⚠️ Se revisaron $maxAttempts resultados y ninguno tenía nota válida.")
            return@withContext null

        } catch (e: Exception) {
            Log.e("ORACLE_NET", "💥 Excepción en OpenCritic: ${e.message}")
            return@withContext null
        }
    }

    // Funciones auxiliares para ser más listos

    private fun isSpamResult(resultName: String, originalQuery: String): Boolean {
        val lowerName = resultName.lowercase()
        val lowerQuery = originalQuery.lowercase()

        // Palabras prohibidas (salvo que la búsqueda original las incluya)
        val blackList = listOf("soundtrack", "ost ", "artbook", "guide", "walkthrough", "dlc", "upgrade", "season pass")

        return blackList.any { badWord ->
            lowerName.contains(badWord) && !lowerQuery.contains(badWord)
        }
    }

    private fun namesAreSimilar(query: String, result: String): Boolean {
        // Lógica simple: Si uno contiene al otro, nos vale para investigar el detalle.
        return result.contains(query, ignoreCase = true) || query.contains(result, ignoreCase = true)
    }

    private fun fetchGameDetails(gameId: Int): Int? {
        try {
            val detailsUrl = "https://api.opencritic.com/api/game/$gameId"
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
            // Silencioso
        }
        return null
    }
}