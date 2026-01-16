package com.trunder.grimoiregames.data.repository

import com.trunder.grimoiregames.data.dao.GameDao
import com.trunder.grimoiregames.data.entity.DlcItem
import com.trunder.grimoiregames.data.entity.Game
import com.trunder.grimoiregames.data.remote.IgdbApi
import com.trunder.grimoiregames.data.remote.MetacriticScraper
import com.trunder.grimoiregames.data.remote.OpenCriticScraper
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

    // --- BÚSQUEDA EN IGDB (ESTRATEGIA EXCLUSIÓN) ---
    // Usamos lógica inversa: BLOQUEAMOS lo que NO queremos.
    suspend fun searchGames(query: String): List<IgdbGameDto> {
        val token = getValidToken()
        val cleanQuery = query.trim() // 👈 Limpiamos espacios fantasma

        // Excluimos: 1 (DLC), 5 (Mod), 6 (Episode), 7 (Season)
        val bodyString = "search \"$cleanQuery\"; fields name, cover.url, first_release_date, total_rating, genres.name, platforms.name, summary, involved_companies.company.name, involved_companies.developer, involved_companies.publisher, age_ratings, game_type; where game_type = (0, 3, 4, 8, 9, 10, 11); limit 20;"
        val body = bodyString.toRequestBody("text/plain".toMediaTypeOrNull())

        return igdbApi.getGames(Constants.IGDB_CLIENT_ID, token, body)
    }

    // --- AÑADIR JUEGO (Mapeo completo con Doble Llamada) ---
    suspend fun addGame(igdbId: Int, selectedPlatform: String, userRegion: String, onProgress: (Float, String) -> Unit) {
        android.util.Log.d("DEBUG_APP", "🎬 Iniciando addGame para ID: $igdbId")

        onProgress(0.1f, "Conectando con IGDB...")
        val token = getValidToken()

        // 1. PRIMERA LLAMADA: Pedimos el juego + DLCs
        val bodyString = """
            fields name, cover.url, first_release_date, rating, aggregated_rating, genres.name, platforms.name, summary, involved_companies.company.name, involved_companies.developer, involved_companies.publisher, age_ratings, category,
            dlcs.name, dlcs.cover.url, dlcs.first_release_date; 
            where id = $igdbId;
        """.trimIndent()

        val body = bodyString.toRequestBody("text/plain".toMediaTypeOrNull())

        try {
            onProgress(0.2f, "Descargando datos del juego...") // 20%
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
                onProgress(0.3f, "Verificando clasificación de edad...") // 30%
                val ids = details.ageRatings.joinToString(",") // Ej: "105532,112550"
                android.util.Log.d("DEBUG_APP", "🕵️ IDs de edad encontrados: $ids. Buscando detalles...")

                val ratingQuery = "fields *; where id = ($ids);"
                val ratingBody = ratingQuery.toRequestBody("text/plain".toMediaTypeOrNull())

                val fetchedRatings = igdbApi.getAgeRatingDetails(Constants.IGDB_CLIENT_ID, token, ratingBody)

                if (fetchedRatings.isNotEmpty()) {
                    android.util.Log.d("DEBUG_APP", "✅ Edad recuperada: ${fetchedRatings.size} registros.")
                    finalAgeRatings = fetchedRatings
                }
            }
            // ----------------------------------------------------------

            onProgress(0.5f, "Hackeando base de datos de Metacritic...") // 50%
            val scrapeResult = MetacriticScraper.scrapScores(details.name, selectedPlatform)

            onProgress(0.7f, "Infiltrándose en OpenCritic...") // 70%
            val openCriticScore = OpenCriticScraper.getScore(details.name)

            onProgress(0.9f, "Escribiendo en el Grimorio...") // 90%
            val hdImageUrl = details.cover?.url?.replace("t_thumb", "t_cover_big")?.let { "https:$it" }
            val releaseDateStr = details.firstReleaseDate?.let {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it * 1000))
            }

            // Mapeo de DLCs
            val dlcList = details.dlcs?.map { dlcDto ->
                DlcItem(
                    name = dlcDto.name,
                    coverUrl = dlcDto.cover?.url?.replace("t_thumb", "t_cover_big")?.let { "https:$it" },
                    releaseDate = dlcDto.firstReleaseDate?.let {
                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it * 1000))
                    }
                )
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
                opencriticPress = openCriticScore,
                opencriticUser = null,
                releaseDate = releaseDateStr,
                genre = details.genres?.firstOrNull()?.name ?: "Desconocido",
                developer = details.involvedCompanies?.find { it.developer }?.company?.name ?: "Desconocido",
                publisher = details.involvedCompanies?.find { it.publisher }?.company?.name ?: "Desconocido",
                region = userRegion,
                ageRating = resolveAgeRating(finalAgeRatings, userRegion),

                // 👇 Guardamos la lista de DLCs
                dlcs = dlcList
            )

            gameDao.insertGame(newGame)
            onProgress(1.0f, "¡Misión Cumplida!") // 100%
            android.util.Log.d("DEBUG_APP", "🎉 ¡ÉXITO! Juego guardado en Room con edad: ${newGame.ageRating}")

        } catch (e: Exception) {
            android.util.Log.e("DEBUG_APP", "💀 ERROR CRÍTICO al guardar: ${e.message}")
            e.printStackTrace()
        }
    }

    suspend fun fetchAndSaveGameDetails(game: Game) {
        val token = getValidToken()

        val gameQuery = """
            fields name, cover.url, first_release_date, rating, aggregated_rating, genres.name, platforms.name, summary, involved_companies.company.name, involved_companies.developer, involved_companies.publisher, age_ratings, 
            dlcs.name, dlcs.cover.url, dlcs.first_release_date; 
            where id = ${game.rawgId};
        """.trimIndent()

        val gameBody = gameQuery.toRequestBody("text/plain".toMediaTypeOrNull())

        try {
            val games = igdbApi.getGames(Constants.IGDB_CLIENT_ID, token, gameBody)
            val details = games.firstOrNull() ?: return

            // --- FASE 2: EL FRANCOTIRADOR ---
            var finalAgeRatings: List<AgeRatingDto>? = null
            if (!details.ageRatings.isNullOrEmpty()) {
                val ids = details.ageRatings.joinToString(",")
                val ratingQuery = "fields *; where id = ($ids);"
                val ratingBody = ratingQuery.toRequestBody("text/plain".toMediaTypeOrNull())
                val fetchedRatings = igdbApi.getAgeRatingDetails(Constants.IGDB_CLIENT_ID, token, ratingBody)
                if (fetchedRatings.isNotEmpty()) {
                    finalAgeRatings = fetchedRatings
                }
            }

            val scrapeResult = MetacriticScraper.scrapScores(details.name, game.platform)
            val openCriticScore = OpenCriticScraper.getScore(details.name)

            val hdImageUrl = details.cover?.url?.replace("t_thumb", "t_cover_big")?.let { "https:$it" }
            val releaseDateStr = details.firstReleaseDate?.let {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it * 1000))
            }

            val dlcList = details.dlcs?.map { dlcDto ->
                DlcItem(
                    name = dlcDto.name,
                    coverUrl = dlcDto.cover?.url?.replace("t_thumb", "t_cover_big")?.let { "https:$it" },
                    releaseDate = dlcDto.firstReleaseDate?.let {
                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it * 1000))
                    }
                )
            }

            val updatedGame = game.copy(
                description = details.summary,
                igdbPress = details.aggregatedRating?.toInt(),
                igdbUser = details.rating?.toInt(),
                metacriticPress = scrapeResult.metaScore,
                metacriticUser = scrapeResult.userScore,
                opencriticPress = openCriticScore,
                opencriticUser = null,
                releaseDate = releaseDateStr,
                genre = details.genres?.firstOrNull()?.name ?: "Desconocido",
                developer = details.involvedCompanies?.find { it.developer }?.company?.name ?: "Desconocido",
                publisher = details.involvedCompanies?.find { it.publisher }?.company?.name ?: "Desconocido",
                ageRating = resolveAgeRating(finalAgeRatings, game.region),
                imageUrl = hdImageUrl ?: game.imageUrl,

                // 👇 Actualizamos DLCs
                dlcs = dlcList
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

    suspend fun getAllGamesSync(): List<Game> = gameDao.getAllGamesList()

    suspend fun restoreGames(games: List<Game>) {
        gameDao.insertAll(games)
    }

    // Backup restore helper
    suspend fun insertGame(game: Game) {
        gameDao.insertGame(game)
    }

    fun updateAllGames(): Flow<Int> = flow {
        val allGames = gameDao.getAllGamesList()
        val totalGames = allGames.size

        if (totalGames == 0) {
            emit(100)
            return@flow
        }

        var processedCount = 0
        emit(0)

        for (game in allGames) {
            try {
                fetchAndSaveGameDetails(game)
                kotlinx.coroutines.delay(200)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            processedCount++
            val percentage = ((processedCount.toFloat() / totalGames) * 100).toInt()
            emit(percentage)
        }
    }

    private fun resolveAgeRating(ratings: List<AgeRatingDto>?, targetRegion: String): String? {
        if (ratings.isNullOrEmpty()) return null

        fun getInfo(dto: AgeRatingDto): Pair<Int, Int> {
            val orgId = dto.organization ?: dto.category ?: -1
            val ratingValue = dto.rating ?: dto.ratingCategory ?: -1
            return Pair(orgId, ratingValue)
        }

        val targetOrgId = when (targetRegion) {
            "NTSC-U"  -> 1
            "PAL EU", "PAL" -> 2
            "NTSC-J"  -> 3
            "PAL DE"  -> 4
            "NTSC-K"  -> 5
            "NTSC-BR" -> 6
            "PAL AU"  -> 7
            else      -> 2
        }

        // 1. FRANCOTIRADOR
        val targetRating = ratings.find {
            val (orgId, _) = getInfo(it)
            orgId == targetOrgId
        }
        if (targetRating != null) return mapRatingToString(targetRating)

        // 2. PLAN B (Australia -> Proxy)
        if (targetOrgId == 7) {
            val proxy = ratings.find {
                val (orgId, _) = getInfo(it)
                orgId == 2 || orgId == 1
            }
            if (proxy != null) {
                val (proxyOrg, proxyVal) = getInfo(proxy)
                val fakeDto = AgeRatingDto(category = 7, rating = proxyVal)
                return mapRatingToString(fakeDto)
            }
        }

        // 3. Fallback Germany -> Europe
        if (targetOrgId == 4) {
            ratings.find {
                val (orgId, _) = getInfo(it)
                orgId == 2
            }?.let { return mapRatingToString(it) }
        }

        ratings.firstOrNull()?.let { return mapRatingToString(it) }

        return null
    }

    private fun mapRatingToString(dto: AgeRatingDto): String {
        val orgId = dto.organization ?: dto.category ?: -1
        val ratingValue = dto.rating ?: dto.ratingCategory ?: -1

        if (ratingValue == -1) return "UNK ($orgId)"

        return when (orgId) {
            1 -> { // ESRB
                when (ratingValue) {
                    6 -> "ESRB RP"
                    7 -> "ESRB EC"
                    8 -> "ESRB E"
                    9 -> "ESRB E10+"
                    10 -> "ESRB T"
                    11 -> "ESRB M"
                    12 -> "ESRB AO"
                    1 -> "ESRB E"
                    2 -> "ESRB E10+"
                    3 -> "ESRB T"
                    4 -> "ESRB M"
                    5 -> "ESRB AO"
                    else -> "ESRB ($ratingValue)"
                }
            }
            2 -> { // PEGI
                when (ratingValue) {
                    1 -> "PEGI 3"
                    2 -> "PEGI 7"
                    3 -> "PEGI 12"
                    4 -> "PEGI 16"
                    5 -> "PEGI 18"
                    8 -> "PEGI 3"
                    9 -> "PEGI 7"
                    10 -> "PEGI 12"
                    11 -> "PEGI 18"
                    12 -> "PEGI 18"
                    else -> "PEGI ($ratingValue)"
                }
            }
            3 -> { // CERO
                when (ratingValue) {
                    13 -> "CERO A"
                    14 -> "CERO B"
                    15 -> "CERO C"
                    16 -> "CERO D"
                    17 -> "CERO Z"
                    else -> "CERO ($ratingValue)"
                }
            }
            4 -> { // USK
                when (ratingValue) {
                    18 -> "USK 0"
                    19 -> "USK 6"
                    20 -> "USK 12"
                    21 -> "USK 16"
                    22 -> "USK 18"
                    else -> "USK ($ratingValue)"
                }
            }
            5 -> { // GRAC
                when (ratingValue) {
                    23 -> "GRAC ALL"
                    24 -> "GRAC 12"
                    25 -> "GRAC 15"
                    26 -> "GRAC 18"
                    39 -> "GRAC Test"
                    else -> "GRAC ($ratingValue)"
                }
            }
            6 -> { // ClassInd
                when (ratingValue) {
                    27 -> "ClassInd L"
                    28 -> "ClassInd 10"
                    29 -> "ClassInd 12"
                    30 -> "ClassInd 14"
                    31 -> "ClassInd 16"
                    32 -> "ClassInd 18"
                    else -> "ClassInd ($ratingValue)"
                }
            }
            7 -> { // ACB
                when (ratingValue) {
                    33 -> "ACB G"
                    34 -> "ACB PG"
                    35 -> "ACB M"
                    36 -> "ACB MA15+"
                    37 -> "ACB R18+"
                    38 -> "ACB RC"
                    1, 8 -> "ACB G"
                    2, 9, 10, 3 -> "ACB PG"
                    4, 11 -> "ACB M"
                    5 -> "ACB MA15+"
                    12 -> "ACB R18+"
                    else -> "ACB ($ratingValue)"
                }
            }
            else -> "UNK ($orgId|$ratingValue)"
        }
    }
}