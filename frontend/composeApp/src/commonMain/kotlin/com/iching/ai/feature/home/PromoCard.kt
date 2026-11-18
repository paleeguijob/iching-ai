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
    val promo = status.promo ?: return null
    if (status.premium || nowMillis >= promo.endsAtMillis || promo.id in dismissed) return null
    // The end is exclusive, so a promo ending at midnight shows the day before as its last.
    val label = if (promo.showEndDate) "${promo.label} · ถึง ${formatThaiShortDate(promo.endsAtMillis - 1, BANGKOK)}" else promo.label
    return PromoCard(promo.id, label, promo.title, "${promo.cta} ›")
}
