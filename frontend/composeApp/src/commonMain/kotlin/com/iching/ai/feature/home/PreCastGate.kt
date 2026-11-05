package com.iching.ai.feature.home

import com.iching.ai.domain.model.AccountStatus

/** What tapping "เริ่มเสี่ยงทาย" offers before the coins are tossed. */
enum class PreCastOffer { NONE, AD, UPSELL }

/**
 * Casting goes straight ahead unless a free seeker has nothing left that pays for the hermit's words:
 * then the ad is offered while one can still be watched, Premium or coins once none can.
 * Unsure ([online] false: the server has not answered yet), the cast goes ahead and the card after the
 * cast offers the same choices.
 */
fun preCastOffer(status: AccountStatus, online: Boolean): PreCastOffer = when {
    !online -> PreCastOffer.NONE
    !status.aiEnabled -> PreCastOffer.NONE
    status.premium -> PreCastOffer.NONE
    status.readingsLeft > 0 -> PreCastOffer.NONE
    status.adsWatchableLeft > 0 -> PreCastOffer.AD
    else -> PreCastOffer.UPSELL
}
