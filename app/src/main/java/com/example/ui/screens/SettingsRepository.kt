package com.example.ui.screens

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
    private val IS_SECURE = booleanPreferencesKey("is_secure")
    private val AUTO_DELETE_DAYS = intPreferencesKey("auto_delete_days")

    val isDarkMode: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[IS_DARK_MODE] ?: true // Default dark mode
        }
        
    val isSecure: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[IS_SECURE] ?: true
        }

    val autoDeleteDays: Flow<Int> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[AUTO_DELETE_DAYS] ?: 0
        }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_DARK_MODE] = enabled
        }
    }
    
    suspend fun setSecure(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_SECURE] = enabled
        }
    }

    suspend fun setAutoDeleteDays(days: Int) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_DELETE_DAYS] = days
        }
    }
}
