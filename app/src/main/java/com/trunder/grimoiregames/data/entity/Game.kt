package com.trunder.grimoiregames.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "games")
data class Game(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val platform: String, // PC, PS5, Switch...
    val status: String,   // Playing, Completed, Backlog...
    val rating: Int? = null, // 1-10, opcional
    val hoursPlayed: Int = 0
)