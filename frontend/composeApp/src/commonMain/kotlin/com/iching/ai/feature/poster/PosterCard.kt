package com.iching.ai.feature.poster

import com.iching.ai.domain.model.AccountStatus
import com.iching.ai.domain.model.PosterLink
import com.iching.ai.ui.formatThaiShortDate
import kotlinx.datetime.TimeZone

/** The poster's small card for the end of the daily page, worded and ready to draw; tapping goes to [link]. */
data class PosterCard(val id: String, val label: String, val title: String, val cta: String, val link: PosterLink)

private val BANGKOK = TimeZone.of("Asia/Bangkok")

/**
 * The card to show, or null. Re-checks what the server already filtered, since the cached status can lag:
 * a fresh Premium purchase, or this device's clock passing the end while the page stays open. A card that would
 * link to the daily page itself is left out.
 */
fun posterCard(status: AccountStatus, dismissed: List<String>, nowMillis: Long): PosterCard? {
    val poster = status.poster ?: return null
    val card = poster.card ?: return null
    val link = poster.link?.takeIf { it != PosterLink.DAILY } ?: return null
    if ((poster.freeOnly && status.premium) || nowMillis >= poster.endsAtMillis || poster.id in dismissed) return null
    // The end is exclusive, so a poster ending at midnight shows the day before as its last.
    val label = if (card.showEndDate) "${card.label} · ถึง ${formatThaiShortDate(poster.endsAtMillis - 1, BANGKOK)}" else card.label
    return PosterCard(poster.id, label, card.title, "${card.cta} ›", link)
}
