package com.zaneschepke.wireguardautotunnel.data.datastore
import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class DataStoreManager(private val context: Context) {
    companion object {
        val LOCATION_DISCLOSURE_SHOWN = booleanPreferencesKey("LOCATION_DISCLOSURE_SHOWN")
    }

    // preferences
    private val preferencesKey = "preferences"
    private val Context.dataStore by preferencesDataStore(
        name = preferencesKey
    )

    suspend fun init() {
        context.dataStore.data.first()
    }

    suspend fun <T> saveToDataStore(key: Preferences.Key<T>, value: T) =
        context.dataStore.edit {
        it[key] = value
    }
    fun <T> getFromStoreFlow(key: Preferences.Key<T>) = context.dataStore.data.map {
        it[key]
    }

    suspend fun <T> getFromStore(key: Preferences.Key<T>) = context.dataStore.data.first { it.contains(key) }[key]

    val locationDisclosureFlow: Flow<Boolean?> = context.dataStore.data.map {
        it[LOCATION_DISCLOSURE_SHOWN]
    }
}
