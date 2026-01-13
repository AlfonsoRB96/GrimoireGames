package com.trunder.grimoiregames.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.text.Normalizer
import java.util.Locale

object MetacriticScraper {

    // Estructura de datos simple para devolver el resultado
    data class ScrapeResult(
        val metaScore: Int?,
        val userScore: Int?
    )

    suspend fun scrapScores(gameTitle: String, platform: String): ScrapeResult = withContext(Dispatchers.IO) {
        try {
            // 1. Slugify del título
            val gameSlug = slugify(gameTitle)

            // 2. Construir URL Raíz
            val url = "https://www.metacritic.com/game/$gameSlug/"

            Log.d("SCRAPER", "Intentando scrapear: $url")

            // 3. Conectar y descargar HTML
            val doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept-Language", "en-US,en;q=0.9")
                .timeout(10000)
                .get()

            // 4. Buscar las notas (SELECTORES CORREGIDOS)
            // El truco es buscar la clase 'c-siteReviewScore_background', que es la cajita de color que tiene el número.

            // --- METASCORE (Prensa) ---
            // Buscamos dentro del bloque de score info -> el elemento con el background de color
            val metaScoreElement = doc.selectFirst(".c-productScoreInfo_scoreNumber .c-siteReviewScore_background")
                ?: doc.selectFirst("div[data-testid='critic-score-info'] .c-siteReviewScore_background")
                ?: doc.selectFirst(".metascore_w.larger") // Fallback web antigua

            // --- USER SCORE (Usuarios) ---
            // Buscamos dentro del bloque de usuario -> el elemento con el background de color
            val userScoreElement = doc.selectFirst(".c-siteReviewScore_userScore .c-siteReviewScore_background")
                ?: doc.selectFirst("div[data-testid='user-score-info'] .c-siteReviewScore_background")
                ?: doc.selectFirst(".metascore_w.user") // Fallback web antigua

            // Log de depuración para ver qué texto exacto estamos cogiendo ahora
            val metaTextRaw = metaScoreElement?.text()
            val userTextRaw = userScoreElement?.text()
            Log.d("SCRAPER", "🔎 Raw Text -> Meta: '$metaTextRaw' | User: '$userTextRaw'")

            // 5. Limpiar y convertir a Int
            val metaScore = parseScore(metaTextRaw)
            val userScore = parseScore(userTextRaw)

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
        val normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
        return normalized
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "") // Quita tildes
            .lowercase(Locale.ROOT)
            .replace(":", "")
            .replace("'", "")
            .replace(".", "")
            .replace("&", "and")
            .replace("+", "plus")
            .replace("/", "-")
            .trim()
            .replace("[^a-z0-9\\s-]".toRegex(), "")
            .replace("\\s+".toRegex(), "-")
    }

    private fun parseScore(text: String?): Int? {
        if (text.isNullOrBlank() || text.contains("tbd", true) || text.equals("tbd", true)) return null
        // Eliminamos cualquier caracter que no sea número o punto (por si acaso se cuela algo más)
        val cleanText = text.trim()
        return cleanText.toDoubleOrNull()?.toInt()
    }

    // (La función mapPlatformToMetacritic ya no se usa porque vamos a la URL raíz,
    // pero la puedes dejar o borrar, no molesta).
}