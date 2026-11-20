package com.iching.ai.ui.strings

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import com.iching.ai.domain.model.AppLanguage

/** Every user-visible string, grouped by section; each section lives in its own file with Th and En objects. */
interface Strings {
    val common: CommonStrings
    val home: HomeStrings
    val divination: DivinationStrings
    val reading: ReadingStrings
    val onboarding: OnboardingStrings
}

private object ThStrings : Strings {
    override val common = ThCommonStrings
    override val home = ThHomeStrings
    override val divination = ThDivinationStrings
    override val reading = ThReadingStrings
    override val onboarding = ThOnboardingStrings
}

private object EnStrings : Strings {
    override val common = EnCommonStrings
    override val home = EnHomeStrings
    override val divination = EnDivinationStrings
    override val reading = EnReadingStrings
    override val onboarding = EnOnboardingStrings
}

fun stringsFor(lang: AppLanguage): Strings = when (lang) {
    AppLanguage.TH -> ThStrings
    AppLanguage.EN -> EnStrings
}

val LocalStrings: ProvidableCompositionLocal<Strings> = compositionLocalOf { stringsFor(AppLanguage.TH) }

val LocalAppLanguage: ProvidableCompositionLocal<AppLanguage> = compositionLocalOf { AppLanguage.TH }
