package com.trunder.grimoiregames.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "games")
data class Game(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0, // ID Local de Room
    val rawgId: Int,
    val title: String,
    val platform: String,
    val status: String,
    val imageUrl: String? = null,
    val rating: Int? = null,
    val hoursPlayed: Int = 0,
    val description: String? = null,
    val genre: String? = null,      // Ej: "RPG"
    val developer: String? = null,  // Ej: "Square Enix"
    val publisher: String? = null,  // Ej: "SEGA"
    val userRating: Int? = null,
    val releaseDate: String? = null, // Ej: "2017-02-23"
    val pegi: String? = null,       // Ej: "3", "7", "12", "16", "18"
    val esrb: String? = null,        // Ej: "E", "T", "M", "AO"
    val igdbPress: Int? = null,
    val igdbUser: Int? = null,
    val metacriticPress: Int? = null,
    val metacriticUser: Int? = null,
    val opencriticPress: Int? = null
) {
// Helper para mostrar la mejor nota disponible
val displayScore: Int?
    get() = metacriticPress ?: opencriticPress ?: igdbPress
}