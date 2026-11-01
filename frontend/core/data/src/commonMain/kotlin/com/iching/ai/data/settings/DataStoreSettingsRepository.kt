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

/** Settings persisted on device. */
class DataStoreSettingsRepository(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override val settings: Flow<AppSettings> = dataStore.data.map { it.toSettings() }

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        dataStore.edit { prefs ->
            val next = transform(prefs.toSettings())
            prefs[SOUND] = next.soundEnabled
            prefs[ONBOARDING] = next.onboardingAccepted
            prefs.remove(LEGACY_API_KEY)
        }
    }

    /** Used after deleting the account. The disclaimer stays accepted on this device. */
    override suspend fun clear() {
        dataStore.edit { prefs ->
            val accepted = prefs[ONBOARDING] ?: false
            prefs.clear()
            prefs[ONBOARDING] = accepted
        }
    }

    private fun Preferences.toSettings() = AppSettings(
        soundEnabled = this[SOUND] ?: true,
        onboardingAccepted = this[ONBOARDING] ?: false,
    )

    private companion object {
        val SOUND = booleanPreferencesKey("sound_enabled")
        val ONBOARDING = booleanPreferencesKey("onboarding_accepted_v1")

        // Left over from builds that called Gemini directly; dropped on the next settings write.
        val LEGACY_API_KEY = stringPreferencesKey("gemini_api_key")
    }
}
