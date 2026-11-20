package com.iching.ai.ui.strings

import com.iching.ai.domain.model.AppLanguage

/** The fixed text on the share cards and the share sheet; the seeker's question is never on a card. */
interface ShareStrings {
    /** The message sent with the card image, ending in the store link. */
    val shareText: String
    fun yearCardHeader(yearLabel: String): String
    fun number(n: Int): String
    fun sourceLine(sourceTitle: String): String
    val slipLabel: String
    val brand: String
    val brandSiamsi: String
    val aiLabel: String
    val failed: String
    val close: String
    val share: String
}

object ThShareStrings : ShareStrings {
    override val shareText = "เสี่ยงทายกับคัมภีร์อี้จิงได้ที่ https://play.google.com/store/apps/details?id=com.iching.ai"
    override fun yearCardHeader(yearLabel: String) = "กวาประจำปี$yearLabel"
    override fun number(n: Int) = formatNumber(n, AppLanguage.TH)
    override fun sourceLine(sourceTitle: String) = "ตำรา $sourceTitle"
    override val slipLabel = "ใบที่"
    override val brand = "อี้จิง"
    override val brandSiamsi = "อี้จิง AI · เซียมซี"
    override val aiLabel = "เนื้อหาสร้างโดย AI · เพื่อการสะท้อนคิด"
    override val failed = "แชร์ไม่สำเร็จ โปรดลองใหม่"
    override val close = "ปิด"
    override val share = "แชร์"
}

object EnShareStrings : ShareStrings {
    override val shareText = "Consult the I Ching at https://play.google.com/store/apps/details?id=com.iching.ai"
    override fun yearCardHeader(yearLabel: String) = "Hexagram for the $yearLabel"
    override fun number(n: Int) = formatNumber(n, AppLanguage.EN)
    override fun sourceLine(sourceTitle: String) = "Source: $sourceTitle"
    override val slipLabel = "Slip"
    override val brand = "I Ching"
    override val brandSiamsi = "I Ching AI · Siam Si"
    override val aiLabel = "AI-generated content · for reflection"
    override val failed = "Sharing did not work. Please try again."
    override val close = "Close"
    override val share = "Share"
}
