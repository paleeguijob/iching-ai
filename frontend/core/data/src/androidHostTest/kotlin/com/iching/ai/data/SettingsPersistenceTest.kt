package com.iching.ai.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.iching.ai.data.settings.DataStoreSettingsRepository
import com.iching.ai.domain.model.AppTheme
import com.iching.ai.domain.model.StreakState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SettingsPersistenceTest {

    private fun <T> withStore(block: suspend (File) -> T): T = runBlocking {
        val dir = Files.createTempDirectory("settings").toFile()
        try {
            block(File(dir, "settings.preferences_pb"))
        } finally {
            dir.deleteRecursively()
        }
    }

    private suspend fun <T> open(file: File, block: suspend (DataStoreSettingsRepository) -> T): T {
        // DataStore allows one active instance per file, so each "launch" gets its own scope.
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val store = PreferenceDataStoreFactory.create(scope = scope) { file }
            return block(DataStoreSettingsRepository(store))
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun retentionFieldsSurviveReload() = withStore { file ->
        val streak = StreakState(8, 9, 100, setOf(AppTheme.INK_PAPER, AppTheme.BAMBOO))
        open(file) { repo ->
            repo.update {
                it.copy(
                    streak = streak,
                    selectedTheme = AppTheme.BAMBOO,
                    dailyPushEnabled = true,
                    dailyPushSynced = false,
                    pushPromptShown = true,
                )
            }
        }
        // Give the cancelled store a moment to release the file before reopening it.
        Thread.sleep(100)
        val loaded = open(file) { it.settings.first() }
        assertEquals(streak, loaded.streak)
        assertEquals(AppTheme.BAMBOO, loaded.selectedTheme)
        assertTrue(loaded.dailyPushEnabled)
        assertEquals(false, loaded.dailyPushSynced)
        assertTrue(loaded.pushPromptShown)
    }

    @Test
    fun unknownThemeWireFallsBackToDefault() = withStore { file ->
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val store = PreferenceDataStoreFactory.create(scope = scope) { file }
            store.edit {
                it[stringPreferencesKey("selected_theme")] = "from_a_future_version"
                it[stringPreferencesKey("unlocked_themes")] = "bamboo,from_a_future_version"
            }
            val loaded = DataStoreSettingsRepository(store).settings.first()
            assertEquals(AppTheme.INK_PAPER, loaded.selectedTheme)
            assertEquals(setOf(AppTheme.INK_PAPER, AppTheme.BAMBOO), loaded.streak.unlocked)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun clearResetsRetentionButKeepsOnboarding() = withStore { file ->
        open(file) { repo ->
            repo.update {
                it.copy(
                    onboardingAccepted = true,
                    streak = StreakState(8, 9, 100, setOf(AppTheme.INK_PAPER, AppTheme.BAMBOO)),
                    selectedTheme = AppTheme.BAMBOO,
                    dailyPushEnabled = true,
                )
            }
            repo.clear()
            val loaded = repo.settings.first()
            assertTrue(loaded.onboardingAccepted)
            assertEquals(StreakState(), loaded.streak)
            assertEquals(AppTheme.INK_PAPER, loaded.selectedTheme)
            assertEquals(false, loaded.dailyPushEnabled)
        }
    }
}
