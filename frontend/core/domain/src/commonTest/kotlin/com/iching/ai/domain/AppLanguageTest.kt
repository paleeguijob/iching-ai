package com.iching.ai.domain

import com.iching.ai.domain.model.AppLanguage
import com.iching.ai.domain.model.AppSettings
import com.iching.ai.domain.model.languageFor
import com.iching.ai.domain.model.migrateLanguage
import com.iching.ai.domain.model.needsLanguagePick
import kotlin.test.Test
import kotlin.test.assertEquals

class AppLanguageTest {
    @Test
    fun wireValuesParseAndUnknownFallsBackToThai() {
        assertEquals(AppLanguage.EN, AppLanguage.fromWire("en"))
        assertEquals(AppLanguage.TH, AppLanguage.fromWire("th"))
        assertEquals(AppLanguage.TH, AppLanguage.fromWire("fr"))
        assertEquals(AppLanguage.TH, AppLanguage.fromWire(null))
    }

    @Test
    fun englishIsOnlyUsedWhileTheServerAllowsIt() {
        assertEquals(AppLanguage.TH, AppSettings(language = AppLanguage.EN).languageFor(false))
        assertEquals(AppLanguage.EN, AppSettings(language = AppLanguage.EN).languageFor(true))
        assertEquals(AppLanguage.TH, AppSettings().languageFor(true))
    }

    @Test
    fun existingSeekersMigrateToThaiAndNewOnesKeepNull() {
        assertEquals(AppLanguage.TH, migrateLanguage(AppSettings(onboardingAccepted = true)).language)
        assertEquals(null, migrateLanguage(AppSettings()).language)
        assertEquals(AppLanguage.EN, migrateLanguage(AppSettings(onboardingAccepted = true, language = AppLanguage.EN)).language)
    }

    @Test
    fun theLanguagePickShowsOnlyOnFirstLaunchWithEnglishOn() {
        assertEquals(true, AppSettings().needsLanguagePick(true))
        assertEquals(false, AppSettings().needsLanguagePick(false))
        assertEquals(false, AppSettings(language = AppLanguage.TH).needsLanguagePick(true))
        assertEquals(false, AppSettings(onboardingAccepted = true).needsLanguagePick(true))
    }
}
