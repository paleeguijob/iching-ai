package com.iching.ai.feature.share

import com.iching.ai.domain.model.Hexagram
import com.iching.ai.domain.oracle.stripMarkup

const val SHARE_TEXT = "เสี่ยงทายกับคัมภีร์อี้จิงได้ที่ https://play.google.com/store/apps/details?id=com.iching.ai"

/** What goes on a share card. There is deliberately no field for the seeker's question: it stays private. */
data class ShareCardContent(
    val hexagram: Hexagram,
    val body: String,
    val judgment: String?,
    val aiLabel: Boolean,
    /** Analytics value: "daily" or "reading". */
    val source: String,
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
fun readingShareContent(hexagram: Hexagram, summary: String?): ShareCardContent {
    val cleaned = summary?.let(::stripMarkup).orEmpty()
    return if (cleaned.isEmpty()) {
        ShareCardContent(hexagram, hexagram.judgment, judgment = null, aiLabel = false, source = "reading")
    } else {
        ShareCardContent(
            hexagram = hexagram,
            body = cleaned,
            judgment = hexagram.judgment,
            aiLabel = true,
            source = "reading",
        )
    }
}
