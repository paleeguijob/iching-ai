package com.iching.ai.domain.model

/** The language the app speaks to the seeker; [wire] is what the server and DataStore see. */
enum class AppLanguage(val wire: String) {
    TH("th"),
    EN("en");

    companion object {
        /** Unknown or missing falls back to Thai, so older data keeps behaving as before. */
        fun fromWire(v: String?): AppLanguage = entries.firstOrNull { it.wire == v } ?: TH
    }
}

/** The language in force: English only while the seeker chose it and the server still allows it. */
fun AppSettings.languageFor(englishEnabled: Boolean): AppLanguage =
    if (englishEnabled && language == AppLanguage.EN) AppLanguage.EN else AppLanguage.TH

/** A fresh install is asked once, before the disclaimer, and only while English is on. */
fun AppSettings.needsLanguagePick(englishEnabled: Boolean): Boolean =
    language == null && !onboardingAccepted && englishEnabled

/** Installs from before 2.0 never chose, and were Thai: keep them Thai. */
fun migrateLanguage(s: AppSettings): AppSettings =
    if (s.onboardingAccepted && s.language == null) s.copy(language = AppLanguage.TH) else s
