package com.iching.ai.ui.strings

import com.iching.ai.domain.model.AppLanguage
import com.iching.ai.domain.model.LineType
import com.iching.ai.domain.oracle.ReadingSection
import com.iching.ai.domain.siamsi.PuaResult
import kotlinx.datetime.TimeZone
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
    fun siamsi() {
        val s = EnSiamSiStrings
        val results = PuaResult.entries
        check("siamsi", listOf(
            s.gateLeft(3), s.gateLimit, s.gateAdWatched, s.gateNoAd, s.gateAdDismissed, s.gateWatchAd(3),
            s.setRow("Guanyin", 100), s.setSheetTitle, s.setSheetSubtitle, s.setSource("Guanyin Oracle", 100),
            s.titleSiamsi, s.titleSlip, s.titlePua, s.shareSlipDescription, s.stickLabel(17), s.confirmByPua, s.tossPua,
            s.shakeAgain, s.openSlip, s.leaveDrawTitle, s.leaveDrawBody, s.leaveInterpretTitle, s.leaveInterpretBody,
            s.leaveResumableBody, s.stay, s.leave, s.prayInHeart, s.holdToShake, s.shakeHint, s.cylinderDescription,
            s.stickDrawn, s.stickDrawnDescription(17), s.stickTitle(17), s.tossPrompt, s.puaRules, s.blockUp, s.blockDown,
            s.tossLabel, s.blocksDescription, s.reflection, s.slipHeader("Guanyin", 17), s.askMore, s.shareSlip,
            s.sourceLine("Guanyin Oracle"), s.storyTitle, s.storyHide, s.storyShow, s.askHermit, s.costOne, s.costLeft(3),
            s.interpret, s.hermitInterpretation, s.hermitConsidering, s.aiLabel, s.offlineInterpret,
            s.limitRateLimited(3), s.limitRateLimited(null), s.limitAiDisabled, s.limitDone, s.failNetwork, s.failBlocked,
            s.failUnauthenticated, s.failOther, s.followUpLimit,
        ) + results.map { s.puaLabel(it) } +
            results.flatMap { r -> listOf(false, true).flatMap { listOf(s.puaHeadline(r, it).first, s.puaHeadline(r, it).second) } })
    }

    @Test
    fun share() {
        val s = EnShareStrings
        check("share", listOf(
            s.shareText, s.yearCardHeader(zodiacYearLabel(2027, AppLanguage.EN)), s.number(17), s.sourceLine("Guanyin Oracle"),
            s.slipLabel, s.brand, s.brandSiamsi, s.aiLabel, s.failed, s.close, s.share,
        ))
    }

    @Test
    fun daily() {
        val s = EnDailyStrings
        check("daily", listOf(
            s.title, s.streakLine(3), s.judgment, s.shareToday, s.readMore, s.unlockTitle("Bamboo"), s.unlockBody(7),
            s.acknowledge, s.pushTitle, s.pushBody, s.pushAccept, s.pushLater,
        ))
    }

    @Test
    fun year() {
        val s = EnYearStrings
        check("year", listOf(
            s.heading(zodiacYearLabel(2027, AppLanguage.EN)), s.ritualTitle, s.ritualPrompt, s.loading, s.offline,
            s.rateLimited, s.inactive, s.retry, s.themeNotice, s.work, s.love, s.money, s.share, s.askHermit,
            s.readScripture, s.footnote,
        ))
    }

    @Test
    fun poster() {
        val s = EnPosterStrings
        check("poster", listOf(
            s.labelWithEnd("Special offer", 1795194000000L - 1, TimeZone.of("Asia/Bangkok")), s.closePopup, s.closeCard,
        ))
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
