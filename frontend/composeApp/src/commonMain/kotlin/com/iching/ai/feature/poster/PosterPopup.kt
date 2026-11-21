package com.iching.ai.feature.poster

import com.iching.ai.domain.model.AccountStatus
import com.iching.ai.domain.model.AppSettings
import com.iching.ai.domain.model.PosterLink

/** The artwork popup for home; tapping goes to [link], or nowhere when null. */
data class PosterPopup(val id: String, val imageUrl: String, val alt: String, val link: PosterLink?)

/**
 * The popup to show now, or null. It pops once per poster, never on the day the app was installed, only after
 * today's daily hexagram was read (so the seeker already has what they came for), and only once its image is
 * ready, so a slow network never shows an empty frame. Premium and the device clock are re-checked because the
 * cached status can lag.
 */
fun posterPopup(status: AccountStatus, settings: AppSettings, today: Long, nowMillis: Long, readyImages: Set<String>): PosterPopup? {
    val poster = status.poster ?: return null
    if (poster.id in settings.seenPosterIds) return null
    if (nowMillis >= poster.endsAtMillis || (poster.freeOnly && status.premium)) return null
    val firstOpen = settings.firstOpenEpochDay ?: return null
    if (firstOpen >= today || settings.streak.lastEpochDay != today) return null
    if (poster.imageUrl !in readyImages) return null
    return PosterPopup(poster.id, poster.imageUrl, poster.alt, poster.link)
}
