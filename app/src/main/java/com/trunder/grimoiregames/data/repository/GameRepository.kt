package com.trunder.grimoiregames.data.repository

import com.trunder.grimoiregames.data.dao.GameDao
import com.trunder.grimoiregames.data.entity.Game
import com.trunder.grimoiregames.data.remote.IgdbApi
import com.trunder.grimoiregames.data.remote.MetacriticScraper
import com.trunder.grimoiregames.data.remote.OpenCriticScraper // Asegúrate de importar esto
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

            // Scraper METACRITIC
            val scrapeResult = MetacriticScraper.scrapScores(details.name, selectedPlatform)

            // 👇 NUEVO: Scraper OPENCRITIC
            // Llamamos a la API de OpenCritic para obtener el "Top Critic Score"
            val openCriticScore = OpenCriticScraper.getScore(details.name)

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

                // 👇 Guardamos los nuevos datos de OpenCritic
                opencriticPress = openCriticScore,
                opencriticUser = null, // No disponible en API pública básica

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

            // 👇 NUEVO: Scraper OPENCRITIC (Actualización)
            val openCriticScore = OpenCriticScraper.getScore(details.name)

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

                // 👇 Actualizamos OpenCritic
                opencriticPress = openCriticScore,
                opencriticUser = null,

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

    private fun resolveAgeRating(ratings: List<AgeRatingDto>?, targetRegion: String): String? {
        if (ratings.isNullOrEmpty()) {
            android.util.Log.e("ORACLE_DEBUG", "❌ No hay ratings disponibles para este juego.")
            return null
        }

        fun getInfo(dto: AgeRatingDto): Pair<Int, Int> {
            val orgId = dto.organization ?: dto.category ?: -1
            val ratingValue = dto.rating ?: dto.ratingCategory ?: -1
            return Pair(orgId, ratingValue)
        }

        // --- INICIO DIAGNÓSTICO ---
        android.util.Log.d("ORACLE_DEBUG", "🔍 Analizando para región: $targetRegion (TargetOrgId calculado internamente)")
        ratings.forEach {
            val (o, r) = getInfo(it)
            android.util.Log.d("ORACLE_DEBUG", "   ➡️ Encontrado: Org=$o | Valor=$r | MapeoString=${mapRatingToString(it)}")
        }
        // --------------------------

        val targetOrgId = when (targetRegion) {
            "NTSC-U"  -> 1
            "PAL EU"  -> 2
            "PAL"     -> 2
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

        if (targetRating != null) {
            val (o, r) = getInfo(targetRating)
            android.util.Log.d("ORACLE_DEBUG", "🎯 FRANCOTIRADOR ACERTÓ: Usando Org $o con valor $r")
            return mapRatingToString(targetRating)
        } else {
            android.util.Log.d("ORACLE_DEBUG", "⚠️ Francotirador falló. Buscando Plan B...")
        }

        // 2. PLAN B (Australia -> Proxy)
        if (targetOrgId == 7) {
            val proxy = ratings.find {
                val (orgId, _) = getInfo(it)
                orgId == 2 || orgId == 1
            }

            if (proxy != null) {
                val (proxyOrg, proxyVal) = getInfo(proxy)
                android.util.Log.d("ORACLE_DEBUG", "🦘 PROXY AUSTRALIANO ACTIVADO: Usando Rating de Org $proxyOrg (Val $proxyVal) para simular ACB")

                val fakeDto = AgeRatingDto(category = 7, rating = proxyVal)
                val resultado = mapRatingToString(fakeDto)
                android.util.Log.d("ORACLE_DEBUG", "   📝 Resultado Proxy: $resultado")
                return resultado
            } else {
                android.util.Log.e("ORACLE_DEBUG", "❌ No se encontró ningún Proxy (ni ESRB ni PEGI).")
            }
        }

        // Resto de la función...
        if (targetOrgId == 4) {
            ratings.find {
                val (orgId, _) = getInfo(it)
                orgId == 2
            }?.let { return mapRatingToString(it) }
        }

        ratings.firstOrNull()?.let {
            android.util.Log.d("ORACLE_DEBUG", "🏳️ RENDICIÓN: Usando el primero que pille.")
            return mapRatingToString(it)
        }

        return null
    }

    private fun mapRatingToString(dto: AgeRatingDto): String {
        // Recuperamos los datos con prioridad corregida
        val orgId = dto.organization ?: dto.category ?: -1
        // Usamos ratingCategory como fuente principal de la nota (API v4 standard)
        val ratingValue = dto.rating ?: dto.ratingCategory ?: -1

        if (ratingValue == -1) return "UNK ($orgId)"

        return when (orgId) {
            1 -> { // === ESRB (USA) ===
                when (ratingValue) {
                    6 -> "ESRB RP"
                    7 -> "ESRB EC"
                    8 -> "ESRB E"
                    9 -> "ESRB E10+"
                    10 -> "ESRB T"
                    11 -> "ESRB M"
                    12 -> "ESRB AO"
                    // Cross-Mapping: Si llega código PEGI, lo traducimos a ESRB
                    1 -> "ESRB E"    // PEGI 3 -> E
                    2 -> "ESRB E10+" // PEGI 7 -> E10+
                    3 -> "ESRB T"    // PEGI 12 -> T
                    4 -> "ESRB M"    // PEGI 16 -> M
                    5 -> "ESRB AO"   // PEGI 18 -> AO
                    else -> "ESRB ($ratingValue)"
                }
            }
            2 -> { // === PEGI (Europe) ===
                when (ratingValue) {
                    1 -> "PEGI 3"
                    2 -> "PEGI 7"
                    3 -> "PEGI 12"
                    4 -> "PEGI 16"
                    5 -> "PEGI 18"
                    // Cross-Mapping: Si llega código ESRB, lo traducimos a PEGI
                    // Esto arregla "Metroid Prime 4" (Llega 10/Teen -> Sale PEGI 12)
                    // Esto arregla "Master Detective" (Llega 11/Mature -> Sale PEGI 18)
                    8 -> "PEGI 3"    // ESRB E -> PEGI 3
                    9 -> "PEGI 7"    // ESRB E10+ -> PEGI 7
                    10 -> "PEGI 12"  // ESRB Teen -> PEGI 12
                    11 -> "PEGI 18"  // ESRB Mature -> PEGI 18
                    12 -> "PEGI 18"  // ESRB AO -> PEGI 18
                    else -> "PEGI ($ratingValue)"
                }
            }
            3 -> { // === CERO (Japan) ===
                when (ratingValue) {
                    13 -> "CERO A"
                    14 -> "CERO B"
                    15 -> "CERO C"
                    16 -> "CERO D"
                    17 -> "CERO Z"
                    else -> "CERO ($ratingValue)"
                }
            }
            4 -> { // === USK (Germany) ===
                when (ratingValue) {
                    18 -> "USK 0"
                    19 -> "USK 6"
                    20 -> "USK 12"
                    21 -> "USK 16"
                    22 -> "USK 18"
                    else -> "USK ($ratingValue)"
                }
            }
            5 -> { // === GRAC (Korea) ===
                when (ratingValue) {
                    23 -> "GRAC ALL"
                    24 -> "GRAC 12"
                    25 -> "GRAC 15"
                    26 -> "GRAC 18"
                    39 -> "GRAC Test"
                    else -> "GRAC ($ratingValue)"
                }
            }
            6 -> { // === ClassInd (Brazil) ===
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
            7 -> { // === ACB (Australia) ===
                when (ratingValue) {
                    33 -> "ACB G"
                    34 -> "ACB PG"
                    35 -> "ACB M"
                    36 -> "ACB MA15+"
                    37 -> "ACB R18+"
                    38 -> "ACB RC"
                    // 🦘 CROSS-MAPPING AUSTRALIANO (Traducción de emergencia)
                    // Si el juego no tiene nota en Australia, traducimos la americana/europea.

                    // G (General) - Para todos
                    1 -> "ACB G"   // PEGI 3 -> G
                    8 -> "ACB G"   // ESRB E -> G

                    // PG (Parental Guidance) - Recomendado supervisión
                    2 -> "ACB PG"  // PEGI 7 -> PG
                    9 -> "ACB PG"  // ESRB E10+ -> PG (¡Aquí está tu Donkey Kong!)
                    10 -> "ACB PG" // ESRB Teen -> PG (A veces es M, pero PG es más seguro por defecto)
                    3 -> "ACB PG"  // PEGI 12 -> PG

                    // M (Mature) - Recomendado 15+ (No restringido)
                    4 -> "ACB M"   // PEGI 16 -> M
                    11 -> "ACB M"  // ESRB Mature -> M

                    // MA15+ / R18+ (Restringido)
                    5 -> "ACB MA15+" // PEGI 18 -> MA15+
                    12 -> "ACB R18+" // ESRB AO -> R18+

                    else -> "ACB ($ratingValue)"
                }
            }
            else -> "UNK ($orgId|$ratingValue)"
        }
    }
}