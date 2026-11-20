package com.iching.ai.ui.strings

/** The year hexagram: the ritual, the result and the messages around them. */
interface YearStrings {
    fun heading(yearLabel: String): String
    val ritualTitle: String
    val ritualPrompt: String
    val loading: String
    val offline: String
    val rateLimited: String
    val inactive: String
    val retry: String
    val themeNotice: String
    val work: String
    val love: String
    val money: String
    val share: String
    val askHermit: String
    val readScripture: String
    val footnote: String
}

object ThYearStrings : YearStrings {
    override fun heading(yearLabel: String) = "กวาประจำปี$yearLabel"
    override val ritualTitle = "ตั้งจิตอธิษฐานรับปีใหม่"
    override val ritualPrompt = "ตั้งจิตให้สงบ แล้วโยนเหรียญ"
    override val loading = "กำลังตั้งจิต..."
    override val offline = "ต้องเชื่อมต่ออินเทอร์เน็ตเพื่อรับกวาประจำปี"
    override val rateLimited = "รอสักครู่แล้วลองโยนอีกครั้ง"
    override val inactive = "ช่วงเสี่ยงทายกวาประจำปีสิ้นสุดแล้ว"
    override val retry = "ลองใหม่"
    override val themeNotice = "ท่านได้รับธีมชาดทอง เลือกใช้ได้ในหน้าตั้งค่า"
    override val work = "การงาน"
    override val love = "ความรัก"
    override val money = "การเงิน"
    override val share = "แชร์การ์ดปีใหม่"
    override val askHermit = "ถามนักพรตเรื่องปีนี้"
    override val readScripture = "อ่านตัวบทคัมภีร์ของกวานี้"
    override val footnote = "ข้อความประจำปีเขียนไว้ล่วงหน้าจากความหมายของกวา ใช้เพื่อการสะท้อนคิด"
}

object EnYearStrings : YearStrings {
    override fun heading(yearLabel: String) = "Year hexagram · $yearLabel"
    override val ritualTitle = "Settle your mind and welcome the new year"
    override val ritualPrompt = "Settle your mind, then toss the coins"
    override val loading = "Settling your mind..."
    override val offline = "An internet connection is needed to receive your year hexagram."
    override val rateLimited = "Please wait a moment, then toss again."
    override val inactive = "The year hexagram period has ended."
    override val retry = "Try again"
    override val themeNotice = "You have received the Cinnabar Gold theme. You can choose it in Settings."
    override val work = "Work"
    override val love = "Love"
    override val money = "Money"
    override val share = "Share your new-year card"
    override val askHermit = "Ask the hermit about this year"
    override val readScripture = "Read this hexagram in the book"
    override val footnote = "The yearly message is written in advance from the meaning of the hexagram. Use it for reflection."
}
