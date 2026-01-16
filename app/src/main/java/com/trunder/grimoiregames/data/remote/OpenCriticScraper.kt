package com.trunder.grimoiregames.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import java.net.URLEncoder
import java.text.Normalizer
import kotlin.math.abs
import kotlin.math.roundToInt

object OpenCriticScraper {

    suspend fun getScore(gameTitle: String): Int? = withContext(Dispatchers.IO) {
        try {
            // 1. 🧼 LIMPIEZA FORENSE
            // Quitamos dos puntos, convertimos + a plus, etc.
            val cleanTitle = sanitizeQuery(gameTitle)
            val query = URLEncoder.encode(cleanTitle, "UTF-8").replace("+", "%20")
            val searchUrl = "https://api.opencritic.com/api/game/search?criteria=$query"

            Log.d("ORACLE_NET", "📡 Hackeando OpenCritic con query limpia: '$cleanTitle' -> $searchUrl")

            // 2. 🕵️‍♀️ INFILTRACIÓN
            val response = Jsoup.connect(searchUrl)
                .ignoreContentType(true)
                .ignoreHttpErrors(true)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept", "application/json, text/plain, */*")
                .header("Referer", "https://opencritic.com/")
                .timeout(10000)
                .execute()

            if (response.statusCode() != 200) {
                Log.e("ORACLE_NET", "💥 Error de conexión: ${response.statusCode()}")
                return@withContext null
            }

            val results = JSONArray(response.body())
            // Revisamos hasta 10 candidatos para no perdernos nada
            val maxAttempts = minOf(results.length(), 10)

            // 3. 🎯 ANÁLISIS DE CANDIDATOS (SNIPER MODE)
            data class Candidate(val id: Int, val name: String, val diffScore: Int, val score: Int?)
            val matches = mutableListOf<Candidate>()
            val cleanOriginalTitle = cleanTitle.lowercase() // Usamos la versión limpia para comparar

            Log.d("ORACLE_DEBUG", "🔎 Analizando ${results.length()} resultados para '$gameTitle'...")

            for (i in 0 until maxAttempts) {
                val item = results.getJSONObject(i)
                val id = item.getInt("id")
                val name = item.getString("name")

                // Filtro anti-spam
                if (isSpamResult(name, gameTitle)) continue

                // Limpiamos el nombre del candidato para comparar en igualdad de condiciones
                val cleanCandidateName = sanitizeQuery(name).lowercase()

                // CONDICIÓN DE MATCH: ¿Se contienen mutuamente?
                if (cleanCandidateName.contains(cleanOriginalTitle) || cleanOriginalTitle.contains(cleanCandidateName)) {

                    // CALCULAR PUNTERÍA (Diferencia de longitud)
                    // "Rain Code" (Original) vs "Rain Code" (Candidato) -> Diff 0 (¡PERFECTO!)
                    // "Rain Code Plus" vs "Rain Code" -> Diff 4 (Peor)
                    val diff = abs(cleanOriginalTitle.length - cleanCandidateName.length)

                    // Intentamos pescar la nota si ya viene en el JSON
                    var preScore: Int? = null
                    if (item.has("topCriticScore") && !item.isNull("topCriticScore")) {
                        val raw = item.getDouble("topCriticScore")
                        val rounded = raw.roundToInt()
                        // Solo guardamos si es válida (>0). Si es -1, guardamos null.
                        if (rounded > 0) preScore = rounded
                    }

                    matches.add(Candidate(id, name, diff, preScore))
                    Log.d("ORACLE_DEBUG", "   ➡ Candidato: '$name' | Diff: $diff | NotaPrevia: $preScore")
                }
            }

            // 4. 🏆 SELECCIÓN DEL BLANCO PERFECTO
            // Ordenamos por menor diferencia (el más parecido)
            val bestMatch = matches.minByOrNull { it.diffScore }

            if (bestMatch != null) {
                Log.d("ORACLE_NET", "🎯 Objetivo fijado: '${bestMatch.name}' (Diff: ${bestMatch.diffScore})")

                // A. Si ya tenemos nota válida del buscador, ¡listo!
                if (bestMatch.score != null) {
                    return@withContext bestMatch.score
                }

                // B. Si no (era null o -1), entramos al detalle a investigar
                Log.d("ORACLE_NET", "⚡ Sin nota en lista. Infiltrándose en ficha detallada (ID: ${bestMatch.id})...")
                return@withContext fetchGameDetails(bestMatch.id)
            }

            Log.w("ORACLE_NET", "💨 No se encontró ningún candidato válido.")
            return@withContext null

        } catch (e: Exception) {
            Log.e("ORACLE_NET", "💥 Excepción crítica: ${e.message}")
            return@withContext null
        }
    }

    // --- HERRAMIENTAS DE HACKEO ---

    private fun sanitizeQuery(input: String): String {
        val normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
        return normalized
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "") // Fuera tildes
            .replace(":", "")       // Dos puntos fuera
            .replace("'", "")
            .replace("-", " ")
            .replace("+", " plus ") // Traducimos + a plus para que coincida con texto
            .replace("&", " and ")
            .trim()
            .replace("\\s+".toRegex(), " ") // Espacios dobles a uno
    }

    private fun isSpamResult(resultName: String, originalQuery: String): Boolean {
        val lowerName = resultName.lowercase()
        val lowerQuery = originalQuery.lowercase()
        val blackList = listOf("soundtrack", "ost ", "artbook", "guide", "walkthrough", "dlc", "upgrade", "season pass")

        return blackList.any { badWord ->
            lowerName.contains(badWord) && !lowerQuery.contains(badWord)
        }
    }

    private fun fetchGameDetails(gameId: Int): Int? {
        try {
            val detailsUrl = "https://api.opencritic.com/api/game/$gameId"
            val response = Jsoup.connect(detailsUrl)
                .ignoreContentType(true)
                .ignoreHttpErrors(true)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .execute()

            if (response.statusCode() == 200) {
                val json = JSONObject(response.body())
                if (json.has("topCriticScore") && !json.isNull("topCriticScore")) {
                    // 👇 Recuperamos con precisión decimal y redondeamos
                    val rawScore = json.getDouble("topCriticScore")
                    val score = rawScore.roundToInt()
                    return if (score > 0) score else null
                }
            }
        } catch (e: Exception) {
            Log.e("ORACLE_DEBUG", "❌ Error al obtener detalles: ${e.message}")
        }
        return null
    }
}