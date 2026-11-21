package com.iching.ai.ui.strings

import com.iching.ai.domain.model.AppLanguage
import com.iching.ai.domain.model.AppSettings
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LanguageReadyTest {
    private val english = AppSettings(language = AppLanguage.EN)

    @Test
    fun nothingShowsBeforeTheSettingsLoad() {
        assertFalse(languageReady(null, englishEnabled = true, language = AppLanguage.TH))
    }

    @Test
    fun anEnglishSeekerWaitsForTheHolderToCatchUp() {
        // The holder starts on a Thai placeholder: that frame must not be drawn.
        assertFalse(languageReady(english, englishEnabled = true, language = AppLanguage.TH))
        assertTrue(languageReady(english, englishEnabled = true, language = AppLanguage.EN))
    }

    @Test
    fun thaiAndSwitchedOffEnglishAreReadyAsThai() {
        assertTrue(languageReady(AppSettings(language = AppLanguage.TH), englishEnabled = true, language = AppLanguage.TH))
        assertTrue(languageReady(AppSettings(), englishEnabled = true, language = AppLanguage.TH))
        assertTrue(languageReady(english, englishEnabled = false, language = AppLanguage.TH))
    }
}
