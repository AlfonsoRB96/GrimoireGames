package com.trunder.grimoiregames.data.entity

// Una clase "POJO" ligera para guardar dentro de la lista
data class DlcItem(
    val name: String,
    val coverUrl: String?,
    val releaseDate: String? // "YYYY-MM-DD"
)