package com.iching.ai.feature.share

import com.iching.ai.domain.model.Hexagram
import com.iching.ai.domain.oracle.stripMarkup
import com.iching.ai.domain.siamsi.SiamSiSet
import com.iching.ai.domain.siamsi.SiamSiStick
import com.iching.ai.ui.toThaiNumerals

const val SHARE_TEXT = "เสี่ยงทายกับคัมภีร์อี้จิงได้ที่ https://play.google.com/store/apps/details?id=com.iching.ai"

/** The look of a card: the sepia oracle card, or the cinnabar-and-gold one of the new-year campaign. */
enum class CardStyle { CLASSIC, NEW_YEAR }

/** What goes on a share card. There is deliberately no field for the seeker's question: it stays private. */
data class ShareCardContent(
    val hexagram: Hexagram,
    val body: String,
    val judgment: String?,
    val aiLabel: Boolean,
    /** Analytics value: "daily" or "reading". */
    val source: String,
    val style: CardStyle = CardStyle.CLASSIC,
    /** Header line of the year card; null on other cards. */
    val yearLabel: String? = null,
)

fun dailyShareContent(hexagram: Hexagram) = ShareCardContent(
    hexagram = hexagram,
    body = hexagram.dailyMessage,
    judgment = null,
    aiLabel = false,
    source = "daily",
)

/**
 * [summary] is the model's one-line summary. Without one the card carries the scripture alone, and no
 * AI label: a reading is never cut down to fit, because the cut-off text says nothing to a friend.
 */
fun readingShareContent(hexagram: Hexagram, summary: String?, newYear: Boolean = false): ShareCardContent {
    val cleaned = summary?.let(::stripMarkup).orEmpty()
    val style = if (newYear) CardStyle.NEW_YEAR else CardStyle.CLASSIC
    return if (cleaned.isEmpty()) {
        ShareCardContent(hexagram, hexagram.judgment, judgment = null, aiLabel = false, source = "reading", style = style)
    } else {
        ShareCardContent(
            hexagram = hexagram,
            body = cleaned,
            judgment = hexagram.judgment,
            aiLabel = true,
            source = "reading",
            style = style,
        )
    }
}

/** The new-year card: the year hexagram's blessing. Pre-written, so no AI label, and no question. */
fun yearShareContent(hexagram: Hexagram, yearLabel: String) = ShareCardContent(
    hexagram = hexagram,
    body = hexagram.yearMessage?.blessing.orEmpty(),
    judgment = null,
    aiLabel = false,
    source = "year",
    style = CardStyle.NEW_YEAR,
    yearLabel = "กวาประจำปี$yearLabel",
)

/** What goes on a Siam Si share card: the slip and where it comes from. Like every card, never the question. */
data class SiamSiShareContent(
    val setId: String,
    val setName: String,
    val number: String,
    val level: String,
    /** The original Chinese poem; empty for a Thai set, whose poem is [poemTh] alone. */
    val poemOrig: List<String>,
    val poemTh: List<String>,
    val labelOrig: String?,
    val source: String,
)

fun siamsiShareContent(set: SiamSiSet, stick: SiamSiStick) = SiamSiShareContent(
    setId = set.id,
    setName = set.name,
    number = stick.number.toThaiNumerals(),
    level = listOfNotNull(stick.level.labelTh, stick.gradeOrig).joinToString(" · "),
    poemOrig = if (stick.poemOrig == stick.poemTh) emptyList() else stick.poemOrig,
    poemTh = stick.poemTh,
    labelOrig = stick.labelOrig,
    source = "ตำรา ${set.sourceTitle}",
)
