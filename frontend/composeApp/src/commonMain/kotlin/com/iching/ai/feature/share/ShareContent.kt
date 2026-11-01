package com.iching.ai.feature.share

import com.iching.ai.domain.model.Hexagram
import com.iching.ai.domain.oracle.excerpt
import com.iching.ai.domain.oracle.stripMarkup

private const val AI_EXCERPT_CHARS = 200

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

/** [aiText] is the hermit's reading; null or blank when the seeker only got the scripture. */
fun readingShareContent(hexagram: Hexagram, aiText: String?): ShareCardContent {
    val cleaned = aiText?.let(::stripMarkup).orEmpty()
    return if (cleaned.isEmpty()) {
        ShareCardContent(hexagram, hexagram.judgment, judgment = null, aiLabel = false, source = "reading")
    } else {
        ShareCardContent(
            hexagram = hexagram,
            body = excerpt(cleaned, AI_EXCERPT_CHARS),
            judgment = hexagram.judgment,
            aiLabel = true,
            source = "reading",
        )
    }
}
