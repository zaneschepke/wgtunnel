package com.zaneschepke.wireguardautotunnel.repository

import androidx.room.TypeConverter

class DatabaseListConverters {
    @TypeConverter
    fun listToString(value: MutableList<String>): String {
        return value.joinToString()
    }
    @TypeConverter
    fun <T> stringToList(value: String): MutableList<String> {
        if(value.isEmpty()) return mutableListOf()
        return value.split(",").toMutableList()
    }
}