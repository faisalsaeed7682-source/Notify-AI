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
    private val USE_CLOUD_AI = booleanPreferencesKey("use_cloud_ai")

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
        
    val useCloudAi: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[USE_CLOUD_AI] ?: false
        }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_DARK_MODE] = enabled
        }
    }
    
    suspend fun setUseCloudAi(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[USE_CLOUD_AI] = enabled
        }
    }

    private val THEME_MODE = intPreferencesKey("theme_mode") // 0=System, 1=Light, 2=Dark
    private val SWIPE_LEFT_ACTION = intPreferencesKey("swipe_left_action") // 0=Delete, 1=Archive
    private val SWIPE_RIGHT_ACTION = intPreferencesKey("swipe_right_action") // 0=Delete, 1=Archive

    val themeMode: Flow<Int> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[THEME_MODE] ?: 4
        }

    val swipeLeftAction: Flow<Int> = context.dataStore.data.map { it[SWIPE_LEFT_ACTION] ?: 0 }
    val swipeRightAction: Flow<Int> = context.dataStore.data.map { it[SWIPE_RIGHT_ACTION] ?: 1 }

    suspend fun setSwipeLeftAction(action: Int) {
        context.dataStore.edit { it[SWIPE_LEFT_ACTION] = action }
    }

    suspend fun setSwipeRightAction(action: Int) {
        context.dataStore.edit { it[SWIPE_RIGHT_ACTION] = action }
    }

    suspend fun setThemeMode(mode: Int) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE] = mode
        }
    }

    private val FORCE_IN_APP_GMAIL = booleanPreferencesKey("force_in_app_gmail")
    private val FORCE_IN_APP_WHATSAPP = booleanPreferencesKey("force_in_app_whatsapp")
    private val FORCE_IN_APP_OUTLOOK = booleanPreferencesKey("force_in_app_outlook")

    val forceInAppGmail: Flow<Boolean> = context.dataStore.data.map { it[FORCE_IN_APP_GMAIL] ?: false }
    val forceInAppWhatsapp: Flow<Boolean> = context.dataStore.data.map { it[FORCE_IN_APP_WHATSAPP] ?: false }
    val forceInAppOutlook: Flow<Boolean> = context.dataStore.data.map { it[FORCE_IN_APP_OUTLOOK] ?: false }

    suspend fun setForceInAppGmail(enabled: Boolean) {
        context.dataStore.edit { it[FORCE_IN_APP_GMAIL] = enabled }
    }

    suspend fun setForceInAppWhatsapp(enabled: Boolean) {
        context.dataStore.edit { it[FORCE_IN_APP_WHATSAPP] = enabled }
    }

    suspend fun setForceInAppOutlook(enabled: Boolean) {
        context.dataStore.edit { it[FORCE_IN_APP_OUTLOOK] = enabled }
    }
}
