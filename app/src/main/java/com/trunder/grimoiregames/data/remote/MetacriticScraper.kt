package com.trunder.grimoiregames.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

object MetacriticScraper {

    // Estructura de datos simple para devolver el resultado
    data class ScrapeResult(
        val metaScore: Int?,
        val userScore: Int?
    )

    suspend fun scrapScores(gameTitle: String, platform: String): ScrapeResult = withContext(Dispatchers.IO) {
        try {
            // 1. "Slugificar" el título y plataforma (Ej: "God of War: Ragnarök" -> "god-of-war-ragnarok")
            val gameSlug = slugify(gameTitle)
            val platformSlug = mapPlatformToMetacritic(platform)

            if (platformSlug == null) {
                Log.e("SCRAPER", "Plataforma no soportada para scraping: $platform")
                return@withContext ScrapeResult(null, null)
            }

            // 2. Construir URL (Ej: metacritic.com/game/playstation-5/god-of-war-ragnarok)
            // NOTA: Metacritic cambió su estructura recientemente.
            val url = "https://www.metacritic.com/game/$gameSlug/"
            // A veces requiere platform en la URL: "https://www.metacritic.com/game/$platformSlug/$gameSlug"
            // Vamos a intentar la estructura más común actual:
            val finalUrl = "https://www.metacritic.com/game/$gameSlug/?platform=$platformSlug"

            Log.d("SCRAPER", "Intentando scrapear: $finalUrl")

            // 3. Conectar y descargar HTML (Nos hacemos pasar por un navegador real)
            val doc = Jsoup.connect(finalUrl)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .timeout(10000) // 10 segundos timeout
                .get()

            // 4. Buscar las notas en el HTML (ESTO ES LO FRÁGIL, DEPENDE DEL DISEÑO DE ELLOS)

            // Selector para MetaScore (Prensa)
            // Buscamos un div que contenga la clase de score. Metacritic usa clases raras ahora (c-productScoreInfo...)
            // Truco: Buscamos por el texto "Metascore" cercano o clases genéricas
            val metaScoreElement = doc.selectFirst("div[title^='Metascore'] span")
                ?: doc.selectFirst(".c-productScoreInfo_scoreNumber_first span") // Fallback diseño nuevo

            // Selector para UserScore (Usuarios)
            val userScoreElement = doc.selectFirst("div[title^='User score'] span")
                ?: doc.selectFirst(".c-productScoreInfo_scoreNumber_second span") // Fallback diseño nuevo

            // 5. Limpiar y convertir a Int
            val metaScore = parseScore(metaScoreElement?.text())
            val userScore = parseScore(userScoreElement?.text()) // Nota: User score suele ser sobre 10 (ej: 8.5)

            // Convertimos nota usuario (8.5) a base 100 (85)
            val finalUserScore = userScore?.let { if (it <= 10) it * 10 else it }

            Log.d("SCRAPER", "✅ ÉXITO: Prensa=$metaScore, User=$finalUserScore")

            ScrapeResult(metaScore, finalUserScore)

        } catch (e: Exception) {
            Log.e("SCRAPER", "❌ ERROR scrapeando: ${e.message}")
            ScrapeResult(null, null)
        }
    }

    // --- HELPERS ---

    private fun slugify(input: String): String {
        return input.lowercase()
            .replace(":", "")
            .replace("'", "")
            .replace(".", "")
            .replace("&", "and")
            .replace(" ", "-")
            .replace(Regex("[^a-z0-9-]"), "") // Quitar caracteres raros
    }

    private fun parseScore(text: String?): Int? {
        if (text.isNullOrBlank() || text.contains("tbd", true)) return null
        return text.trim().toDoubleOrNull()?.toInt()
    }

    private fun mapPlatformToMetacritic(platform: String): String? {
        // Mapeo manual de tus nombres a los de Metacritic
        return when {
            platform.contains("PlayStation 5", true) -> "playstation-5"
            platform.contains("PlayStation 4", true) -> "playstation-4"
            platform.contains("Switch", true) -> "nintendo-switch"
            platform.contains("Xbox Series", true) -> "xbox-series-x"
            platform.contains("PC", true) -> "pc"
            else -> null // Si no lo conocemos, devolvemos null y no scrapeamos
        }
    }
}