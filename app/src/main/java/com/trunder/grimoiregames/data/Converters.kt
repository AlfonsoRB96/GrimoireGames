package com.trunder.grimoiregames.data

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.trunder.grimoiregames.data.entity.DlcItem

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromDlcList(value: List<DlcItem>?): String? {
        if (value == null) return null
        val type = object : TypeToken<List<DlcItem>>() {}.type
        return gson.toJson(value, type)
    }

    @TypeConverter
    fun toDlcList(value: String?): List<DlcItem>? {
        if (value == null) return null
        val type = object : TypeToken<List<DlcItem>>() {}.type
        return gson.fromJson(value, type)
    }
}