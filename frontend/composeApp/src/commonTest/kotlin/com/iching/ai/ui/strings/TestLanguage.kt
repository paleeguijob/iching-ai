package com.iching.ai.ui.strings

import com.iching.ai.domain.model.AppLanguage
import com.iching.ai.domain.usecase.ObserveSettingsUseCase
import com.iching.ai.session.NoSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.flowOf

/** The Thai strings, for tests that check the copy the app has always shown. */
internal val TH_STRINGS: Strings = stringsFor(AppLanguage.TH)

/** A language holder that stays on Thai: its settings never emit, so the default language holds. */
internal fun thaiLanguageHolder() = AppLanguageHolder(ObserveSettingsUseCase(NoSettings), flowOf(true), CoroutineScope(Job()))
