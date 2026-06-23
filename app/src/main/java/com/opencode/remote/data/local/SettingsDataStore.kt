package com.opencode.remote.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.opencode.remote.domain.ServerConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "opencode_settings")

@Singleton
class SettingsDataStore @Inject constructor(
    private val context: Context
) {
    private object Keys {
        val HOST = stringPreferencesKey("host")
        val PORT = intPreferencesKey("port")
        val USERNAME = stringPreferencesKey("username")
        val PASSWORD = stringPreferencesKey("password")
        val THEME = stringPreferencesKey("theme")
        val LANGUAGE = stringPreferencesKey("language")
    }

    fun getConfig(): Flow<ServerConfig> {
        return context.dataStore.data.map { prefs ->
            ServerConfig(
                host = prefs[Keys.HOST] ?: "192.168.1.100",
                port = prefs[Keys.PORT] ?: 4096,
                username = prefs[Keys.USERNAME] ?: "opencode",
                password = prefs[Keys.PASSWORD] ?: ""
            )
        }
    }

    suspend fun saveConfig(config: ServerConfig) {
        context.dataStore.edit { prefs ->
            prefs[Keys.HOST] = config.host
            prefs[Keys.PORT] = config.port
            prefs[Keys.USERNAME] = config.username
            prefs[Keys.PASSWORD] = config.password
        }
    }

    fun getTheme(): Flow<String> {
        return context.dataStore.data.map { prefs ->
            prefs[Keys.THEME] ?: "system"
        }
    }

    suspend fun saveTheme(theme: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME] = theme
        }
    }

    fun getLanguage(): Flow<String> {
        return context.dataStore.data.map { prefs ->
            prefs[Keys.LANGUAGE] ?: "en"
        }
    }

    suspend fun saveLanguage(language: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LANGUAGE] = language
        }
    }
}
