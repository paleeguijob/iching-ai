package com.iching.ai.ui.strings

import com.iching.ai.domain.model.AppLanguage
import com.iching.ai.domain.model.languageFor
import com.iching.ai.domain.usecase.ObserveSettingsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** The language in force, readable outside Compose (view models, session, Android entry points). */
class AppLanguageHolder(
    observeSettings: ObserveSettingsUseCase,
    englishEnabled: Flow<Boolean>,
    scope: CoroutineScope,
) {
    val language: StateFlow<AppLanguage> =
        combine(observeSettings(), englishEnabled) { settings, enabled -> settings.languageFor(enabled) }
            .stateIn(scope, SharingStarted.Eagerly, AppLanguage.TH)

    val strings: Strings get() = stringsFor(language.value)
}
