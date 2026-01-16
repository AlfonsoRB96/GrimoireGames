package com.trunder.grimoiregames.data.repository

import com.trunder.grimoiregames.data.dao.GameDao
import com.trunder.grimoiregames.data.entity.DlcItem
import com.trunder.grimoiregames.data.entity.Game
import com.trunder.grimoiregames.data.remote.IgdbApi
import com.trunder.grimoiregames.data.remote.MetacriticScraper
import com.trunder.grimoiregames.data.remote.OpenCriticScraper
import com.trunder.grimoiregames.data.remote.TwitchAuthApi
import com.trunder.grimoiregames.data.remote.dto.AgeRatingCategory
import com.trunder.grimoiregames.data.remote.dto.AgeRatingDto
import com.trunder.grimoiregames.data.remote.dto.AgeRatingOrganization
import com.trunder.grimoiregames.data.remote.dto.IgdbGameDto
import com.trunder.grimoiregames.util.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class GameRepository @Inject constructor(
    private val gameDao: GameDao,
    private val igdbApi: IgdbApi,
    private val authApi: TwitchAuthApi
) {
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

    // --- BÚSQUEDA ---
    suspend fun searchGames(query: String): List<IgdbGameDto> {
        val token = getValidToken()
        val cleanQuery = query.trim()
        val bodyString = "search \"$cleanQuery\"; fields name, cover.url, first_release_date, total_rating, genres.name, platforms.name, summary, involved_companies.company.name, involved_companies.developer, involved_companies.publisher, age_ratings, game_type; where game_type = (0, 3, 4, 8, 9, 10, 11); limit 20;"
        val body = bodyString.toRequestBody("text/plain".toMediaTypeOrNull())
        return igdbApi.getGames(Constants.IGDB_CLIENT_ID, token, body)
    }

    // --- AÑADIR JUEGO ---
    suspend fun addGame(igdbId: Int, selectedPlatform: String, userRegion: String, onProgress: (Float, String) -> Unit) {
        android.util.Log.d("GRIMOIRE", "🎬 Iniciando addGame para ID: $igdbId")
        onProgress(0.1f, "Conectando con IGDB...")
        val token = getValidToken()

        val bodyString = """
            fields name, cover.url, first_release_date, rating, aggregated_rating, genres.name, platforms.name, summary, involved_companies.company.name, involved_companies.developer, involved_companies.publisher, age_ratings,
            dlcs.name, dlcs.cover.url, dlcs.first_release_date; 
            where id = $igdbId;
        """.trimIndent()
        val body = bodyString.toRequestBody("text/plain".toMediaTypeOrNull())

        try {
            onProgress(0.2f, "Descargando datos...")
            val games = igdbApi.getGames(Constants.IGDB_CLIENT_ID, token, body)
            val details = games.firstOrNull() ?: run {
                android.util.Log.e("GRIMOIRE", "❌ ERROR: Juego no encontrado.")
                return
            }

            // --- FASE 2: FRANCOTIRADOR ---
            var finalAgeRatings: List<AgeRatingDto>? = null
            if (!details.ageRatings.isNullOrEmpty()) {
                onProgress(0.3f, "Analizando clasificación de edad...")
                val ids = details.ageRatings.joinToString(",")
                val ratingQuery = "fields organization, rating_category; where id = ($ids);"
                val ratingBody = ratingQuery.toRequestBody("text/plain".toMediaTypeOrNull())

                val fetchedRatings = igdbApi.getAgeRatingDetails(Constants.IGDB_CLIENT_ID, token, ratingBody)
                if (fetchedRatings.isNotEmpty()) {
                    finalAgeRatings = fetchedRatings
                }
            }

            onProgress(0.5f, "Hackeando Metacritic...")
            val scrapeResult = MetacriticScraper.scrapScores(details.name, selectedPlatform)

            onProgress(0.7f, "Consultando OpenCritic...")
            val openCriticScore = OpenCriticScraper.getScore(details.name)

            onProgress(0.9f, "Guardando en Grimorio...")
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
                dlcs = dlcList
            )

            gameDao.insertGame(newGame)
            onProgress(1.0f, "Mission Complete!")

        } catch (e: Exception) {
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

            var finalAgeRatings: List<AgeRatingDto>? = null
            if (!details.ageRatings.isNullOrEmpty()) {
                val ids = details.ageRatings.joinToString(",")
                val ratingQuery = "fields organization, rating_category; where id = ($ids);"
                val ratingBody = ratingQuery.toRequestBody("text/plain".toMediaTypeOrNull())
                finalAgeRatings = igdbApi.getAgeRatingDetails(Constants.IGDB_CLIENT_ID, token, ratingBody)
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
                releaseDate = releaseDateStr,
                genre = details.genres?.firstOrNull()?.name ?: "Desconocido",
                developer = details.involvedCompanies?.find { it.developer }?.company?.name ?: "Desconocido",
                publisher = details.involvedCompanies?.find { it.publisher }?.company?.name ?: "Desconocido",
                ageRating = resolveAgeRating(finalAgeRatings, game.region),
                imageUrl = hdImageUrl ?: game.imageUrl,
                dlcs = dlcList
            )

            gameDao.updateGame(updatedGame)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteGame(game: Game) = gameDao.deleteGame(game)
    suspend fun updateGame(game: Game) = gameDao.updateGame(game)
    fun getGameById(id: Int) = gameDao.getGameById(id)
    suspend fun getAllGamesSync(): List<Game> = gameDao.getAllGamesList()
    suspend fun restoreGames(games: List<Game>) = gameDao.insertAll(games)
    suspend fun insertGame(game: Game) = gameDao.insertGame(game)
    fun updateAllGames(): Flow<Int> = flow {
        val allGames = gameDao.getAllGamesList()
        val totalGames = allGames.size
        if (totalGames == 0) { emit(100); return@flow }
        var processedCount = 0
        emit(0)
        for (game in allGames) {
            try { fetchAndSaveGameDetails(game); kotlinx.coroutines.delay(200) }
            catch (e: Exception) { e.printStackTrace() }
            processedCount++
            emit(((processedCount.toFloat() / totalGames) * 100).toInt())
        }
    }

    // ============================================================================================
    // 🧠 LOGICA "DECODER" DE ORACLE (FIX PARA BUG DE IGDB)
    // ============================================================================================

    private fun resolveAgeRating(ratings: List<AgeRatingDto>?, targetRegion: String): String? {
        if (ratings.isNullOrEmpty()) return null

        // 🕵️‍♀️ 1. ESTRATEGIA: IGNORAR ESRB (Org 1) y CENTRARNOS EN PEGI (Org 2)
        val pegiSource = ratings.find { it.organizationId == 2 }

        if (pegiSource != null) {
            // 🔧 CORRECCIÓN AQUÍ: Usamos .ratingId en vez de .ratingCategory
            val ratingId = pegiSource.ratingId ?: -1

            // ⚠️ DETECTOR DE ANOMALÍA
            val isEsrbDisguised = ratingId in 6..12

            if (isEsrbDisguised) {
                if (targetRegion == "NTSC-U") {
                    val fakeDto = AgeRatingDto(organizationId = 1, ratingId = ratingId) // Corrección en constructor
                    return mapRatingToString(fakeDto)
                }
                else if (targetRegion == "PAL" || targetRegion == "PAL EU") {
                    return mapEsrbIdToPegiString(ratingId)
                }
            } else {
                if (targetRegion == "PAL" || targetRegion == "PAL EU") {
                    return mapRatingToString(pegiSource)
                }
            }
        }

        // --- 2. FALLBACK ESTÁNDAR ---
        val targetOrgId = when (targetRegion) {
            "NTSC-U" -> 1
            "PAL EU", "PAL" -> 2
            "NTSC-J" -> 3
            "PAL DE" -> 4
            "NTSC-K" -> 5
            "NTSC-BR" -> 6
            "PAL AU" -> 7
            else -> 2
        }

        val match = ratings.find { it.organizationId == targetOrgId }
        if (match != null) return mapRatingToString(match)

        return ratings.firstOrNull()?.let { mapRatingToString(it) }
    }

    private fun mapEsrbIdToPegiString(esrbId: Int): String {
        return when (esrbId) {
            8 -> "PEGI 3"
            9 -> "PEGI 7"
            10 -> "PEGI 12"
            11 -> "PEGI 16"
            12 -> "PEGI 18"
            else -> "PEGI 3"
        }
    }

    private fun mapRatingToString(dto: AgeRatingDto): String {
        val orgId = dto.organization?.id ?: dto.organizationId ?: -1
        // 🔧 CORRECCIÓN AQUÍ TAMBIÉN: .ratingId
        val ratingValue = dto.rating?.id ?: dto.ratingId ?: -1

        if (ratingValue == -1) return "UNK"

        return when {
            // PEGI
            orgId == 2 -> when (ratingValue) {
                1 -> "PEGI 3"
                2 -> "PEGI 7"
                3 -> "PEGI 12"
                4 -> "PEGI 16"
                5 -> "PEGI 18"
                else -> "PEGI ($ratingValue)"
            }
            // ESRB
            orgId == 1 -> when (ratingValue) {
                6, 7 -> "ESRB RP"
                8 -> "ESRB E"
                9 -> "ESRB E10+"
                10 -> "ESRB T"
                11 -> "ESRB M"
                12 -> "ESRB AO"
                else -> "ESRB ($ratingValue)"
            }
            // CERO
            orgId == 3 -> when (ratingValue) {
                13 -> "CERO A"
                14 -> "CERO B"
                15 -> "CERO C"
                16 -> "CERO D"
                17 -> "CERO Z"
                else -> "CERO ($ratingValue)"
            }
            // USK
            orgId == 4 -> when (ratingValue) {
                18 -> "USK 0"
                19 -> "USK 6"
                20 -> "USK 12"
                21 -> "USK 16"
                22 -> "USK 18"
                else -> "USK ($ratingValue)"
            }
            // GRAC
            orgId == 5 -> when (ratingValue) {
                23, 27 -> "GRAC ALL"
                24 -> "GRAC 12"
                25 -> "GRAC 15"
                26 -> "GRAC 18"
                else -> "GRAC ($ratingValue)"
            }
            // CLASS IND
            orgId == 6 -> when (ratingValue) {
                28 -> "ClassInd L"
                29 -> "ClassInd 10"
                30 -> "ClassInd 12"
                31 -> "ClassInd 14"
                32 -> "ClassInd 16"
                33 -> "ClassInd 18"
                else -> "ClassInd ($ratingValue)"
            }
            // ACB
            orgId == 7 -> when (ratingValue) {
                34 -> "ACB G"
                35 -> "ACB PG"
                36 -> "ACB M"
                37 -> "ACB MA15+"
                38 -> "ACB R18+"
                39 -> "ACB RC"
                else -> "ACB ($ratingValue)"
            }
            else -> "UNK"
        }
    }
}