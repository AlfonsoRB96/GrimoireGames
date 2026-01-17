package com.trunder.grimoiregames.data.remote.dto

import com.google.gson.annotations.SerializedName

// Respuesta principal de un Juego en IGDB
data class IgdbGameDto(
    val id: Int,
    val name: String,
    val summary: String?, // Descripción
    @SerializedName("first_release_date")
    val firstReleaseDate: Long?, // ¡OJO! Viene en formato UNIX Timestamp (segundos)
    @SerializedName("aggregated_rating")
    val aggregatedRating: Double?, // Prensa
    @SerializedName("rating")
    val rating: Double?, // Usuarios (0-100)
    val cover: IgdbImageDto?, // Carátula
    val genres: List<IgdbItemDto>?,
    val platforms: List<IgdbItemDto>?,
    @SerializedName("involved_companies")
    val involvedCompanies: List<IgdbCompanyWrapperDto>?, // Desarrolladores/Publishers
    @SerializedName("age_ratings")
    val ageRatings: List<Long>?, // PEGI y ESRB

    // 👇 NUEVOS CAMPOS AGREGADOS POR ORACLE 👇
    val gameType: Int = 0, // 0 = Main, 1 = DLC, etc.
    // Aquí recibiremos los DLCs cuando pidamos el detalle
    // Nota: IGDB a veces los llama "dlcs" o "expansions". Usaremos "dlcs" por ahora.
    val dlcs: List<IgdbGameDto>?,
    // Opcional: Si quieres detectar si es un bundle y qué juegos trae
    @SerializedName("collection")
    val collection: IgdbItemDto?,

    @SerializedName("version_parent") val versionParent: IgdbGameDto? = null
)

data class IgdbImageDto(
    val id: Int,
    val url: String? // Ej: "//images.igdb.com/igdb/image/..."
)

data class IgdbItemDto(
    val id: Int,
    val name: String
)

// IGDB envuelve la compañía en un objeto intermedio
data class IgdbCompanyWrapperDto(
    val company: IgdbItemDto,
    val developer: Boolean,
    val publisher: Boolean
)

data class IgdbAgeRatingDto(
    val category: Int, // 1 = ESRB, 2 = PEGI
    val rating: Int    // El ID de la calificación (ej: 18+ etc)
)

// Respuesta de Autenticación de Twitch
data class TwitchTokenDto(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("expires_in") val expiresIn: Long,
    @SerializedName("token_type") val tokenType: String
)