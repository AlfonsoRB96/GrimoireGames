package com.trunder.grimoiregames.data.remote.dto

import com.google.gson.annotations.SerializedName

data class FranchiseDto(
    val id: Long,
    val name: String,
    val slug: String?
)