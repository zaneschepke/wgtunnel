package com.zaneschepke.wireguardautotunnel.data

import androidx.room.TypeConverter
import kotlinx.serialization.json.Json

class DatabaseListConverters {
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
}
