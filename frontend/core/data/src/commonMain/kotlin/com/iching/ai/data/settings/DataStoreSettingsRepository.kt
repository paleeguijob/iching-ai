package com.iching.ai.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.iching.ai.domain.model.AppSettings
import com.iching.ai.domain.model.AppTheme
import com.iching.ai.domain.model.StreakState
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
            prefs.writeStreak(next.streak)
            prefs[SELECTED_THEME] = next.selectedTheme.wire
            prefs[DAILY_PUSH_ENABLED] = next.dailyPushEnabled
            prefs[DAILY_PUSH_SYNCED] = next.dailyPushSynced
            prefs[PUSH_PROMPT_SHOWN] = next.pushPromptShown
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
        streak = StreakState(
            current = this[STREAK_CURRENT] ?: 0,
            best = this[STREAK_BEST] ?: 0,
            lastEpochDay = this[STREAK_LAST_DAY],
            unlocked = setOf(AppTheme.INK_PAPER) +
                this[UNLOCKED_THEMES].orEmpty().split(",").mapNotNull(AppTheme::fromWire),
        ),
        selectedTheme = AppTheme.fromWire(this[SELECTED_THEME]) ?: AppTheme.INK_PAPER,
        dailyPushEnabled = this[DAILY_PUSH_ENABLED] ?: false,
        dailyPushSynced = this[DAILY_PUSH_SYNCED] ?: true,
        pushPromptShown = this[PUSH_PROMPT_SHOWN] ?: false,
    )

    private fun MutablePreferences.writeStreak(streak: StreakState) {
        this[STREAK_CURRENT] = streak.current
        this[STREAK_BEST] = streak.best
        val last = streak.lastEpochDay
        if (last != null) this[STREAK_LAST_DAY] = last else remove(STREAK_LAST_DAY)
        this[UNLOCKED_THEMES] = streak.unlocked.joinToString(",") { it.wire }
    }

    private companion object {
        val SOUND = booleanPreferencesKey("sound_enabled")
        val ONBOARDING = booleanPreferencesKey("onboarding_accepted_v1")
        val STREAK_CURRENT = intPreferencesKey("streak_current")
        val STREAK_BEST = intPreferencesKey("streak_best")
        val STREAK_LAST_DAY = longPreferencesKey("streak_last_day")
        val UNLOCKED_THEMES = stringPreferencesKey("unlocked_themes")
        val SELECTED_THEME = stringPreferencesKey("selected_theme")
        val DAILY_PUSH_ENABLED = booleanPreferencesKey("daily_push_enabled")
        val DAILY_PUSH_SYNCED = booleanPreferencesKey("daily_push_synced")
        val PUSH_PROMPT_SHOWN = booleanPreferencesKey("push_prompt_shown")

        // Left over from builds that called Gemini directly; dropped on the next settings write.
        val LEGACY_API_KEY = stringPreferencesKey("gemini_api_key")
    }
}
