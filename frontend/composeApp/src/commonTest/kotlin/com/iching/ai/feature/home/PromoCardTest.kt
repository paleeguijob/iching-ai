package com.iching.ai.feature.home

import com.iching.ai.domain.model.AccountStatus
import com.iching.ai.domain.model.PromoStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PromoCardTest {
    // 2026-11-21T00:00+07:00, exclusive: the last day the promo runs is 20 Nov.
    private val endsAt = 1795194000000L
    private val now = endsAt - 86_400_000
    private val promo = PromoStatus("1111-2026", "โปรพิเศษ", "Premium รายปี ลด ๔๐%", "ดูโปร", endsAt, showEndDate = true)
    private val free = AccountStatus.UNKNOWN_FREE.copy(promo = promo)
    private val shown = PromoCard("1111-2026", "โปรพิเศษ · ถึง ๒๐ พ.ย.", "Premium รายปี ลด ๔๐%", "ดูโปร ›")

    @Test
    fun showsTheCardForFree() = assertEquals(shown, promoCard(free, emptyList(), now))

    @Test
    fun endDateIsTheLastDayShown() = assertEquals("โปรพิเศษ · ถึง ๒๐ พ.ย.", promoCard(free, emptyList(), endsAt - 1)?.label)

    @Test
    fun noEndDateWhenTurnedOff() = assertEquals(
        "โปรพิเศษ",
        promoCard(free.copy(promo = promo.copy(showEndDate = false)), emptyList(), now)?.label,
    )

    @Test
    fun noPromoNoCard() = assertNull(promoCard(AccountStatus.UNKNOWN_FREE, emptyList(), now))

    @Test
    fun premiumHidesTheCard() = assertNull(promoCard(free.copy(premium = true), emptyList(), now))

    @Test
    fun dismissedHidesTheCard() = assertNull(promoCard(free, listOf("1111-2026"), now))

    @Test
    fun aNewIdShowsAfterTheOldWasDismissed() = assertEquals(shown, promoCard(free, listOf("1010-2026"), now))

    @Test
    fun pastTheEndOnThisDeviceHidesTheCard() = assertNull(promoCard(free, emptyList(), endsAt))
}
