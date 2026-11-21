package com.iching.ai.ui.strings

/** Asking for a store rating (the rating sheet itself is the store's); only the Settings row is ours. */
interface ReviewStrings {
    val rateApp: String
    val rateAppSubtitle: String
}

object ThReviewStrings : ReviewStrings {
    override val rateApp = "ให้คะแนนแอป"
    override val rateAppSubtitle = "รีวิวใน Google Play ช่วยให้คนอื่นเจอแอปนี้"
}

object EnReviewStrings : ReviewStrings {
    override val rateApp = "Rate the app"
    override val rateAppSubtitle = "A review on Google Play helps others find this app"
}
