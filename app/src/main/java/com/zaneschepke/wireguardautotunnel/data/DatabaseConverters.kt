package com.zaneschepke.wireguardautotunnel.data

import androidx.room.TypeConverter
import com.zaneschepke.wireguardautotunnel.data.entity.Settings
import kotlinx.serialization.json.Json

class DatabaseConverters {
    @TypeConverter
    fun listToString(value: MutableList<String>): String {
        return Json.encodeToString(value)
    }

    @TypeConverter
    fun stringToList(value: String): MutableList<String> {
        if (value.isBlank() || value.isEmpty()) return mutableListOf()
        return try {
            Json.decodeFromString<MutableList<String>>(value)
        } catch (e: Exception) {
            val list = value.split(",").toMutableList()
            val json = listToString(list)
            Json.decodeFromString<MutableList<String>>(json)
        }
    }

    @TypeConverter fun fromStatus(status: Settings.WifiDetectionMethod): Int = status.value

    @TypeConverter
    fun toStatus(value: Int): Settings.WifiDetectionMethod =
        Settings.WifiDetectionMethod.fromValue(value)
}
