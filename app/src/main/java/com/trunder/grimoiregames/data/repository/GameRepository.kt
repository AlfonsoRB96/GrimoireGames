package com.trunder.grimoiregames.data.repository

import com.trunder.grimoiregames.data.dao.GameDao
import com.trunder.grimoiregames.data.entity.Game
import com.trunder.grimoiregames.data.remote.IgdbApi
import com.trunder.grimoiregames.data.remote.MetacriticScraper
import com.trunder.grimoiregames.data.remote.TwitchAuthApi
import com.trunder.grimoiregames.data.remote.dto.AgeRatingDto
import com.trunder.grimoiregames.data.remote.dto.IgdbGameDto
import com.trunder.grimoiregames.util.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.collections.isNullOrEmpty

class GameRepository @Inject constructor(
    private val gameDao: GameDao,
    private val igdbApi: IgdbApi,
    private val authApi: TwitchAuthApi
) {
    // Variable para guardar el token en memoria RAM mientras la app vive
    private var accessToken: String? = null

    val allGames: Flow<List<Game>> = gameDao.getAllGames()

    // --- GESTIÓN DE TOKEN ---
    private suspend fun getValidToken(): String {
        if (accessToken.isNullOrBlank()) {
            val response = authApi.getAccessToken(
                Constants.IGDB_CLIENT_ID,
                Constants.IGDB_CLIENT_SECRET
            )
            accessToken = response.accessToken
        }
        return "Bearer $accessToken"
    }

    // --- BÚSQUEDA EN IGDB ---
    // Usamos el lenguaje "Apicalypse" de IGDB
    suspend fun searchGames(query: String): List<IgdbGameDto> {
        val token = getValidToken()

        // 👇 CAMBIO: Quitamos el ".*" de age_ratings.
        // Solo pedimos los IDs para que la lista cargue rápido y no falle.
        val bodyString = "search \"$query\"; fields name, cover.url, first_release_date, total_rating, genres.name, platforms.name, summary, involved_companies.company.name, involved_companies.developer, involved_companies.publisher, age_ratings; where version_parent = null; limit 20;"

        val body = bodyString.toRequestBody("text/plain".toMediaTypeOrNull())

        return igdbApi.getGames(Constants.IGDB_CLIENT_ID, token, body)
    }

    // --- AÑADIR JUEGO (Mapeo completo con Doble Llamada) ---
    suspend fun addGame(igdbId: Int, selectedPlatform: String, userRegion: String) {
        android.util.Log.d("DEBUG_APP", "🎬 Iniciando addGame para ID: $igdbId")

        val token = getValidToken()

        // 1. PRIMERA LLAMADA: Pedimos el juego (age_ratings vendrá como lista de IDs)
        val bodyString = "fields name, cover.url, first_release_date, rating, aggregated_rating, genres.name, platforms.name, summary, involved_companies.company.name, involved_companies.developer, involved_companies.publisher, age_ratings; where id = $igdbId;"

        val body = bodyString.toRequestBody("text/plain".toMediaTypeOrNull())

        try {
            android.util.Log.d("DEBUG_APP", "🌍 Llamando a IGDB (Fase 1)...")

            val games = igdbApi.getGames(Constants.IGDB_CLIENT_ID, token, body)
            val details = games.firstOrNull()

            if (details == null) {
                android.util.Log.e("DEBUG_APP", "❌ ERROR: La lista de juegos vino vacía.")
                return
            }

            // --- FASE 2: EL FRANCOTIRADOR (Buscar detalles de edad) ---
            var finalAgeRatings: List<AgeRatingDto>? = null

            // Si el juego tiene IDs de edad...
            if (!details.ageRatings.isNullOrEmpty()) {
                val ids = details.ageRatings.joinToString(",") // Ej: "105532,112550"
                android.util.Log.d("DEBUG_APP", "🕵️ IDs de edad encontrados: $ids. Buscando detalles...")

                // Usamos el comodín (*) para que IGDB no nos oculte nada
                val ratingQuery = "fields *; where id = ($ids);"
                val ratingBody = ratingQuery.toRequestBody("text/plain".toMediaTypeOrNull())

                // Llamada al endpoint específico
                val fetchedRatings = igdbApi.getAgeRatingDetails(Constants.IGDB_CLIENT_ID, token, ratingBody)

                if (fetchedRatings.isNotEmpty()) {
                    android.util.Log.d("DEBUG_APP", "✅ Edad recuperada: ${fetchedRatings.size} registros.")
                    finalAgeRatings = fetchedRatings
                }
            }
            // ----------------------------------------------------------

            // Scraper y Mapeo normal
            val scrapeResult = MetacriticScraper.scrapScores(details.name, selectedPlatform)

            val hdImageUrl = details.cover?.url?.replace("t_thumb", "t_cover_big")?.let { "https:$it" }
            val releaseDateStr = details.firstReleaseDate?.let {
                java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(it * 1000))
            }

            android.util.Log.d("DEBUG_APP", "✅ Datos procesados. Preparando inserción en BD...")

            val newGame = Game(
                rawgId = details.id,
                title = details.name,
                platform = selectedPlatform,
                status = "Backlog",
                imageUrl = hdImageUrl,
                description = details.summary,
                igdbPress = details.aggregatedRating?.toInt(),
                igdbUser = details.rating?.toInt(),
                metacriticPress = scrapeResult.metaScore,
                metacriticUser = scrapeResult.userScore,
                opencriticPress = null,
                releaseDate = releaseDateStr,
                genre = details.genres?.firstOrNull()?.name ?: "Desconocido",
                developer = details.involvedCompanies?.find { it.developer }?.company?.name ?: "Desconocido",
                publisher = details.involvedCompanies?.find { it.publisher }?.company?.name ?: "Desconocido",
                region = userRegion,

                // 👇 AQUÍ PASAMOS LA LISTA OBTENIDA EN LA FASE 2
                ageRating = resolveAgeRating(finalAgeRatings, userRegion)
            )

            gameDao.insertGame(newGame)
            android.util.Log.d("DEBUG_APP", "🎉 ¡ÉXITO! Juego guardado en Room con edad: ${newGame.ageRating}")

        } catch (e: Exception) {
            android.util.Log.e("DEBUG_APP", "💀 ERROR CRÍTICO al guardar: ${e.message}")
            e.printStackTrace()
        }
    }

    suspend fun fetchAndSaveGameDetails(game: Game) {
        val token = getValidToken()

        // 1. PRIMERA LLAMADA: Pedimos el juego (ahora age_ratings se guardará como lista de IDs)
        // Nota: Quitamos "age_ratings.category" y dejamos solo "age_ratings"
        val gameQuery = "fields name,cover.url,first_release_date,rating,aggregated_rating,genres.name,platforms.name,summary,involved_companies.company.name,involved_companies.developer,involved_companies.publisher,age_ratings; where id = ${game.rawgId};"

        val gameBody = gameQuery.toRequestBody("text/plain".toMediaTypeOrNull())

        try {
            // Esto ahora funcionará porque IgdbGameDto espera List<Long>
            val games = igdbApi.getGames(Constants.IGDB_CLIENT_ID, token, gameBody)
            val details = games.firstOrNull() ?: return

            // --- FASE 2: EL FRANCOTIRADOR ---
            var finalAgeRatings: List<AgeRatingDto>? = null

            // Si recibimos IDs de edad (la lista de Longs no está vacía)
            if (!details.ageRatings.isNullOrEmpty()) {
                val ids = details.ageRatings.joinToString(",") // "205404,166648"

                android.util.Log.d("GRIMOIRE", "🚀 IDs recibidos: $ids. Buscando detalles...")

                // Hacemos la llamada al endpoint específico de edades
                val ratingQuery = "fields *; where id = ($ids);"
                val ratingBody = ratingQuery.toRequestBody("text/plain".toMediaTypeOrNull())

                // Esta llamada devuelve List<AgeRatingDto>, así que usamos esa clase
                val fetchedRatings = igdbApi.getAgeRatingDetails(Constants.IGDB_CLIENT_ID, token, ratingBody)

                if (fetchedRatings.isNotEmpty()) {
                    android.util.Log.d("GRIMOIRE", "✅ Detalles recuperados: ${fetchedRatings.size} ratings.")
                    finalAgeRatings = fetchedRatings
                }
            }
            // -------------------------------

            // Mapeo y Guardado
            val scrapeResult = MetacriticScraper.scrapScores(details.name, game.platform)
            val hdImageUrl = details.cover?.url?.replace("t_thumb", "t_cover_big")?.let { "https:$it" }
            val releaseDateStr = details.firstReleaseDate?.let {
                java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(it * 1000))
            }

            val updatedGame = game.copy(
                description = details.summary,
                igdbPress = details.aggregatedRating?.toInt(),
                igdbUser = details.rating?.toInt(),
                metacriticPress = scrapeResult.metaScore,
                metacriticUser = scrapeResult.userScore,
                releaseDate = releaseDateStr,
                genre = details.genres?.firstOrNull()?.name ?: "Desconocido",
                developer = details.involvedCompanies?.find { it.developer }?.company?.name ?: "Desconocido",
                publisher = details.involvedCompanies?.find { it.publisher }?.company?.name ?: "Desconocido",

                // 👇 Pasamos la lista de objetos que conseguimos en la fase 2
                ageRating = resolveAgeRating(finalAgeRatings, game.region),

                imageUrl = hdImageUrl ?: game.imageUrl
            )

            gameDao.updateGame(updatedGame)

        } catch (e: Exception) {
            android.util.Log.e("GRIMOIRE", "Error: ${e.message}")
            e.printStackTrace()
        }
    }

    suspend fun deleteGame(game: Game) = gameDao.deleteGame(game)
    suspend fun updateGame(game: Game) = gameDao.updateGame(game)
    fun getGameById(id: Int) = gameDao.getGameById(id)

    // --- MAPEO INTELIGENTE DE EDAD ---
    // --- MAPEO DE EDAD BASADO EN REGIÓN (GLOBAL) ---
    private fun resolveAgeRating(ratings: List<AgeRatingDto>?, targetRegion: String): String? {
        if (ratings.isNullOrEmpty()) return null

        fun getInfo(dto: AgeRatingDto): Pair<Int, Int> {
            val cat = dto.category ?: dto.organization ?: -1
            val rat = dto.rating ?: dto.ratingCategory ?: -1
            return Pair(cat, rat)
        }

        // DICCIONARIO DE ORGANIZACIONES IGDB
        // 1=ESRB, 2=PEGI, 3=CERO, 4=USK, 5=GRAC, 6=ClassInd, 7=ACB
        val targetOrgId = when (targetRegion) {
            "NTSC-U"  -> 1 // USA
            "PAL EU"  -> 2 // Europa (Default)
            "PAL"     -> 2 // Compatibilidad hacia atrás
            "NTSC-J"  -> 3 // Japón
            "PAL DE"  -> 4 // Alemania (USK)
            "NTSC-K"  -> 5 // Corea (GRAC)
            "NTSC-BR" -> 6 // Brasil (ClassInd)
            "PAL AU"  -> 7 // Australia (ACB)
            else      -> 2 // Por defecto PEGI
        }

        // 1. Buscamos la clasificación exacta para esa región
        val targetRating = ratings.find { (it.category ?: it.organization) == targetOrgId }

        if (targetRating != null) {
            return mapRatingToString(targetRating)
        }

        // 2. Fallback inteligente:
        // Si pusiste "PAL DE" (Alemania) pero el juego no tiene USK, intenta devolver PEGI (Europa).
        if (targetOrgId == 4) { // Si buscábamos USK y falló...
            ratings.find { (it.category ?: it.organization) == 2 }?.let { return mapRatingToString(it) }
        }

        // 3. Último recurso: Devuelve lo primero que pille (mejor que null)
        ratings.firstOrNull()?.let { return mapRatingToString(it) }

        return null
    }

    private fun mapRatingToString(dto: AgeRatingDto): String {
        val cat = dto.category ?: dto.organization ?: -1
        val rat = dto.rating ?: dto.ratingCategory ?: -1

        return when (cat) {
            1 -> { // ESRB
                when (rat) {
                    6 -> "ESRB RP"; 7 -> "ESRB EC"; 8 -> "ESRB E"; 9 -> "ESRB 10+"; 10 -> "ESRB T"; 11 -> "ESRB M"; 12 -> "ESRB AO"
                    else -> "ESRB ($rat)"
                }
            }
            2 -> { // PEGI
                when (rat) {
                    1 -> "PEGI 3"; 2 -> "PEGI 7"; 3 -> "PEGI 12"; 4 -> "PEGI 16"; 5 -> "PEGI 18"
                    else -> "PEGI ($rat)"
                }
            }
            3 -> { // CERO (Japón)
                when (rat) {
                    13 -> "CERO A"; 14 -> "CERO B"; 15 -> "CERO C"; 16 -> "CERO D"; 17 -> "CERO Z"
                    else -> "CERO ($rat)"
                }
            }
            4 -> { // USK (Alemania)
                when (rat) {
                    18 -> "USK 0"; 19 -> "USK 6"; 20 -> "USK 12"; 21 -> "USK 16"; 22 -> "USK 18"
                    else -> "USK ($rat)"
                }
            }
            5 -> { // GRAC (Corea)
                when (rat) {
                    23 -> "GRAC All"; 24 -> "GRAC 12"; 25 -> "GRAC 15"; 26 -> "GRAC 18"; 39 -> "GRAC Test"
                    else -> "GRAC ($rat)"
                }
            }
            6 -> { // ClassInd (Brasil)
                when (rat) {
                    27 -> "L (All)"; 28 -> "10"; 29 -> "12"; 30 -> "14"; 31 -> "16"; 32 -> "18"
                    else -> "ClassInd ($rat)"
                }
            }
            7 -> { // ACB (Australia)
                when (rat) {
                    33 -> "ACB G"; 34 -> "ACB PG"; 35 -> "ACB M"; 36 -> "ACB MA15+"; 37 -> "ACB R18+"; 38 -> "ACB RC"
                    else -> "ACB ($rat)"
                }
            }
            else -> "UNK ($rat)"
        }
    }

    // Funciones para Backup/Restore
    suspend fun getAllGamesSync(): List<Game> = gameDao.getAllGamesList()

    suspend fun restoreGames(games: List<Game>) {
        gameDao.insertAll(games)
    }

    fun updateAllGames(): Flow<Int> = flow {
        // 1. Cogemos todos los juegos
        val allGames = gameDao.getAllGamesList()
        val totalGames = allGames.size

        if (totalGames == 0) {
            emit(100)
            return@flow
        }

        var processedCount = 0
        emit(0) // Empezamos en 0%

        // 2. Iteramos uno a uno
        for (game in allGames) {
            try {
                // Actualizamos el juego (IGDB + Scraper)
                fetchAndSaveGameDetails(game)

                // Pausa técnica para no saturar la API
                kotlinx.coroutines.delay(200)

            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 3. Calculamos y emitimos el nuevo porcentaje
            processedCount++
            val percentage = ((processedCount.toFloat() / totalGames) * 100).toInt()
            emit(percentage)
        }
    }

    // 👇 ESTA ES LA FUNCIÓN QUE TE FALTA
    // Guarda un juego directamente en la BD sin buscarlo en internet.
    // Necesaria para la restauración de copias de seguridad (Backup).
    suspend fun insertGame(game: Game) {
        gameDao.insertGame(game)
    }
}