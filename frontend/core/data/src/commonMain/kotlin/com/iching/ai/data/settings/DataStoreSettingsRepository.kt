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
import com.iching.ai.domain.siamsi.DivinationMode
import com.iching.ai.domain.siamsi.SiamSiUsage
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
            prefs[CAST_GUIDE_SEEN] = next.castGuideSeen
            prefs[READING_GUIDE_SEEN] = next.readingGuideSeen
            prefs.writeStreak(next.streak)
            prefs[SELECTED_THEME] = next.selectedTheme.wire
            prefs[DAILY_PUSH_ENABLED] = next.dailyPushEnabled
            prefs[DAILY_PUSH_SYNCED] = next.dailyPushSynced
            prefs[PUSH_PROMPT_SHOWN] = next.pushPromptShown
            prefs[DIVINATION_MODE] = next.divinationMode.wire
            next.siamsiSetId?.let { prefs[SIAMSI_SET] = it } ?: prefs.remove(SIAMSI_SET)
            prefs[SIAMSI_DAY] = next.siamsiUsage.day
            prefs[SIAMSI_USED] = next.siamsiUsage.used
            prefs[SIAMSI_PENDING] = next.siamsiUsage.pendingDraw
            prefs[SIAMSI_LAST_ENABLED] = next.siamsiLastEnabled
            // One string, newline-joined: a string-set key would lose the order that decides which ids drop first.
            prefs[SEEN_POSTERS] = next.seenPosterIds.joinToString("\n")
            prefs[DISMISSED_POSTER_CARDS] = next.dismissedPosterCardIds.joinToString("\n")
            next.firstOpenEpochDay?.let { prefs[FIRST_OPEN_DAY] = it } ?: prefs.remove(FIRST_OPEN_DAY)
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
        // Installs from before the guides already know the app: with the disclaimer accepted and no key yet,
        // the guides count as seen. A new install writes both keys false when it accepts.
        castGuideSeen = this[CAST_GUIDE_SEEN] ?: (this[ONBOARDING] ?: false),
        readingGuideSeen = this[READING_GUIDE_SEEN] ?: (this[ONBOARDING] ?: false),
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
        divinationMode = DivinationMode.fromWire(this[DIVINATION_MODE]) ?: DivinationMode.ICHING,
        siamsiSetId = this[SIAMSI_SET],
        siamsiUsage = SiamSiUsage(
            day = this[SIAMSI_DAY] ?: 0L,
            used = this[SIAMSI_USED] ?: 0,
            pendingDraw = this[SIAMSI_PENDING] ?: false,
        ),
        siamsiLastEnabled = this[SIAMSI_LAST_ENABLED] ?: false,
        seenPosterIds = this[SEEN_POSTERS].lines(),
        dismissedPosterCardIds = this[DISMISSED_POSTER_CARDS].lines(),
        firstOpenEpochDay = this[FIRST_OPEN_DAY],
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
        val CAST_GUIDE_SEEN = booleanPreferencesKey("cast_guide_seen")
        val READING_GUIDE_SEEN = booleanPreferencesKey("reading_guide_seen")
        val STREAK_CURRENT = intPreferencesKey("streak_current")
        val STREAK_BEST = intPreferencesKey("streak_best")
        val STREAK_LAST_DAY = longPreferencesKey("streak_last_day")
        val UNLOCKED_THEMES = stringPreferencesKey("unlocked_themes")
        val SELECTED_THEME = stringPreferencesKey("selected_theme")
        val DAILY_PUSH_ENABLED = booleanPreferencesKey("daily_push_enabled")
        val DAILY_PUSH_SYNCED = booleanPreferencesKey("daily_push_synced")
        val PUSH_PROMPT_SHOWN = booleanPreferencesKey("push_prompt_shown")
        val DIVINATION_MODE = stringPreferencesKey("divination_mode")
        val SIAMSI_SET = stringPreferencesKey("siamsi_set")
        val SEEN_POSTERS = stringPreferencesKey("seen_posters")
        val DISMISSED_POSTER_CARDS = stringPreferencesKey("dismissed_poster_cards")
        val FIRST_OPEN_DAY = longPreferencesKey("first_open_day")
        val SIAMSI_DAY = longPreferencesKey("siamsi_day")
        val SIAMSI_USED = intPreferencesKey("siamsi_used")
        val SIAMSI_PENDING = booleanPreferencesKey("siamsi_pending")
        val SIAMSI_LAST_ENABLED = booleanPreferencesKey("siamsi_last_enabled")

        // Left over from builds that called Gemini directly; dropped on the next settings write.
        val LEGACY_API_KEY = stringPreferencesKey("gemini_api_key")
    }
}

/** A newline-joined id list back into ids; missing = none. */
private fun String?.lines(): List<String> = this?.split("\n")?.filter { it.isNotEmpty() } ?: emptyList()
