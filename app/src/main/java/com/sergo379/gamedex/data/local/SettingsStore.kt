package com.sergo379.gamedex.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import com.sergo379.gamedex.domain.LibrarySortMode

private val Context.dataStore by preferencesDataStore("settings")

class SettingsStore(private val context: Context) {
    private val apiKey = stringPreferencesKey("rawg_api_key")
    private val lastUpdate = longPreferencesKey("last_update")
    private val librarySort = stringPreferencesKey("library_sort")
    val apiKeyFlow: Flow<String> = context.dataStore.data.map { it[apiKey].orEmpty() }
    val lastUpdateFlow: Flow<Long?> = context.dataStore.data.map { it[lastUpdate] }
    val librarySortFlow: Flow<LibrarySortMode> = context.dataStore.data.map { preferences ->
        preferences[librarySort]
            ?.let { saved -> runCatching { LibrarySortMode.valueOf(saved) }.getOrNull() }
            ?: LibrarySortMode.ADDED_NEWEST
    }
    suspend fun apiKey(): String = apiKeyFlow.first()
    suspend fun saveApiKey(value: String) { context.dataStore.edit { it[apiKey] = value.trim() } }
    suspend fun markUpdated() { context.dataStore.edit { it[lastUpdate] = System.currentTimeMillis() } }
    suspend fun saveLibrarySort(value: LibrarySortMode) { context.dataStore.edit { it[librarySort] = value.name } }
}
