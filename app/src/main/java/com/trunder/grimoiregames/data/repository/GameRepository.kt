package com.trunder.grimoiregames.data.repository

import com.trunder.grimoiregames.data.dao.GameDao
import com.trunder.grimoiregames.data.entity.Game
import com.trunder.grimoiregames.data.remote.IgdbApi
import com.trunder.grimoiregames.data.remote.TwitchAuthApi
import com.trunder.grimoiregames.data.remote.dto.IgdbGameDto
import com.trunder.grimoiregames.util.Constants
import kotlinx.coroutines.flow.Flow
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

        // Esta query pide campos específicos, busca por nombre y filtra versiones raras
        val bodyString = """
            search "$query";
            fields name, cover.url, first_release_date, total_rating, genres.name, platforms.name, summary, involved_companies.company.name, involved_companies.developer, involved_companies.publisher, age_ratings.category, age_ratings.rating;
            where version_parent = null; 
            limit 20;
        """.trimIndent()

        val body = bodyString.toRequestBody("text/plain".toMediaTypeOrNull())

        return igdbApi.getGames(Constants.IGDB_CLIENT_ID, token, body)
    }

    // --- AÑADIR JUEGO (Mapeo completo) ---
    suspend fun addGame(igdbId: Int, selectedPlatform: String) {
        // LOG 1: Ver si entra a la función
        android.util.Log.d("DEBUG_APP", "🎬 Iniciando addGame para ID: $igdbId")

        val token = getValidToken()

        val bodyString = """
            fields name, cover.url, first_release_date, rating, aggregated_rating, genres.name, platforms.name, summary, involved_companies.company.name, involved_companies.developer, involved_companies.publisher, age_ratings.category, age_ratings.rating;
            where id = $igdbId;
        """.trimIndent()

        val body = bodyString.toRequestBody("text/plain".toMediaTypeOrNull())

        try {
            // LOG 2: Antes de llamar a la API
            android.util.Log.d("DEBUG_APP", "🌍 Llamando a IGDB...")

            val games = igdbApi.getGames(Constants.IGDB_CLIENT_ID, token, body)

            // LOG 3: Ver si la API devuelve algo
            android.util.Log.d("DEBUG_APP", "📦 Respuesta recibida. Juegos encontrados: ${games.size}")

            val details = games.firstOrNull()
            if (details == null) {
                android.util.Log.e("DEBUG_APP", "❌ ERROR: La lista de juegos vino vacía.")
                return
            }

            // Mapeo (resumido para no ocupar mucho)
            val hdImageUrl = details.cover?.url?.replace("t_thumb", "t_cover_big")?.let { "https:$it" }
            val releaseDateStr = details.firstReleaseDate?.let {
                java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(it * 1000))
            }

            // LOG 4: Datos procesados
            android.util.Log.d("DEBUG_APP", "✅ Datos procesados. Preparando inserción en BD...")

            val newGame = Game(
                rawgId = details.id,
                title = details.name,
                platform = selectedPlatform, // Usamos la que elegiste
                status = "Backlog",
                imageUrl = hdImageUrl,
                description = details.summary,
                metacritic = details.aggregatedRating?.toInt(),
                userRating = details.rating?.toInt(),
                releaseDate = releaseDateStr,
                genre = details.genres?.firstOrNull()?.name ?: "Desconocido",
                developer = details.involvedCompanies?.find { it.developer }?.company?.name ?: "Desconocido",
                publisher = details.involvedCompanies?.find { it.publisher }?.company?.name ?: "Desconocido",
                // Mapeo seguro de nulls para ESRB/PEGI
                esrb = mapEsrbIdToText(details.ageRatings?.find { it.category == 1 }?.rating),
                pegi = mapPegiIdToText(details.ageRatings?.find { it.category == 2 }?.rating)
            )

            // LOG 5: Intentando guardar
            gameDao.insertGame(newGame)
            android.util.Log.d("DEBUG_APP", "🎉 ¡ÉXITO! Juego guardado en Room.")

        } catch (e: Exception) {
            // LOG DE ERROR: ESTO ES LO QUE NECESITAMOS VER
            android.util.Log.e("DEBUG_APP", "💀 ERROR CRÍTICO al guardar: ${e.message}")
            e.printStackTrace()
        }
    }

    // 👇 FUNCIÓN RECUPERADA Y ADAPTADA A IGDB
    // Sirve para refrescar los datos (nota, fecha, etc.) cuando entras al detalle
    suspend fun fetchAndSaveGameDetails(game: Game) {
        val token = getValidToken()

        // 👇 CORRECCIÓN:
        // 1. Usamos ${game.rawgId} porque el ID está dentro del objeto Game.
        // 2. Pedimos 'rating' y 'aggregated_rating'.
        val bodyString = """
            fields name, cover.url, first_release_date, rating, aggregated_rating, genres.name, platforms.name, summary, involved_companies.company.name, involved_companies.developer, involved_companies.publisher, age_ratings.category, age_ratings.rating;
            where id = ${game.rawgId};
        """.trimIndent()

        val body = bodyString.toRequestBody("text/plain".toMediaTypeOrNull())

        try {
            val games = igdbApi.getGames(Constants.IGDB_CLIENT_ID, token, body)
            val details = games.firstOrNull() ?: return

            // Mapeo (Imagen HD, Fecha, etc...)
            val hdImageUrl = details.cover?.url?.replace("t_thumb", "t_cover_big")?.let { "https:$it" }

            val releaseDateStr = details.firstReleaseDate?.let {
                java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(it * 1000))
            }

            // Actualizamos el juego
            val updatedGame = game.copy(
                description = details.summary,
                // 👇 ACTUALIZAMOS LAS DOS NOTAS
                metacritic = details.aggregatedRating?.toInt(), // Prensa
                userRating = details.rating?.toInt(),           // Usuarios

                releaseDate = releaseDateStr,
                genre = details.genres?.firstOrNull()?.name ?: "Desconocido",
                developer = details.involvedCompanies?.find { it.developer }?.company?.name ?: "Desconocido",
                publisher = details.involvedCompanies?.find { it.publisher }?.company?.name ?: "Desconocido",

                // Mapeo de PEGI/ESRB
                esrb = mapEsrbIdToText(details.ageRatings?.find { it.category == 1 }?.rating),
                pegi = mapPegiIdToText(details.ageRatings?.find { it.category == 2 }?.rating),

                imageUrl = hdImageUrl ?: game.imageUrl
            )

            gameDao.updateGame(updatedGame)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteGame(game: Game) = gameDao.deleteGame(game)
    suspend fun updateGame(game: Game) = gameDao.updateGame(game)
    fun getGameById(id: Int) = gameDao.getGameById(id)

    // --- HELPERS DE MAPEO (IGDB usa números para las edades) ---
    private fun mapPegiIdToText(id: Int?): String? {
        return when (id) {
            1 -> "PEGI 3"
            2 -> "PEGI 7"
            3 -> "PEGI 12"
            4 -> "PEGI 16"
            5 -> "PEGI 18"
            else -> null
        }
    }

    private fun mapEsrbIdToText(id: Int?): String? {
        return when (id) {
            6 -> "RP" // Rating Pending
            7 -> "EC" // Early Childhood
            8 -> "E"  // Everyone
            9 -> "E10+" // Everyone 10+
            10 -> "T" // Teen
            11 -> "M" // Mature
            12 -> "AO" // Adults Only
            else -> null
        }
    }
}