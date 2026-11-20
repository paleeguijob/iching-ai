package com.iching.ai.ui.strings

import com.iching.ai.domain.model.AccountStatus
import com.iching.ai.domain.model.AppLanguage
import com.iching.ai.domain.model.AppSettings
import com.iching.ai.domain.model.languageFor
import com.iching.ai.domain.usecase.ObserveSettingsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn

/** The language in force, readable outside Compose (view models, session, Android entry points). */
class AppLanguageHolder(
    observeSettings: ObserveSettingsUseCase,
    englishAllowed: Flow<Boolean>,
    scope: CoroutineScope,
) {
    /** Whether English may be offered at all (see [effectiveEnglishEnabled]). */
    val englishEnabled: StateFlow<Boolean> = englishAllowed.stateIn(scope, SharingStarted.Eagerly, true)

    val language: StateFlow<AppLanguage> =
        combine(observeSettings(), englishAllowed) { settings, enabled -> settings.languageFor(enabled) }
            .stateIn(scope, SharingStarted.Eagerly, AppLanguage.TH)

    val strings: Strings get() = stringsFor(language.value)
}

/**
 * Until the server has answered once, [AccountStatus.englishEnabled] is only a default; the flag from the last
 * real status, kept in settings, decides instead, so an offline launch keeps the seeker's language.
 */
fun effectiveEnglishEnabled(online: Flow<Boolean>, status: Flow<AccountStatus>, settings: Flow<AppSettings>): Flow<Boolean> =
    combine(online, status, settings) { isOnline, st, s -> if (isOnline) st.englishEnabled else s.lastEnglishEnabled }
        .distinctUntilChanged()
