package com.trunder.grimoiregames.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.net.URLEncoder
import java.text.Normalizer
import kotlin.math.abs

object MetacriticScraper {

    data class ScrapeResult(
        val metaScore: Int?,
        val userScore: Int?
    )

    suspend fun scrapScores(gameTitle: String, platform: String): ScrapeResult = withContext(Dispatchers.IO) {
        try {
            // 1. 🕵️‍♀️ BÚSQUEDA
            val searchUrl = buildSearchUrl(gameTitle)
            Log.d("ORACLE_META", "📡 Buscando en Metacritic: $searchUrl")

            val searchDoc = Jsoup.connect(searchUrl)
                .userAgent("Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                .header("Accept-Language", "en-US,en;q=0.9")
                .timeout(10000)
                .get()

            // 2. 🎯 SELECCIÓN INTELIGENTE (SNIPER MODE 2.0)
            val targetUrl = findBestMatchInSearchResults(searchDoc, gameTitle)

            if (targetUrl == null) {
                Log.w("ORACLE_META", "💨 No se encontró ningún candidato viable en la búsqueda.")
                return@withContext ScrapeResult(null, null)
            }

            Log.d("ORACLE_META", "✅ Objetivo fijado: $targetUrl")

            // 3. ⛏️ EXTRACCIÓN
            return@withContext fetchScoresFromPage(targetUrl)

        } catch (e: Exception) {
            Log.e("ORACLE_META", "💥 Misión fallida: ${e.message}")
            return@withContext ScrapeResult(null, null)
        }
    }

    // --- CEREBRO DE ORACLE ---

    private fun buildSearchUrl(query: String): String {
        val cleanQuery = sanitizeQuery(query)
        val encodedQuery = URLEncoder.encode(cleanQuery, "UTF-8")
        return "https://www.metacritic.com/search/$encodedQuery/?page=1&category=13"
    }

    private fun findBestMatchInSearchResults(doc: org.jsoup.nodes.Document, originalTitle: String): String? {
        val candidatesElements = doc.select("a[href^='/game/']")
        val cleanOriginalTitle = sanitizeQuery(originalTitle).lowercase()

        data class Candidate(val url: String, val title: String, val diffScore: Int)
        val matches = mutableListOf<Candidate>()

        Log.d("ORACLE_DEBUG", "🔎 Analizando ${candidatesElements.size} candidatos...")

        for (link in candidatesElements) {
            val titleText = link.text().trim()
            if (titleText.isBlank()) continue

            // 👇 Aquí usamos la nueva sanitización que entiende "plus"
            val cleanCandidateTitle = sanitizeQuery(titleText).lowercase()

            // 1. FILTRO DE CONTENIDO
            if (cleanCandidateTitle.contains(cleanOriginalTitle) || cleanOriginalTitle.contains(cleanCandidateTitle)) {

                // 2. PUNTUACIÓN DE DIFERENCIA
                // Ahora "Rain Code+" (convertido a "plus") y "Rain Code Plus" tendrán diff 0.
                val diff = abs(cleanOriginalTitle.length - cleanCandidateTitle.length)

                matches.add(Candidate("https://www.metacritic.com${link.attr("href")}", titleText, diff))
                Log.d("ORACLE_DEBUG", "   ➡ Candidato: '$titleText' (Limpio: '$cleanCandidateTitle') | Diff: $diff")
            }
        }

        // 3. SELECCIÓN DEL MEJOR
        val bestMatch = matches.minByOrNull { it.diffScore }

        if (bestMatch != null) {
            Log.d("ORACLE_DEBUG", "🎯 GANADOR: '${bestMatch.title}' (Diff: ${bestMatch.diffScore})")
            return bestMatch.url
        }

        return null
    }

    private fun fetchScoresFromPage(url: String): ScrapeResult {
        val doc = Jsoup.connect(url)
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .timeout(8000)
            .get()

        val metaScoreElement = doc.selectFirst(".c-productScoreInfo_scoreNumber .c-siteReviewScore_background")
            ?: doc.selectFirst("div[data-testid='critic-score-info'] .c-siteReviewScore_background")
            ?: doc.selectFirst(".metascore_w.larger")

        val userScoreElement = doc.selectFirst(".c-siteReviewScore_userScore .c-siteReviewScore_background")
            ?: doc.selectFirst("div[data-testid='user-score-info'] .c-siteReviewScore_background")
            ?: doc.selectFirst(".metascore_w.user")

        val metaScoreRaw = parseScore(metaScoreElement?.text())
        val userScoreRaw = parseScore(userScoreElement?.text())

        val metaScore = metaScoreRaw?.toInt()
        val userScore = userScoreRaw?.let {
            if (it <= 10.0) (it * 10).toInt() else it.toInt()
        }

        return ScrapeResult(metaScore, userScore)
    }

    private fun sanitizeQuery(input: String): String {
        val normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
        return normalized
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .replace(":", "")
            .replace(" - ", " ")
            .replace("-", " ")
            // 👇 ¡EL CAMBIO CLAVE! 👇
            .replace("+", " plus ") // Traducimos el símbolo a palabra
            .replace("&", " and ")  // Y el ampersand también
            .trim()
            .replace("\\s+".toRegex(), " ") // Evitamos dobles espacios
    }

    private fun parseScore(text: String?): Double? {
        if (text.isNullOrBlank() || text.contains("tbd", true)) return null
        val cleanText = text.trim()
        return cleanText.toDoubleOrNull()
    }
}