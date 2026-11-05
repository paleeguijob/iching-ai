package com.iching.ai.feature.home

import com.iching.ai.domain.model.AccountStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class PreCastGateTest {
    /** A free seeker who has used today's free reading but can still watch two ads. */
    private val freeOut = AccountStatus.UNKNOWN_FREE.copy(includedLeft = 0, readingsLeft = 0, adsWatchableLeft = 2)

    @Test
    fun offlineCastsStraightAway() = assertEquals(PreCastOffer.NONE, preCastOffer(freeOut, online = false))

    @Test
    fun aiOffCastsStraightAway() = assertEquals(PreCastOffer.NONE, preCastOffer(freeOut.copy(aiEnabled = false), online = true))

    @Test
    fun premiumOutOfReadingsCastsStraightAway() = assertEquals(
        PreCastOffer.NONE,
        preCastOffer(freeOut.copy(premium = true, readingsLeft = 0, adsWatchableLeft = 0), online = true),
    )

    @Test
    fun readingsLeftCastsStraightAway() = assertEquals(PreCastOffer.NONE, preCastOffer(freeOut.copy(readingsLeft = 1), online = true))

    @Test
    fun coinsCastStraightAway() = assertEquals(
        PreCastOffer.NONE,
        preCastOffer(freeOut.copy(coins = 3, readingsLeft = 3, adsWatchableLeft = 0), online = true),
    )

    @Test
    fun freeOutWithAdsOffersTheAd() = assertEquals(PreCastOffer.AD, preCastOffer(freeOut, online = true))

    @Test
    fun freeWithNothingLeftOffersUpsell() = assertEquals(PreCastOffer.UPSELL, preCastOffer(freeOut.copy(adsWatchableLeft = 0), online = true))
}
