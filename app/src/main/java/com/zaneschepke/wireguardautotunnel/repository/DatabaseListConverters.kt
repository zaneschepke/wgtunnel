package com.zaneschepke.wireguardautotunnel.repository

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DatabaseListConverters {
    @TypeConverter
    fun listToString(value: MutableList<String>): String {
        return Json.encodeToString(value)
    }
    @TypeConverter
    fun stringToList(value: String): MutableList<String> {
        if(value.isEmpty()) return mutableListOf()
        return Json.decodeFromString<MutableList<String>>(value)
    }
}