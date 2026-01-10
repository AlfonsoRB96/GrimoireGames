package com.trunder.grimoiregames.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "games")
data class Game(
    @PrimaryKey(autoGenerate = true) val id: Int = 0, // ID Local de Room
    val rawgId: Int, // ¡NUEVO! Guardamos el ID de RAWG para poder refrescar datos en el futuro

    val title: String,
    val platform: String,
    val status: String,
    val imageUrl: String? = null,

    // --- TUS DATOS DE USUARIO ---
    val rating: Int? = null,
    val hoursPlayed: Int = 0,

    // --- DATOS ENRIQUECIDOS (NUEVOS) ---
    val description: String? = null,
    val genre: String? = null,      // Ej: "RPG"
    val developer: String? = null,  // Ej: "Square Enix"
    val publisher: String? = null,  // Ej: "SEGA"
    val metacritic: Int? = null,    // Ej: 85
    val releaseDate: String? = null // Ej: "2017-02-23"
)