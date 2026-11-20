package com.iching.ai.ui.strings

import com.iching.ai.domain.model.LineType
import com.iching.ai.domain.oracle.ReadingSection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Every string the English objects can produce, called with sample values, must hold no Thai letters.
 * The lists are written out by hand (no reflection): a new member of a section goes into its list here.
 */
class NoThaiInEnglishTest {
    private val thai = Regex("[฀-๿]")

    private fun check(section: String, strings: List<String>) {
        assertTrue(strings.isNotEmpty(), section)
        strings.forEachIndexed { i, text -> assertFalse(thai.containsMatchIn(text), "$section #$i has Thai: $text") }
    }

    @Test
    fun common() {
        val s = EnCommonStrings
        check("common", listOf(
            s.cancel, s.close, s.gotIt, s.back, s.number(3), s.premiumDaily, s.premiumDailyNoAds, s.coinPack, s.working,
            s.openingAd, s.comeBackTomorrow, s.adMustFinish, s.adLimitReached, s.adUnavailable, s.paymentFailed,
        ))
    }

    @Test
    fun home() {
        val s = EnHomeStrings
        check("home", listOf(
            s.tabAsk, s.tabLibrary, s.tabJournal, s.appName, s.modeIching, s.modeSiamsi, s.settings, s.askPlaceholderSiamsi,
            s.askPlaceholder, s.yearPlaceholder, s.askYearTitle, s.dailyHexagram, s.dailyHexagramPending, s.ritualPrompt,
            s.ritualCaption, s.setButton("Set A"), s.siamsiWatchAd, s.siamsiShake, s.siamsiPaywall, s.artHintWatchAd,
            s.artHintPaywall, s.startButton, s.startButtonDeciding, s.premiumPrefix, s.quotaPaused, s.quotaToday,
            s.quotaCoins, s.quotaAds, s.coinsNote, s.quotaAdsNote, s.premiumOutNote, s.freeOutNote, s.quotaUnit,
            s.quotaPlusCoins(1), s.quotaPlusCoins(3), s.quotaSpoken("Label", 3), s.preCastBlankQuestion, s.preCastAdTitle,
            s.preCastAdBody, s.preCastWatchAd, s.preCastWithoutWords, s.preCastReady, s.siamsiPreCastWatchAd,
            s.siamsiPreCastWithoutWords, s.siamsiPreCastReady, s.adsLeftSuffix(3), s.preCastUpsellTitle, s.preCastUpsellBody,
            s.preCastReadyTitle, s.productsFailed, s.adUnavailableOrScripture, s.streakStart, s.streakDayLabel(3),
            s.streakRun(3), s.streakKeep(s.streakRun(3)), s.streakBest(s.streakRun(3), 9),
            s.streakNextTheme(s.streakRun(3), 1, "Bamboo"), s.streakNextTheme(s.streakRun(3), 4, "Bamboo"),
            s.campaignNew(2027), s.campaignCast("Kūn"), s.campaignActionCast, s.campaignActionView,
        ))
    }

    @Test
    fun divination() {
        val s = EnDivinationStrings
        val lines = LineType.entries
        check("divination", listOf(
            s.titleCasting, s.titleReveal, s.titleSafety, s.titleReading, s.yearOpening, s.blankPrompt, s.shareCard,
            s.savedNote, s.backToCalm, s.unrolling, s.leaveTitle, s.leaveSaved, s.leaveUnsaved, s.stay, s.leave,
            s.tossHint, s.settling, s.toss, s.tossNext, s.tossRest, s.castProgress(3),
            s.listenPreparing, s.listenStop, s.listen, s.listenPremium, s.noVoiceTitle, s.noVoiceBody, s.openSettings,
            s.notNow, s.howToReadTitle, s.termPrimaryMeaning, s.termRelatingMeaning, s.noChangingLines, s.tapToReadFull,
            s.noticeQuotaGranted, s.noticeQuotaAdsDone, s.noticeQuotaDone, s.noticeRateLimited(3), s.noticeRateLimited(null),
            s.noticeAiDisabled, s.noticeFollowUpLimit, s.noticeNetwork, s.noticeBlocked, s.noticeUnauthenticated,
            s.noticeOther, s.noticeOffline, s.getGuidance, s.watchAdForGuidance(3), s.askAgain, s.followUpTitle,
            s.followUpTeaser(3), s.followUpPremiumOnly, s.seePlans, s.hermitThinking, s.followUpPlaceholder(3), s.send,
            s.followUpsUsed, s.reportTitle, s.reportSubmit, s.helplineLabel, s.helplineUri, s.emergencyLabel, s.emergencyUri, s.backHome,
            s.recordNotFound, s.yearHexagramMissing, s.hermitUnreachable, s.scriptureUnavailable, s.followUpLimitReached,
            s.followUpNeedsPremium, s.followUpNoNetwork, s.followUpFailed, s.reportThanks, s.reportFailed,
        ) + lines.map { s.lineGloss(it) } + lines.map { s.lineLabel(it) } +
            listOf("harmful", "offensive", "wrong", "other").map { s.reportReason(it) })
    }

    @Test
    fun reading() {
        val s = EnReadingStrings
        val cards = listOf("ดวงเด่น", "ควรระวัง", "คำแนะนำ").map { ReadingSection(it, "text") }
        check("reading", listOf(
            s.sheetScripture, s.sheetHermit, s.hermitStudying, s.unspokenQuestion, s.primaryLabel, s.relatingLabel,
            s.changesInto, s.aiDisclaimer, s.scriptureDisclaimer, s.reported, s.reportAction, s.scriptureTitle, s.judgment,
            s.image, s.allLinesChange, s.relatingHeading("Kūn"),
        ) + cards.map { s.sectionTitle(it) })
        assertEquals(listOf("Going well", "Watch out", "What to do"), cards.map { s.sectionTitle(it) })
    }

    @Test
    fun onboarding() {
        val s = EnOnboardingStrings
        check("onboarding", s.points.flatMap { listOf(it.first, it.second) } + listOf(
            s.skip, s.next, s.acceptAndStart, s.pageOf(1, 4), s.askTitle, s.askBody, s.castTitle, s.castBody, s.readTitle,
            s.readBody, s.readNote, s.askBlank, s.aboutCategory("work"), s.noQuestionNeeded, s.tossLabel, s.tryTapping,
            s.practiceDone, s.practiceProgress(3, "Solid line (yang)"), s.sampleName, s.sampleJudgment,
            s.disclaimerTitle, s.terms, s.privacy,
        ) + LineType.entries.map { s.practiceLineName(it) })
    }

    @Test
    fun englishCrisisActionsHaveNoThaiNumbers() {
        val s = EnDivinationStrings
        listOf(s.helplineLabel, s.helplineUri, s.emergencyLabel, s.emergencyUri).forEach {
            assertFalse(it.contains("1323") || it.contains("1669"), it)
        }
        assertFalse(EnOnboardingStrings.points.last().second.let { it.contains("1323") || it.contains("1669") })
        assertEquals("https://findahelpline.com", s.helplineUri)
    }

    @Test
    fun thaiCrisisActionsKeepTheirNumbers() {
        val s = ThDivinationStrings
        assertEquals("tel:1323", s.helplineUri)
        assertEquals("tel:1669", s.emergencyUri)
        assertEquals("โทรสายด่วนสุขภาพจิต 1323", s.helplineLabel)
        assertEquals("เหตุฉุกเฉินทางการแพทย์ 1669", s.emergencyLabel)
    }
}
