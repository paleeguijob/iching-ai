package com.iching.ai.ui.strings

import com.iching.ai.domain.oracle.ReadingSection

/** The reading view: its header, the hermit's cards, the disclaimer and the original scripture block. */
interface ReadingStrings {
    val sheetScripture: String
    val sheetHermit: String
    val hermitStudying: String
    val unspokenQuestion: String
    val primaryLabel: String
    val relatingLabel: String
    val changesInto: String
    val aiDisclaimer: String
    val scriptureDisclaimer: String
    val reported: String
    val reportAction: String
    val scriptureTitle: String
    val judgment: String
    val image: String
    val allLinesChange: String
    fun relatingHeading(name: String): String

    /** The heading of one of the hermit's cards; the server's Thai marker is the key and is never shown as is in English. */
    fun sectionTitle(section: ReadingSection): String
}

object ThReadingStrings : ReadingStrings {
    override val sheetScripture = "ตัวบทจากคัมภีร์"
    override val sheetHermit = "คำชี้แนะจากนักพรต"
    override val hermitStudying = "นักพรตกำลังพินิจคัมภีร์..."
    override val unspokenQuestion = "ตั้งจิตอธิษฐาน มิได้เอ่ยคำถาม"
    override val primaryLabel = "กวาหลัก"
    override val relatingLabel = "กวาแปร"
    override val changesInto = "เปลี่ยนเป็น"
    override val aiDisclaimer = "เนื้อหาคำทำนายสร้างโดย AI อ้างอิงคัมภีร์อี้จิง · เพื่อความบันเทิงและการสะท้อนคิด ไม่ใช่คำแนะนำทางการแพทย์ กฎหมาย หรือการเงิน"
    override val scriptureDisclaimer = "ตัวบทจากคัมภีร์อี้จิง · เพื่อความบันเทิงและการสะท้อนคิด ไม่ใช่คำแนะนำทางการแพทย์ กฎหมาย หรือการเงิน"
    override val reported = "รายงานแล้ว"
    override val reportAction = "รายงานคำตอบไม่เหมาะสม"
    override val scriptureTitle = "ตัวบทคัมภีร์ดั้งเดิม"
    override val judgment = "คำตัดสิน"
    override val image = "ภาพลักษณ์"
    override val allLinesChange = "เส้นทั้งหกแปร"
    override fun relatingHeading(name: String) = "กวาแปร · $name"
    override fun sectionTitle(section: ReadingSection) = section.label
}

object EnReadingStrings : ReadingStrings {
    override val sheetScripture = "Text from the book"
    override val sheetHermit = "The hermit's guidance"
    override val hermitStudying = "The hermit is studying the book..."
    override val unspokenQuestion = "A silent prayer, no question spoken"
    override val primaryLabel = "Primary hexagram"
    override val relatingLabel = "Relating hexagram"
    override val changesInto = "Changes into"
    override val aiDisclaimer = "This reading is written by AI from the I Ching text · for reflection and entertainment, not medical, legal or financial advice"
    override val scriptureDisclaimer = "Text from the I Ching · for reflection and entertainment, not medical, legal or financial advice"
    override val reported = "Reported"
    override val reportAction = "Report an inappropriate answer"
    override val scriptureTitle = "Original scripture"
    override val judgment = "Judgment"
    override val image = "Image"
    override val allLinesChange = "All six lines changing"
    override fun relatingHeading(name: String) = "Relating hexagram · $name"
    override fun sectionTitle(section: ReadingSection) = when (section.label) {
        "ดวงเด่น" -> "Going well"
        "ควรระวัง" -> "Watch out"
        "คำแนะนำ" -> "What to do"
        else -> section.label
    }
}
