package com.iching.ai.feature.poster

import com.iching.ai.domain.model.AccountStatus
import com.iching.ai.domain.model.PosterCardText
import com.iching.ai.domain.model.PosterLink
import com.iching.ai.domain.model.PosterStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PosterCardTest {
    // 2026-11-21T00:00+07:00, exclusive: the last day the promo runs is 20 Nov.
    private val endsAt = 1795194000000L
    private val now = endsAt - 86_400_000
    private val poster = PosterStatus("1111-2026", "https://w/content/posters/a.webp", "โปร", true, PosterLink.PAYWALL, PosterCardText("โปรพิเศษ", "Premium รายปี ลด ๔๐%", "ดูโปร", true), endsAt)
    private val free = AccountStatus.UNKNOWN_FREE.copy(poster = poster)
    private val shown = PosterCard("1111-2026", "โปรพิเศษ · ถึง ๒๐ พ.ย.", "Premium รายปี ลด ๔๐%", "ดูโปร ›", PosterLink.PAYWALL)

    @Test
    fun showsTheCardForFree() = assertEquals(shown, posterCard(free, emptyList(), now))

    @Test
    fun endDateIsTheLastDayShown() = assertEquals("โปรพิเศษ · ถึง ๒๐ พ.ย.", posterCard(free, emptyList(), endsAt - 1)?.label)

    @Test
    fun noEndDateWhenTurnedOff() = assertEquals(
        "โปรพิเศษ",
        posterCard(free.copy(poster = poster.copy(card = poster.card!!.copy(showEndDate = false))), emptyList(), now)?.label,
    )

    @Test
    fun noPromoNoCard() = assertNull(posterCard(AccountStatus.UNKNOWN_FREE, emptyList(), now))

    @Test
    fun premiumHidesFreeCard() = assertNull(posterCard(free.copy(premium = true), emptyList(), now))

    @Test
    fun dismissedHidesTheCard() = assertNull(posterCard(free, listOf("1111-2026"), now))

    @Test
    fun aNewIdShowsAfterTheOldWasDismissed() = assertEquals(shown, posterCard(free, listOf("1010-2026"), now))

    @Test
    fun pastTheEndOnThisDeviceHidesTheCard() = assertNull(posterCard(free, emptyList(), endsAt))

    @Test
    fun premiumSeesAllAudienceCard() =
        assertEquals(shown, posterCard(free.copy(premium = true, poster = poster.copy(freeOnly = false)), emptyList(), now))

    @Test
    fun noCardTextNoCard() = assertNull(posterCard(free.copy(poster = poster.copy(card = null)), emptyList(), now))

    @Test
    fun dailyLinkHidesCard() =
        assertNull(posterCard(free.copy(poster = poster.copy(link = PosterLink.DAILY)), emptyList(), now))

    @Test
    fun cardCarriesLink() =
        assertEquals(PosterLink.YEAR, posterCard(free.copy(poster = poster.copy(link = PosterLink.YEAR)), emptyList(), now)?.link)
}
