package com.iching.ai.ui.strings

import com.iching.ai.domain.model.AppLanguage
import kotlinx.datetime.TimeZone

/** The fixed text around a server poster; the poster's own words come from the server and are not here. */
interface PosterStrings {
    /** The card's label with the last day appended, e.g. "Offer · until 20 Nov 2026". */
    fun labelWithEnd(label: String, lastDayMillis: Long, timeZone: TimeZone): String
    val closePopup: String
    val closeCard: String
}

object ThPosterStrings : PosterStrings {
    override fun labelWithEnd(label: String, lastDayMillis: Long, timeZone: TimeZone) =
        "$label · ถึง ${formatShortDate(lastDayMillis, AppLanguage.TH, timeZone)}"
    override val closePopup = "ปิดประกาศนี้"
    override val closeCard = "ปิดโปรนี้"
}

object EnPosterStrings : PosterStrings {
    override fun labelWithEnd(label: String, lastDayMillis: Long, timeZone: TimeZone) =
        "$label · until ${formatShortDate(lastDayMillis, AppLanguage.EN, timeZone)}"
    override val closePopup = "Close this notice"
    override val closeCard = "Close this offer"
}
