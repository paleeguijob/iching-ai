package com.iching.ai.ui.strings

import com.iching.ai.domain.model.AppLanguage

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
}
