package com.iching.ai.feature.home

import com.iching.ai.domain.model.AccountStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class GuidanceQuotaTest {
    private val free = AccountStatus.UNKNOWN_FREE
    private val premium = free.copy(premium = true, adsWatchableLeft = 0)

    @Test
    fun freeShowsReadingsLeft() = assertEquals(
        GuidanceQuota("วันนี้เหลือคำชี้แนะจากนักพรต", count = 3, note = null),
        guidanceQuota(free.copy(includedLeft = 1, readingsLeft = 3)),
    )

    @Test
    fun premiumShowsReadingsLeftToday() = assertEquals(
        GuidanceQuota("Premium · วันนี้เหลือคำชี้แนะจากนักพรต", count = 10, note = null),
        guidanceQuota(premium.copy(includedLeft = 10, readingsLeft = 10)),
    )

    @Test
    fun coinsAreNotCountedAsToday() = assertEquals(
        GuidanceQuota(
            "วันนี้เหลือคำชี้แนะจากนักพรต",
            count = 1,
            note = "และมีเหรียญอี้จิงอีก ๑๐ เหรียญ ใช้ได้ทุกวัน ไม่มีวันหมดอายุ",
        ),
        guidanceQuota(free.copy(coins = 10, includedLeft = 1, readingsLeft = 11)),
    )

    @Test
    fun premiumCoinsAreNotCountedAsToday() = assertEquals(
        GuidanceQuota(
            "Premium · วันนี้เหลือคำชี้แนะจากนักพรต",
            count = 3,
            note = "และมีเหรียญอี้จิงอีก ๑๐ เหรียญ ใช้ได้ทุกวัน ไม่มีวันหมดอายุ",
        ),
        guidanceQuota(premium.copy(coins = 10, includedLeft = 3, readingsLeft = 13)),
    )

    @Test
    fun freeOutOfTodayShowsCoinsInsteadOfAds() = assertEquals(
        GuidanceQuota("เหรียญอี้จิงใช้ทำนายได้อีก", count = 10, note = "ใช้ได้ทุกวัน ไม่มีวันหมดอายุ"),
        guidanceQuota(free.copy(coins = 10, includedLeft = 0, readingsLeft = 10, adsWatchableLeft = 2)),
    )

    @Test
    fun premiumOutOfTodayShowsCoinsInsteadOfZero() = assertEquals(
        GuidanceQuota("Premium · เหรียญอี้จิงใช้ทำนายได้อีก", count = 10, note = "ใช้ได้ทุกวัน ไม่มีวันหมดอายุ"),
        guidanceQuota(premium.copy(coins = 10, includedLeft = 0, readingsLeft = 10)),
    )

    @Test
    fun freeAtZeroShowsTheAdsLeftInsteadOfZero() = assertEquals(
        GuidanceQuota(
            "ชมโฆษณา รับคำชี้แนะเพิ่มได้อีก",
            count = 2,
            note = "กดเริ่มเสี่ยงทาย แล้วชมโฆษณาสั้นก่อนโยนเหรียญ",
        ),
        guidanceQuota(free.copy(includedLeft = 0, readingsLeft = 0, adsWatchableLeft = 2)),
    )

    @Test
    fun freeAtZeroStopsOfferingAdsOnceTheyAreUsedUp() = assertEquals(
        GuidanceQuota(
            "วันนี้เหลือคำชี้แนะจากนักพรต",
            count = 0,
            note = "สมัคร Premium หรือใช้เหรียญอี้จิงเพื่อรับคำชี้แนะต่อ · ยังเสี่ยงทายและอ่านตัวบทได้",
        ),
        guidanceQuota(free.copy(includedLeft = 0, readingsLeft = 0, adsWatchableLeft = 0)),
    )

    @Test
    fun premiumAtZeroNeverOffersAnAd() = assertEquals(
        GuidanceQuota("Premium · วันนี้เหลือคำชี้แนะจากนักพรต", count = 0, note = "ยังเสี่ยงทายและอ่านตัวบทได้"),
        guidanceQuota(premium.copy(includedLeft = 0, readingsLeft = 0, adsWatchableLeft = 2)),
    )

    @Test
    fun aiOffHidesTheCount() = assertEquals(
        GuidanceQuota("นักพรตปลีกวิเวกชั่วคราว — ยังเสี่ยงทายและอ่านตัวบทคัมภีร์ได้", count = null, note = null),
        guidanceQuota(premium.copy(includedLeft = 10, aiEnabled = false)),
    )
}
