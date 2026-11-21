package com.iching.ai.ui.strings

/** The Android home-screen widget: only what shows when today's hexagram can't be read. */
interface WidgetStrings {
    val empty: String
}

object ThWidgetStrings : WidgetStrings {
    override val empty = "แตะเพื่อดูกวาประจำวัน"
}

object EnWidgetStrings : WidgetStrings {
    override val empty = "Tap to see today's hexagram"
}
