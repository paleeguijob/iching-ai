package com.iching.ai.feature.divination

import com.iching.ai.domain.model.AccountStatus
import com.iching.ai.domain.repository.FailureReason
import com.iching.ai.domain.repository.LimitReason
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QuotaUpsellTest {
    private val quota = ReadingNotice.Limited(LimitReason.QUOTA, retryAfterSeconds = null)
    private val spent = AccountStatus.UNKNOWN_FREE.copy(includedLeft = 0, readingsLeft = 0, adsWatchableLeft = 0, coins = 0)

    @Test
    fun freeWithFreeAndAdsUsedUpIsOffered() = assertTrue(offersQuotaUpsell(quota, spent, canRetry = true))

    @Test
    fun anAdThatCanStillUnlockThisCastComesFirst() =
        assertFalse(offersQuotaUpsell(quota, spent.copy(adsWatchableLeft = 1), canRetry = true))

    @Test
    fun anAdThatCannotUnlockThisCastDoesNotHoldTheOfferBack() =
        assertTrue(offersQuotaUpsell(quota, spent.copy(adsWatchableLeft = 1), canRetry = false))

    @Test
    fun coinsLeftAreNotOffered() =
        assertFalse(offersQuotaUpsell(quota, spent.copy(coins = 3, readingsLeft = 3), canRetry = true))

    @Test
    fun premiumIsNeverOffered() =
        assertFalse(offersQuotaUpsell(quota, spent.copy(premium = true), canRetry = true))

    @Test
    fun aiOffIsNotOffered() =
        assertFalse(offersQuotaUpsell(quota, spent.copy(aiEnabled = false), canRetry = true))

    @Test
    fun otherNoticesAreNotOffered() {
        assertFalse(offersQuotaUpsell(ReadingNotice.Limited(LimitReason.RATE_LIMITED, 10), spent, canRetry = true))
        assertFalse(offersQuotaUpsell(ReadingNotice.Failed(FailureReason.NETWORK), spent, canRetry = true))
        assertFalse(offersQuotaUpsell(ReadingNotice.Offline, spent, canRetry = true))
        assertFalse(offersQuotaUpsell(null, spent, canRetry = true))
    }
}
