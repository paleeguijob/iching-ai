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
    fun premiumShowsIncludedLeft() = assertEquals(
        GuidanceQuota("Premium · วันนี้เหลือคำชี้แนะจากนักพรต", count = 10, note = null),
        guidanceQuota(premium.copy(includedLeft = 10, readingsLeft = 12)),
    )

    @Test
    fun freeAtZeroOffersAnAdWhileOneCanStillBeWatched() = assertEquals(
        GuidanceQuota(
            "วันนี้เหลือคำชี้แนะจากนักพรต",
            count = 0,
            note = "ยังเสี่ยงทายและอ่านตัวบทได้ หรือชมโฆษณาเพื่อรับเพิ่ม",
        ),
        guidanceQuota(free.copy(includedLeft = 0, readingsLeft = 0, adsWatchableLeft = 1)),
    )

    @Test
    fun freeAtZeroStopsOfferingAdsOnceTheyAreUsedUp() = assertEquals(
        GuidanceQuota("วันนี้เหลือคำชี้แนะจากนักพรต", count = 0, note = "ยังเสี่ยงทายและอ่านตัวบทได้"),
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
