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
    val siamsi: SiamSiStrings
    val share: ShareStrings
    val daily: DailyStrings
    val year: YearStrings
    val poster: PosterStrings
    val journal: JournalStrings
    val library: LibraryStrings
    val settings: SettingsStrings
    val paywall: PaywallStrings
    val review: ReviewStrings
    val widget: WidgetStrings
}

private object ThStrings : Strings {
    override val common = ThCommonStrings
    override val home = ThHomeStrings
    override val divination = ThDivinationStrings
    override val reading = ThReadingStrings
    override val onboarding = ThOnboardingStrings
    override val siamsi = ThSiamSiStrings
    override val share = ThShareStrings
    override val daily = ThDailyStrings
    override val year = ThYearStrings
    override val poster = ThPosterStrings
    override val journal = ThJournalStrings
    override val library = ThLibraryStrings
    override val settings = ThSettingsStrings
    override val paywall = ThPaywallStrings
    override val review = ThReviewStrings
    override val widget = ThWidgetStrings
}

private object EnStrings : Strings {
    override val common = EnCommonStrings
    override val home = EnHomeStrings
    override val divination = EnDivinationStrings
    override val reading = EnReadingStrings
    override val onboarding = EnOnboardingStrings
    override val siamsi = EnSiamSiStrings
    override val share = EnShareStrings
    override val daily = EnDailyStrings
    override val year = EnYearStrings
    override val poster = EnPosterStrings
    override val journal = EnJournalStrings
    override val library = EnLibraryStrings
    override val settings = EnSettingsStrings
    override val paywall = EnPaywallStrings
    override val review = EnReviewStrings
    override val widget = EnWidgetStrings
}

fun stringsFor(lang: AppLanguage): Strings = when (lang) {
    AppLanguage.TH -> ThStrings
    AppLanguage.EN -> EnStrings
}

val LocalStrings: ProvidableCompositionLocal<Strings> = compositionLocalOf { stringsFor(AppLanguage.TH) }

val LocalAppLanguage: ProvidableCompositionLocal<AppLanguage> = compositionLocalOf { AppLanguage.TH }
