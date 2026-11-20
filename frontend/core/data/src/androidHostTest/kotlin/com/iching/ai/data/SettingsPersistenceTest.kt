package com.iching.ai.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.iching.ai.data.settings.DataStoreSettingsRepository
import com.iching.ai.domain.model.AppLanguage
import com.iching.ai.domain.model.AppTheme
import com.iching.ai.domain.model.StreakState
import com.iching.ai.domain.siamsi.DivinationMode
import com.iching.ai.domain.siamsi.SiamSiUsage
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
    fun reviewCountersSurviveReloadAndDefaultOnUpgrade() = withStore { file ->
        val fresh = open(file) { it.settings.first() }
        assertEquals(0, fresh.completedReadings)
        assertEquals(null, fresh.lastReviewRequestEpochDay)
        open(file) { repo -> repo.update { it.copy(completedReadings = 4, lastReviewRequestEpochDay = 20_410L) } }
        Thread.sleep(100)
        val loaded = open(file) { it.settings.first() }
        assertEquals(4, loaded.completedReadings)
        assertEquals(20_410L, loaded.lastReviewRequestEpochDay)
    }

    @Test
    fun posterIdsAndFirstOpenSurviveReload() = withStore { file ->
        val fresh = open(file) { it.settings.first() }
        assertEquals(emptyList(), fresh.seenPosterIds)
        assertEquals(emptyList(), fresh.dismissedPosterCardIds)
        assertEquals(null, fresh.firstOpenEpochDay)
        open(file) { repo ->
            repo.update { it.copy(seenPosterIds = listOf("a", "b"), dismissedPosterCardIds = listOf("c"), firstOpenEpochDay = 20_400L) }
        }
        Thread.sleep(100)
        val loaded = open(file) { it.settings.first() }
        assertEquals(listOf("a", "b"), loaded.seenPosterIds)
        assertEquals(listOf("c"), loaded.dismissedPosterCardIds)
        assertEquals(20_400L, loaded.firstOpenEpochDay)
    }

    @Test
    fun siamsiFieldsSurviveReloadAndDefaultWhenMissing() = withStore { file ->
        val fresh = open(file) { it.settings.first() }
        assertEquals(DivinationMode.ICHING, fresh.divinationMode)
        assertEquals(null, fresh.siamsiSetId)
        assertEquals(SiamSiUsage(), fresh.siamsiUsage)
        assertEquals(false, fresh.siamsiLastEnabled)
        val usage = SiamSiUsage(20_400L, used = 1, pendingDraw = true)
        open(file) { repo ->
            repo.update { it.copy(divinationMode = DivinationMode.SIAMSI, siamsiSetId = "jiazi60", siamsiUsage = usage, siamsiLastEnabled = true) }
        }
        Thread.sleep(100)
        val loaded = open(file) { it.settings.first() }
        assertEquals(DivinationMode.SIAMSI, loaded.divinationMode)
        assertEquals("jiazi60", loaded.siamsiSetId)
        assertEquals(usage, loaded.siamsiUsage)
        assertEquals(true, loaded.siamsiLastEnabled)
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

    @Test
    fun clearKeepsTheChosenLanguage() = withStore { file ->
        open(file) { repo ->
            repo.update { it.copy(onboardingAccepted = true, language = AppLanguage.EN, lastEnglishEnabled = false) }
            repo.clear()
            val loaded = repo.settings.first()
            assertEquals(AppLanguage.EN, loaded.language)
            assertEquals(false, loaded.lastEnglishEnabled)
        }
    }

    @Test
    fun newInstallGetsTheGuidesUntilSeen() = withStore { file ->
        open(file) { repo ->
            repo.update { it.copy(firstOpenEpochDay = 20_400L) }
            repo.update { it.copy(onboardingAccepted = true) }
            val loaded = repo.settings.first()
            assertEquals(false, loaded.castGuideSeen)
            assertEquals(false, loaded.readingGuideSeen)
            repo.update { it.copy(castGuideSeen = true) }
            assertEquals(true, repo.settings.first().castGuideSeen)
            assertEquals(false, repo.settings.first().readingGuideSeen)
        }
    }

    @Test
    fun installFromBeforeTheGuidesSkipsThem() = withStore { file ->
        // A build without the guide keys wrote only the disclaimer flag.
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            PreferenceDataStoreFactory.create(scope = scope) { file }.edit { it[booleanPreferencesKey("onboarding_accepted_v1")] = true }
        } finally {
            scope.cancel()
        }
        Thread.sleep(100)
        val loaded = open(file) { it.settings.first() }
        assertEquals(true, loaded.castGuideSeen)
        assertEquals(true, loaded.readingGuideSeen)
    }

    @Test
    fun languageSurvivesReload() = withStore { file ->
        assertEquals(null, open(file) { it.settings.first() }.language)
        open(file) { repo -> repo.update { it.copy(language = AppLanguage.EN, lastEnglishEnabled = false) } }
        Thread.sleep(100)
        val loaded = open(file) { it.settings.first() }
        assertEquals(AppLanguage.EN, loaded.language)
        assertEquals(false, loaded.lastEnglishEnabled)
    }

    @Test
    fun installsFromBeforeLanguageReadAsThai() = withStore { file ->
        open(file) { repo -> repo.update { it.copy(onboardingAccepted = true) } }
        Thread.sleep(100)
        // Simulate the old data: no language key at all.
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val store = PreferenceDataStoreFactory.create(scope = scope) { file }
            store.edit { it.remove(stringPreferencesKey("language")) }
        } finally { scope.cancel() }
        Thread.sleep(100)
        assertEquals(AppLanguage.TH, open(file) { it.settings.first() }.language)
    }
}
