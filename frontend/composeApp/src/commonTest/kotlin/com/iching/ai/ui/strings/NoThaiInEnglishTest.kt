package com.iching.ai.ui.strings

import com.iching.ai.domain.model.AppTheme
import com.iching.ai.domain.model.QuestionCategory
import com.iching.ai.domain.model.Trigram
import com.iching.ai.domain.siamsi.SiamSiLevel
import com.iching.ai.domain.siamsi.SiamSiTopic
import com.iching.ai.domain.model.AppLanguage
import com.iching.ai.domain.model.LineType
import com.iching.ai.domain.model.ProductKind
import com.iching.ai.domain.model.StoreProduct
import com.iching.ai.domain.oracle.ReadingSection
import com.iching.ai.domain.siamsi.PuaResult
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
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
        ) + QuestionCategory.entries.flatMap { listOf(s.category(it), s.categorySample(it)) } +
            SiamSiLevel.entries.map(s::level) + SiamSiTopic.entries.map(s::topic) +
            AppTheme.entries.map(s::theme) + Trigram.entries.map(s::trigramNature))
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
    fun journal() {
        val s = EnJournalStrings
        check("journal", listOf(
            s.title, s.subtitle(null, 3), s.subtitle(7, 3), s.subtitle(1, 1), s.filterAll, s.filterIching, s.filterSiamsi,
            s.hiddenTitle(3), s.hiddenTitle(1), s.hiddenBody, s.seePlans, s.yearReading, s.siamsiTag, s.scriptureTag,
            s.prayedInHeart, s.emptyTitle, s.emptyBody, s.emptyAction, s.deleteDescription, s.deleteTitle, s.deleteBody,
            s.delete, s.shareSlip, s.setMissing, s.outcomeQuestion, s.outcomeYes, s.outcomePartly, s.outcomeNo,
            s.notePlaceholder, s.save, s.askHermitBody, s.askHermitAction,
            s.siamsiRow("Guanyin Oracle", 17, null, "Good", true), s.siamsiRow("Guanyin Oracle", 3, "丙寅", null, false),
        ))
    }

    @Test
    fun library() {
        val s = EnLibraryStrings
        check("library", listOf(
            s.title, s.subtitle, s.clear, s.searchPlaceholder, s.filterAll, s.noResults, s.noResultsFor("water"),
            s.hexagramNumber(17), s.upperTrigram, s.lowerTrigram, s.overview, s.sixLines, s.whenAllLinesChange,
        ) + (1..6).map { s.linePosition(it) })
    }

    @Test
    fun settings() {
        val s = EnSettingsStrings
        val until = 1795194000000L
        check("settings", listOf(
            s.pageMain, s.pageAccount, s.pageFeedback, s.pageAbout, s.pushBlockedTitle, s.pushBlockedBody, s.planPremium,
            s.planFree, s.readingsLeftToday(3), s.readingsLeftToday(1), s.coinsNote(10), s.until(until), s.details, s.upgrade,
            s.sound, s.dailyPush, s.dailyPushSubtitle, s.yearRow(2027), s.theme, s.streak(3, 9), s.streak(1, 1),
            s.lockedStreak(7), s.lockedStreak(1), s.lockedCampaign, s.lockedSpecial, s.lockedSuffix, s.linkedApple,
            s.linkedGoogle, s.notLinked, s.localAccount, s.accountId("abcd1234"), s.linkHint, s.signInApple, s.signInGoogle,
            s.syncJournal, s.sectionAccount, s.sectionPurchases, s.restore, s.manageSubscriptions, s.deleteAccount,
            s.shareApp, s.shareAppSubtitle, s.shareAppText("https://example.com"), s.sendFeedback, s.sendFeedbackSubtitle,
            s.aboutBody, s.howToCast, s.privacy, s.terms, s.version("2.0.0"), s.deleteTitle, s.deleteBody, s.deleteConfirm,
            s.feedbackThanks, s.feedbackFailed, s.feedbackLimit, s.linkedCarriedOver, s.linkedPartial, s.linkFailed,
            s.restoreFailed, s.deleteFailed, s.syncAdded(3), s.syncAdded(1), s.syncUpToDate, s.syncPremiumOnly, s.syncFailed,
            s.feedbackTitle, s.feedbackSubtitle, s.feedbackPlaceholder, s.feedbackCounter(12, 1000), s.feedbackSending,
        ))
    }

    @Test
    fun paywall() {
        val s = EnPaywallStrings
        val yearly = StoreProduct("y", "iching_yearly", ProductKind.YEARLY, "", "\$3.99", "ปี", "ทดลองฟรี ๗ วัน", 7)
        val monthly = StoreProduct("m", "iching_monthly", ProductKind.MONTHLY, "", "\$0.99", "เดือน", null)
        val coins = StoreProduct("c", "iching_coins_10", ProductKind.COINS, "", "\$0.99", null, null)
        check("paywall", listOf(
            s.pending, s.awaitingPayment, s.coinsReceived, s.welcomePremium, s.restoredPremium, s.restoredCoins(10),
            s.restoreNothing, s.restoreFailed, s.productsFailed, s.title, s.tagline, s.memberLine(null), s.memberLine(1795194000000L),
            s.reload, s.coinsHeading, s.coinsNote(10), s.restore, s.manage, s.terms, s.privacy, s.planYearly, s.planMonthly,
            s.bestValue, s.startTrial(s.trialLabel(yearly)!!), s.subscribe(monthly), s.subscribe(coins),
            s.pricePerPeriod(yearly), s.pricePerPeriod(coins), s.renewalTerms(yearly), s.renewalTerms(monthly), s.renewalTerms(coins),
        ) + s.benefits)
        assertNull(s.trialLabel(monthly))
        assertEquals("7-day free trial", s.trialLabel(yearly))
        assertEquals("ทดลองฟรี ๗ วัน", ThPaywallStrings.trialLabel(yearly))
        assertEquals("ทดลองฟรี ๗ วัน จากนั้น เรียกเก็บ \$3.99 ทุกปี ต่ออายุอัตโนมัติจนกว่าจะยกเลิก ยกเลิกได้ทุกเมื่อในการตั้งค่าบัญชีของร้านค้า อย่างน้อย ๒๔ ชั่วโมงก่อนรอบบิลถัดไป", ThPaywallStrings.renewalTerms(yearly))
    }

    @Test
    fun review() {
        val s = EnReviewStrings
        check("review", listOf(s.rateApp, s.rateAppSubtitle))
    }

    @Test
    fun englishSettingsNameNoPushHour() {
        val text = listOf(EnSettingsStrings.dailyPush, EnSettingsStrings.dailyPushSubtitle).joinToString(" ")
        assertFalse(Regex("\\d").containsMatchIn(text), text)
        assertFalse(text.contains("Bangkok", ignoreCase = true), text)
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
