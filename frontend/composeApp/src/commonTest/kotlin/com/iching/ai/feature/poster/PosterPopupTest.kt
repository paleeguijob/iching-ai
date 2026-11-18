package com.iching.ai.feature.poster

import com.iching.ai.domain.model.AccountStatus
import com.iching.ai.domain.model.AppSettings
import com.iching.ai.domain.model.PosterLink
import com.iching.ai.domain.model.PosterStatus
import com.iching.ai.domain.model.StreakState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PosterPopupTest {
    private val today = 20_400L
    private val endsAt = 1795194000000L
    private val now = endsAt - 86_400_000
    private val url = "https://w/content/posters/cny.webp"
    private val poster = PosterStatus("cny", url, "ตรุษจีน", true, PosterLink.PAYWALL, null, endsAt)
    private val status = AccountStatus.UNKNOWN_FREE.copy(poster = poster)
    private val settings = AppSettings(firstOpenEpochDay = today - 3, streak = StreakState(current = 1, lastEpochDay = today))
    private val ready = setOf(url)
    private val shown = PosterPopup("cny", url, "ตรุษจีน", PosterLink.PAYWALL)

    @Test
    fun allConditionsShowPopup() = assertEquals(shown, posterPopup(status, settings, today, now, ready))

    @Test
    fun noPoster() = assertNull(posterPopup(AccountStatus.UNKNOWN_FREE, settings, today, now, ready))

    @Test
    fun alreadySeen() = assertNull(posterPopup(status, settings.copy(seenPosterIds = listOf("cny")), today, now, ready))

    @Test
    fun expiredOnDevice() = assertNull(posterPopup(status, settings, today, endsAt, ready))

    @Test
    fun premiumNeverGetsFreePopup() = assertNull(posterPopup(status.copy(premium = true), settings, today, now, ready))

    @Test
    fun premiumGetsAllAudiencePopup() = assertEquals(
        shown,
        posterPopup(status.copy(premium = true, poster = poster.copy(freeOnly = false)), settings, today, now, ready),
    )

    @Test
    fun firstOpenUnknown() = assertNull(posterPopup(status, settings.copy(firstOpenEpochDay = null), today, now, ready))

    @Test
    fun installDay() = assertNull(posterPopup(status, settings.copy(firstOpenEpochDay = today), today, now, ready))

    @Test
    fun notReadToday() =
        assertNull(posterPopup(status, settings.copy(streak = StreakState(current = 1, lastEpochDay = today - 1)), today, now, ready))

    @Test
    fun imageNotReady() = assertNull(posterPopup(status, settings, today, now, emptySet()))

    @Test
    fun popupWithoutLinkStillShows() =
        assertEquals(shown.copy(link = null), posterPopup(status.copy(poster = poster.copy(link = null)), settings, today, now, ready))
}
