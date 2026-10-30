package com.iching.ai.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.iching.ai.domain.model.AppSettings
import com.iching.ai.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Settings persisted on device. When the user has never saved a key, [defaultApiKey]
 * (from the build) is used; an explicitly cleared key is stored as "" and respected.
 */
class DataStoreSettingsRepository(
    private val dataStore: DataStore<Preferences>,
    private val defaultApiKey: String,
) : SettingsRepository {

    override val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            apiKey = prefs[API_KEY] ?: defaultApiKey,
            model = prefs[MODEL]?.takeIf { it.isNotBlank() } ?: AppSettings.DEFAULT_MODEL,
            soundEnabled = prefs[SOUND] ?: true,
        )
    }

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        dataStore.edit { prefs ->
            val current = AppSettings(
                apiKey = prefs[API_KEY] ?: defaultApiKey,
                model = prefs[MODEL]?.takeIf { it.isNotBlank() } ?: AppSettings.DEFAULT_MODEL,
                soundEnabled = prefs[SOUND] ?: true,
            )
            val next = transform(current)
            prefs[API_KEY] = next.apiKey.trim()
            prefs[MODEL] = next.model.trim()
            prefs[SOUND] = next.soundEnabled
        }
    }

    private companion object {
        val API_KEY = stringPreferencesKey("gemini_api_key")
        val MODEL = stringPreferencesKey("gemini_model")
        val SOUND = booleanPreferencesKey("sound_enabled")
    }
}
