package com.iching.ai.ui.strings

import kotlin.test.Test
import kotlin.test.assertEquals

/** The privacy and terms links open the page in the reader's language; Thai keeps the original paths. */
class LegalLinksTest {
    @Test
    fun thaiKeepsTheOriginalPaths() {
        listOf(
            ThSettingsStrings.privacyPath to ThSettingsStrings.termsPath,
            ThPaywallStrings.privacyPath to ThPaywallStrings.termsPath,
            ThOnboardingStrings.privacyPath to ThOnboardingStrings.termsPath,
        ).forEach { (privacy, terms) ->
            assertEquals("/privacy", privacy)
            assertEquals("/terms", terms)
        }
    }

    @Test
    fun englishOpensTheEnglishPages() {
        listOf(
            EnSettingsStrings.privacyPath to EnSettingsStrings.termsPath,
            EnPaywallStrings.privacyPath to EnPaywallStrings.termsPath,
            EnOnboardingStrings.privacyPath to EnOnboardingStrings.termsPath,
        ).forEach { (privacy, terms) ->
            assertEquals("/en/privacy", privacy)
            assertEquals("/en/terms", terms)
        }
    }
}
