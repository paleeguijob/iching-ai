package com.iching.ai.feature.home

import com.iching.ai.domain.model.AccountStatus
import com.iching.ai.ui.formatThaiShortDate
import kotlinx.datetime.TimeZone

/** The home promo card, worded and ready to draw. */
data class PromoCard(val id: String, val label: String, val title: String, val cta: String)

private val BANGKOK = TimeZone.of("Asia/Bangkok")

/**
 * The promo card to show, or null. Re-checks what the server already filtered, since the cached status can lag:
 * a fresh Premium purchase, or this device's clock passing the end while home stays open.
 */
fun promoCard(status: AccountStatus, dismissed: List<String>, nowMillis: Long): PromoCard? {
    val poster = status.poster ?: return null
    val card = poster.card ?: return null
    if ((poster.freeOnly && status.premium) || nowMillis >= poster.endsAtMillis || poster.id in dismissed) return null
    // The end is exclusive, so a poster ending at midnight shows the day before as its last.
    val label = if (card.showEndDate) "${card.label} · ถึง ${formatThaiShortDate(poster.endsAtMillis - 1, BANGKOK)}" else card.label
    return PromoCard(poster.id, label, card.title, "${card.cta} ›")
}
