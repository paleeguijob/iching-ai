package com.iching.ai.ui.strings

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import com.iching.ai.domain.model.AppLanguage

/** Every user-visible string, grouped by section; each section lives in its own file with Th and En objects. */
interface Strings {
    val common: CommonStrings
}

private object ThStrings : Strings {
    override val common = ThCommonStrings
}

private object EnStrings : Strings {
    override val common = EnCommonStrings
}

fun stringsFor(lang: AppLanguage): Strings = when (lang) {
    AppLanguage.TH -> ThStrings
    AppLanguage.EN -> EnStrings
}

val LocalStrings: ProvidableCompositionLocal<Strings> = compositionLocalOf { stringsFor(AppLanguage.TH) }

val LocalAppLanguage: ProvidableCompositionLocal<AppLanguage> = compositionLocalOf { AppLanguage.TH }
