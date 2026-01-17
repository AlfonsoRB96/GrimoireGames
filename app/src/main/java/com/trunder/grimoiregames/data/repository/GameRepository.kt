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
        // Nota: En búsqueda simple no traemos toda la herencia profunda para no ralentizar,
        // solo lo básico por si acaso.
        val bodyString = "search \"$cleanQuery\"; fields name, cover.url, first_release_date, total_rating, genres.name, platforms.name, summary, involved_companies.company.name, involved_companies.developer, involved_companies.publisher, age_ratings, game_type, parent_game, version_parent.name, version_parent.summary, franchises.name; where game_type = (0, 3, 4, 8, 9, 10, 11); limit 20;"
        val body = bodyString.toRequestBody("text/plain".toMediaTypeOrNull())
        return igdbApi.getGames(Constants.IGDB_CLIENT_ID, token, body)
    }

    // --- AÑADIR JUEGO ---
    suspend fun addGame(igdbId: Int, selectedPlatform: String, userRegion: String, onProgress: (Float, String) -> Unit) {
        android.util.Log.d("GRIMOIRE", "🎬 Iniciando addGame para ID: $igdbId")
        onProgress(0.1f, "Conectando con IGDB...")
        val token = getValidToken()

        // 🟢 AÑADIDO: franchises.name y version_parent.franchises.name
        val bodyString = """
            fields name, cover.url, first_release_date, rating, aggregated_rating, genres.name, platforms.name, summary, involved_companies.company.name, involved_companies.developer, involved_companies.publisher, age_ratings,
            dlcs.name, dlcs.cover.url, dlcs.first_release_date, 
            game_type, franchises.name,
            version_parent.name, 
            version_parent.summary,
            version_parent.franchises.name,
            version_parent.involved_companies.company.name, 
            version_parent.involved_companies.developer, 
            version_parent.involved_companies.publisher,
            version_parent.dlcs.name, 
            version_parent.dlcs.cover.url, 
            version_parent.dlcs.first_release_date; 
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

            // --- FASE 2: CLASIFICACIÓN DE EDAD ---
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

            // =================================================================
            // 🕵️‍♀️ LÓGICA ORACLE: PROTOCOLO DE HERENCIA TOTAL
            // =================================================================
            val hasParent = details.versionParent != null

            // 1. Nombre para Scraper (Del Padre)
            val nameForScraping = if (hasParent) details.versionParent!!.name else details.name

            // 2. Summary Fusionado (Padre + Hijo)
            val finalSummary = if (hasParent) {
                val parentSum = details.versionParent.summary ?: ""
                val childSum = details.summary ?: ""
                if (parentSum.isNotBlank()) "$parentSum\n\n🔹 ${details.name}:\n$childSum" else childSum
            } else {
                details.summary
            }

            // 3. 🟢 Developer & Publisher (Heredado del Padre si existe)
            val companiesSource = if (hasParent && !details.versionParent.involvedCompanies.isNullOrEmpty()) {
                details.versionParent.involvedCompanies
            } else {
                details.involvedCompanies
            }

            val developerName = companiesSource?.find { it.developer }?.company?.name ?: "Desconocido"
            val publisherName = companiesSource?.find { it.publisher }?.company?.name ?: "Desconocido"

            // 4. 🟢 DLCs (Heredado del Padre: mostramos los "hermanos" y el contenido del padre)
            val dlcsSource = if (hasParent && !details.versionParent?.dlcs.isNullOrEmpty()) {
                details.versionParent.dlcs
            } else {
                details.dlcs
            }

            // Intentamos coger la franquicia del padre primero, si no, la del propio juego
            val franchiseName = if (hasParent && !details.versionParent?.franchises.isNullOrEmpty()) {
                details.versionParent.franchises.first().name
            } else {
                details.franchises?.firstOrNull()?.name
            }
            // =================================================================

            onProgress(0.5f, "Hackeando Metacritic ($nameForScraping)...")
            val scrapeResult = MetacriticScraper.scrapScores(nameForScraping, selectedPlatform)

            onProgress(0.7f, "Consultando OpenCritic...")
            val openCriticScore = OpenCriticScraper.getScore(nameForScraping)

            onProgress(0.9f, "Guardando en Grimorio...")
            val hdImageUrl = details.cover?.url?.replace("t_thumb", "t_cover_big")?.let { "https:$it" }
            val releaseDateStr = details.firstReleaseDate?.let {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it * 1000))
            }

            // Mapeamos los DLCs seleccionados (Padre o Propios)
            val dlcList = dlcsSource?.map { dlcDto ->
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
                description = finalSummary, // 🟢 Fusionado
                igdbPress = details.aggregatedRating?.toInt(),
                igdbUser = details.rating?.toInt(),
                metacriticPress = scrapeResult.metaScore,
                metacriticUser = scrapeResult.userScore,
                opencriticPress = openCriticScore,
                opencriticUser = null,
                releaseDate = releaseDateStr,
                genre = details.genres?.firstOrNull()?.name ?: "Desconocido",
                developer = developerName, // 🟢 Heredado
                publisher = publisherName, // 🟢 Heredado
                region = userRegion,
                ageRating = resolveAgeRating(finalAgeRatings, userRegion),
                dlcs = dlcList, // 🟢 Heredado
                franchise = franchiseName
            )

            gameDao.insertGame(newGame)
            onProgress(1.0f, "Mission Complete!")

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun fetchAndSaveGameDetails(game: Game) {
        val token = getValidToken()

        // 🟢 UPDATE QUERY: También aquí pedimos la estructura profunda del padre
        val gameQuery = """
            fields name, cover.url, first_release_date, rating, aggregated_rating, genres.name, platforms.name, summary, involved_companies.company.name, involved_companies.developer, involved_companies.publisher, age_ratings, 
            dlcs.name, dlcs.cover.url, dlcs.first_release_date, 
            game_type, franchises.name,
            version_parent.name, 
            version_parent.summary,
            version_parent.franchises.name,
            version_parent.involved_companies.company.name, 
            version_parent.involved_companies.developer, 
            version_parent.involved_companies.publisher,
            version_parent.dlcs.name, 
            version_parent.dlcs.cover.url, 
            version_parent.dlcs.first_release_date; 
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

            // =================================================================
            // 🕵️‍♀️ LÓGICA ORACLE: PROTOCOLO DE HERENCIA (UPDATE)
            // =================================================================
            val hasParent = details.versionParent != null

            val nameForScraping = if (hasParent) details.versionParent.name else details.name

            val finalSummary = if (hasParent) {
                val parentSum = details.versionParent.summary ?: ""
                val childSum = details.summary ?: ""
                if (parentSum.isNotBlank()) "$parentSum\n\n🔹 ${details.name}:\n$childSum" else childSum
            } else {
                details.summary
            }

            // 🟢 Heredar Companies
            val companiesSource = if (hasParent && !details.versionParent.involvedCompanies.isNullOrEmpty()) {
                details.versionParent.involvedCompanies
            } else {
                details.involvedCompanies
            }
            val developerName = companiesSource?.find { it.developer }?.company?.name ?: "Desconocido"
            val publisherName = companiesSource?.find { it.publisher }?.company?.name ?: "Desconocido"

            // 🟢 Heredar DLCs
            val dlcsSource = if (hasParent && !details.versionParent.dlcs.isNullOrEmpty()) {
                details.versionParent.dlcs
            } else {
                details.dlcs
            }

            val franchiseName = if (hasParent && !details.versionParent.franchises.isNullOrEmpty()) {
                details.versionParent.franchises.first().name
            } else {
                details.franchises?.firstOrNull()?.name
            }
            // =================================================================

            val scrapeResult = MetacriticScraper.scrapScores(nameForScraping, game.platform)
            val openCriticScore = OpenCriticScraper.getScore(nameForScraping)

            val hdImageUrl = details.cover?.url?.replace("t_thumb", "t_cover_big")?.let { "https:$it" }
            val releaseDateStr = details.firstReleaseDate?.let {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it * 1000))
            }

            val dlcList = dlcsSource?.map { dlcDto ->
                DlcItem(
                    name = dlcDto.name,
                    coverUrl = dlcDto.cover?.url?.replace("t_thumb", "t_cover_big")?.let { "https:$it" },
                    releaseDate = dlcDto.firstReleaseDate?.let {
                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it * 1000))
                    }
                )
            }

            val updatedGame = game.copy(
                description = finalSummary,
                igdbPress = details.aggregatedRating?.toInt(),
                igdbUser = details.rating?.toInt(),
                metacriticPress = scrapeResult.metaScore,
                metacriticUser = scrapeResult.userScore,
                opencriticPress = openCriticScore,
                releaseDate = releaseDateStr,
                genre = details.genres?.firstOrNull()?.name ?: "Desconocido",
                developer = developerName, // 🟢 Update Heredado
                publisher = publisherName, // 🟢 Update Heredado
                ageRating = resolveAgeRating(finalAgeRatings, game.region),
                imageUrl = hdImageUrl ?: game.imageUrl,
                dlcs = dlcList, // 🟢 Update Heredado
                franchise = franchiseName
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
    // 🧠 LOGICA DE CLASIFICACIÓN (SIN CAMBIOS AQUÍ)
    // ============================================================================================

    private fun resolveAgeRating(ratings: List<AgeRatingDto>?, targetRegion: String): String? {
        if (ratings.isNullOrEmpty()) return null

        val pegiSource = ratings.find { it.organizationId == 2 }

        if (pegiSource != null) {
            val ratingId = pegiSource.ratingId ?: -1
            val isEsrbDisguised = ratingId in 6..12

            if (isEsrbDisguised) {
                if (targetRegion == "NTSC-U") {
                    val fakeDto = AgeRatingDto(organizationId = 1, ratingId = ratingId)
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