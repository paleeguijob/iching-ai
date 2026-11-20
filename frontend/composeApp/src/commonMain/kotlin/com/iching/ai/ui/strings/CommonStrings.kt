package com.iching.ai.ui.strings

import com.iching.ai.domain.model.AppLanguage
import com.iching.ai.domain.model.AppTheme
import com.iching.ai.domain.model.QuestionCategory
import com.iching.ai.domain.model.Trigram
import com.iching.ai.domain.siamsi.SiamSiLevel
import com.iching.ai.domain.siamsi.SiamSiTopic

/** Generic button and label text shared by many screens. */
interface CommonStrings {
    val cancel: String
    val close: String
    val gotIt: String
    val back: String

    /** A whole number as the language writes it (Thai numerals in Thai). */
    fun number(n: Int): String

    val premiumDaily: String
    val premiumDailyNoAds: String
    val coinPack: String
    val working: String
    val openingAd: String
    val comeBackTomorrow: String
    val adMustFinish: String
    val adLimitReached: String
    val adUnavailable: String
    val paymentFailed: String

    // Labels for domain enums. The domain keeps its Thai fields (the server and tests use them); the UI reads these.
    fun category(c: QuestionCategory): String

    /** The example question a category chip fills into the question box. */
    fun categorySample(c: QuestionCategory): String
    fun level(l: SiamSiLevel): String
    fun topic(t: SiamSiTopic): String
    fun theme(t: AppTheme): String
    fun trigramNature(t: Trigram): String
}

object ThCommonStrings : CommonStrings {
    override val cancel = "ยกเลิก"
    override val close = "ปิด"
    override val gotIt = "เข้าใจแล้ว"
    override val back = "กลับ"
    override fun number(n: Int) = formatNumber(n, AppLanguage.TH)
    override val premiumDaily = "Premium · วันละ ๕ คำทำนาย"
    override val premiumDailyNoAds = "Premium · วันละ ๕ คำทำนาย ไม่มีโฆษณา"
    override val coinPack = "เหรียญอี้จิง ๑๐ เหรียญ"
    override val working = "กำลังดำเนินการ..."
    override val openingAd = "กำลังเปิดโฆษณา..."
    override val comeBackTomorrow = "หรือกลับมาใหม่พรุ่งนี้ สิทธิ์ชมโฆษณาเริ่มนับใหม่ทุกวัน"
    override val adMustFinish = "ต้องชมโฆษณาจนจบจึงจะได้รับสิทธิ์"
    override val adLimitReached = "วันนี้ใช้สิทธิ์ชมโฆษณาครบแล้ว"
    override val adUnavailable = "ยังไม่มีโฆษณาพร้อมแสดง โปรดลองใหม่อีกครู่"
    override val paymentFailed = "การชำระเงินไม่สำเร็จ โปรดลองใหม่อีกครั้ง"
    override fun category(c: QuestionCategory) = c.labelTh
    override fun categorySample(c: QuestionCategory) = c.sampleTh
    override fun level(l: SiamSiLevel) = l.labelTh
    override fun topic(t: SiamSiTopic) = t.labelTh
    override fun theme(t: AppTheme) = t.labelTh
    override fun trigramNature(t: Trigram) = t.natureTh
}

object EnCommonStrings : CommonStrings {
    override val cancel = "Cancel"
    override val close = "Close"
    override val gotIt = "Got it"
    override val back = "Back"
    override fun number(n: Int) = formatNumber(n, AppLanguage.EN)
    override val premiumDaily = "Premium · 5 readings a day"
    override val premiumDailyNoAds = "Premium · 5 readings a day, no ads"
    override val coinPack = "10 I Ching coins"
    override val working = "Working on it..."
    override val openingAd = "Opening the ad..."
    override val comeBackTomorrow = "Or come back tomorrow. Ad views reset every day."
    override val adMustFinish = "Watch the ad to the end to earn the reading."
    override val adLimitReached = "You have used all of today's ad views."
    override val adUnavailable = "No ad is ready right now. Please try again shortly."
    override val paymentFailed = "The payment did not go through. Please try again."

    override fun category(c: QuestionCategory) = when (c) {
        QuestionCategory.WORK -> "Work"
        QuestionCategory.LOVE -> "Love"
        QuestionCategory.MONEY -> "Money"
        QuestionCategory.DECISION -> "A decision"
        QuestionCategory.MIND -> "Peace of mind"
        QuestionCategory.NEWYEAR -> "New Year"
    }

    override fun categorySample(c: QuestionCategory) = when (c) {
        QuestionCategory.WORK -> "How should my work go in the time ahead?"
        QuestionCategory.LOVE -> "Which way is my relationship heading?"
        QuestionCategory.MONEY -> "How should I handle my money right now?"
        QuestionCategory.DECISION -> "How should I decide the matter weighing on me?"
        QuestionCategory.MIND -> "How can I find calm within?"
        QuestionCategory.NEWYEAR -> "Where should I aim my work this year?"
    }

    override fun level(l: SiamSiLevel) = when (l) {
        SiamSiLevel.EXCELLENT -> "Excellent"
        SiamSiLevel.GOOD -> "Good"
        SiamSiLevel.FAIR -> "Fair"
        SiamSiLevel.CAUTION -> "Caution"
    }

    override fun topic(t: SiamSiTopic) = when (t) {
        SiamSiTopic.WORK -> "Work"
        SiamSiTopic.MONEY -> "Money"
        SiamSiTopic.LOVE -> "Love"
        SiamSiTopic.HEALTH -> "Health"
        SiamSiTopic.TRAVEL -> "Travel"
        SiamSiTopic.LOST -> "Lost items"
        SiamSiTopic.DEALINGS -> "Legal and dealings"
    }

    override fun theme(t: AppTheme) = when (t) {
        AppTheme.INK_PAPER -> "Ink Paper"
        AppTheme.BAMBOO -> "Green Bamboo"
        AppTheme.CINNABAR_DAWN -> "Cinnabar Dawn"
        AppTheme.INK_NIGHT -> "Ink Night"
        AppTheme.CINNABAR_GOLD -> "Cinnabar Gold"
    }

    override fun trigramNature(t: Trigram) = when (t) {
        Trigram.QIAN -> "Heaven"
        Trigram.DUI -> "Lake"
        Trigram.LI -> "Fire"
        Trigram.ZHEN -> "Thunder"
        Trigram.XUN -> "Wind"
        Trigram.KAN -> "Water"
        Trigram.GEN -> "Mountain"
        Trigram.KUN -> "Earth"
    }
}
