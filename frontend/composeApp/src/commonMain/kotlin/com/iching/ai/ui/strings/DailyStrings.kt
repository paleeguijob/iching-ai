package com.iching.ai.ui.strings

import com.iching.ai.domain.model.AppLanguage

/** The daily hexagram page and its two dialogs. */
interface DailyStrings {
    val title: String
    fun streakLine(days: Int): String
    val judgment: String
    val shareToday: String
    val readMore: String
    fun unlockTitle(themeLabel: String): String
    fun unlockBody(days: Int): String
    val acknowledge: String
    val pushTitle: String
    val pushBody: String
    val pushAccept: String
    val pushLater: String
}

object ThDailyStrings : DailyStrings {
    private val lang = AppLanguage.TH

    override val title = "กวาประจำวัน"
    override fun streakLine(days: Int) = "เส้นทางเต๋า · เปิดดูต่อเนื่อง ${formatNumber(days, lang)} วัน"
    override val judgment = "คำตัดสิน"
    override val shareToday = "แชร์การ์ดวันนี้"
    override val readMore = "อ่านต่อในคัมภีร์"
    override fun unlockTitle(themeLabel: String) = "ปลดล็อกธีม$themeLabel"
    override fun unlockBody(days: Int) = "ท่านเปิดดูกวาประจำวันครบ ${formatNumber(days, lang)} วันต่อเนื่องแล้ว เลือกใช้ธีมนี้ได้ในหน้าตั้งค่า"
    override val acknowledge = "รับทราบ"
    override val pushTitle = "รับกวาประจำวันทุกเช้าไหม"
    override val pushBody = "นักพรตจะส่งข้อความประจำวันให้ท่านเวลา ๗ โมงเช้า ปิดได้ทุกเมื่อในหน้าตั้งค่า"
    override val pushAccept = "รับแจ้งเตือน"
    override val pushLater = "ไว้ก่อน"
}

object EnDailyStrings : DailyStrings {
    override val title = "Daily hexagram"
    override fun streakLine(days: Int) = "Path of the Dao · $days-day streak"
    override val judgment = "Judgment"
    override val shareToday = "Share today's card"
    override val readMore = "Read on in the book"
    override fun unlockTitle(themeLabel: String) = "Theme unlocked: $themeLabel"
    override fun unlockBody(days: Int) = "You have opened the daily hexagram $days days in a row. You can choose this theme in Settings."
    override val acknowledge = "Got it"
    override val pushTitle = "Receive the daily hexagram every morning?"
    override val pushBody = "The hermit will send you a daily message once a day. You can turn it off any time in Settings."
    override val pushAccept = "Turn on notifications"
    override val pushLater = "Not now"
}
