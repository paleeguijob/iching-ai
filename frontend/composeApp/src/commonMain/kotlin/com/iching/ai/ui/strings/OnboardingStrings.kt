package com.iching.ai.ui.strings

import com.iching.ai.domain.model.AppLanguage
import com.iching.ai.domain.model.LineType

/** First-launch pages, the practice cast and the disclaimer. */
interface OnboardingStrings {
    /** Title and body of each point on the disclaimer page. */
    val points: List<Pair<String, String>>
    val skip: String
    val next: String
    val acceptAndStart: String
    fun pageOf(current: Int, count: Int): String

    val askTitle: String
    val askBody: String
    val castTitle: String
    val castBody: String
    val readTitle: String
    val readBody: String
    val readNote: String

    val askBlank: String
    fun aboutCategory(label: String): String
    val noQuestionNeeded: String

    val tossLabel: String
    val tryTapping: String
    val practiceDone: String
    fun practiceProgress(tossed: Int, lineName: String): String
    fun practiceLineName(line: LineType): String

    val sampleName: String
    val sampleJudgment: String

    val disclaimerTitle: String
    val terms: String
    val privacy: String

    /** Path of the privacy page on the web host, in this language. */
    val privacyPath: String

    /** Path of the terms page on the web host, in this language. */
    val termsPath: String

    /** The language picker. Bilingual on purpose, the same in both languages: a seeker must find their own. */
    val languageTitle: String
    val languageThai: String
    val languageEnglish: String
    val languageHint: String
}

/** Shown as is in Thai and in English: someone with English switched on must still be able to find Thai. */
internal const val LANGUAGE_TITLE = "ภาษา / Language"
internal const val LANGUAGE_NAME_TH = "ภาษาไทย"
internal const val LANGUAGE_NAME_EN = "English"
internal const val LANGUAGE_HINT = "เปลี่ยนได้ในตั้งค่า · Change anytime in Settings"

object ThOnboardingStrings : OnboardingStrings {
    private val lang = AppLanguage.TH

    override val points = listOf(
        "เพื่อความบันเทิงและการสะท้อนคิด" to
            "คำทำนายช่วยให้ท่านใคร่ครวญสถานการณ์ มิใช่คำแนะนำทางการแพทย์ กฎหมาย การเงิน หรือจิตวิทยา โปรดใช้วิจารณญาณก่อนตัดสินใจเรื่องสำคัญ",
        "เนื้อหาสร้างโดย AI" to
            "นักพรตในแอปนี้คือผู้ช่วย AI (Google Gemini) ที่สวมบทบาทและอ้างอิงตัวบทคัมภีร์อี้จิงจริง คำตอบอาจคลาดเคลื่อนได้",
        "ความเป็นส่วนตัว" to
            "คำถามของท่านจะถูกส่งไปประมวลผลที่ Google Gemini โปรดอย่าพิมพ์ข้อมูลที่ระบุตัวตน เช่น เลขบัตร ที่อยู่ หรือข้อมูลสุขภาพ",
        "ยามทุกข์ใจ" to
            "หากท่านกำลังรู้สึกไม่ปลอดภัยหรือคิดทำร้ายตนเอง โปรดโทรสายด่วนสุขภาพจิต 1323 หรือเหตุฉุกเฉิน 1669",
    )
    override val skip = "ข้าม"
    override val next = "ถัดไป"
    override val acceptAndStart = "รับทราบและเริ่มต้น"
    override fun pageOf(current: Int, count: Int) = "หน้า ${formatNumber(current, lang)} จาก ${formatNumber(count, lang)}"

    override val askTitle = "ถามเรื่องที่ค้างใจ"
    override val askBody = "อี้จิงคือคัมภีร์โบราณของจีน ที่ผู้คนใช้ถามทางในยามลังเล\nพิมพ์คำถามของท่าน\nหรือเว้นว่างไว้แล้วอธิษฐานในใจก็ได้"
    override val castTitle = "โยนเหรียญหกครั้ง"
    override val castBody = "เหรียญสามเหรียญ โยนหนึ่งครั้งได้หนึ่งเส้น\nต่อขึ้นจากล่างทีละเส้น\nครบหกเส้นเรียกว่า “กวา” หนึ่งใน ๖๔ กวาของคัมภีร์"
    override val readTitle = "อ่านคำตอบสองชั้น"
    override val readBody = "ชั้นแรก คือตัวบทคัมภีร์ของกวาที่ท่านได้\nชั้นที่สอง คือคำชี้แนะจากนักพรต\nซึ่ง AI เรียบเรียงจากตัวบท\nให้ตรงกับคำถามของท่าน"
    override val readNote = "เสี่ยงทายและอ่านตัวบทได้เสมอ\nส่วนคำชี้แนะจากนักพรตมีจำนวนต่อวัน\nหน้าแรกบอกที่เหลือไว้ก่อนท่านเริ่มทุกครั้ง"

    override val askBlank = "เว้นว่างไว้ อธิษฐานในใจ…"
    override fun aboutCategory(label: String) = "เรื่อง$label"
    override val noQuestionNeeded = "ไม่ต้องเอ่ยคำถามก็ได้"

    override val tossLabel = "โยนเหรียญ"
    override val tryTapping = "ลองแตะเหรียญดู"
    override val practiceDone = "ครบหกเส้น ได้หนึ่งกวา · แตะเพื่อลองใหม่"
    override fun practiceProgress(tossed: Int, lineName: String) = "${formatNumber(tossed, lang)} / ๖ · $lineName"
    override fun practiceLineName(line: LineType) = when (line) {
        LineType.YOUNG_YANG -> "เส้นเต็ม (หยาง)"
        LineType.YOUNG_YIN -> "เส้นขาด (หยิน)"
        LineType.OLD_YANG -> "เส้นเต็มที่กำลังแปร"
        LineType.OLD_YIN -> "เส้นขาดที่กำลังแปร"
    }

    override val sampleName = "๑๑ · ไท่ สันติ, ความรุ่งเรือง"
    override val sampleJudgment = "“สิ่งเล็กจากไป สิ่งใหญ่มาถึง เป็นมงคล แผ่ซ่าน”"

    override val disclaimerTitle = "ก่อนเริ่มถามทาง"
    override val terms = "ข้อกำหนดการใช้งาน"
    override val privacy = "นโยบายความเป็นส่วนตัว"
    override val privacyPath = "/privacy"
    override val termsPath = "/terms"
    override val languageTitle = LANGUAGE_TITLE
    override val languageThai = LANGUAGE_NAME_TH
    override val languageEnglish = LANGUAGE_NAME_EN
    override val languageHint = LANGUAGE_HINT
}

object EnOnboardingStrings : OnboardingStrings {
    override val points = listOf(
        "For reflection and entertainment" to
            "A reading helps you reflect on your situation. It is not medical, legal, financial or psychological advice. Please use your own judgment on important decisions.",
        "Content made by AI" to
            "The hermit in this app is an AI assistant (Google Gemini) playing a role and drawing on the real I Ching text. Its answers can be wrong.",
        "Privacy" to
            "Your question is sent to Google Gemini for processing. Please do not type identifying details such as ID numbers, addresses or health information.",
        "In hard times" to
            "If you feel unsafe or are thinking of harming yourself, please contact your local emergency services, or find a helpline near you at findahelpline.com.",
    )
    override val skip = "Skip"
    override val next = "Next"
    override val acceptAndStart = "I understand, begin"
    override fun pageOf(current: Int, count: Int) = "Page $current of $count"

    override val askTitle = "Ask what is on your mind"
    override val askBody = "The I Ching is an ancient Chinese book that people consult in times of doubt.\nType your question,\nor leave it blank and ask silently."
    override val castTitle = "Toss the coins six times"
    override val castBody = "Three coins, one toss, one line.\nThe lines build upward from the bottom.\nSix lines make a hexagram, one of the book's 64."
    override val readTitle = "Read the answer in two layers"
    override val readBody = "First, the book's own text for your hexagram.\nSecond, the hermit's guidance,\nwritten by AI from that text\nto fit your question."
    override val readNote = "You can always cast and read the text.\nThe hermit's guidance is limited each day.\nThe home screen shows what is left before you begin."

    override val askBlank = "Leave it blank, ask silently…"
    override fun aboutCategory(label: String) = "About $label"
    override val noQuestionNeeded = "No need to speak a question"

    override val tossLabel = "Toss the coins"
    override val tryTapping = "Try tapping the coins"
    override val practiceDone = "Six lines make a hexagram · tap to try again"
    override fun practiceProgress(tossed: Int, lineName: String) = "$tossed / 6 · $lineName"
    override fun practiceLineName(line: LineType) = when (line) {
        LineType.YOUNG_YANG -> "Solid line (yang)"
        LineType.YOUNG_YIN -> "Broken line (yin)"
        LineType.OLD_YANG -> "Solid line, changing"
        LineType.OLD_YIN -> "Broken line, changing"
    }

    override val sampleName = "11 · Tài · Peace"
    override val sampleJudgment = "“The small go away, the great arrive. Good fortune. Flourishing.”"

    override val disclaimerTitle = "Before you begin"
    override val terms = "Terms of use"
    override val privacy = "Privacy policy"
    override val privacyPath = "/en/privacy"
    override val termsPath = "/en/terms"
    override val languageTitle = LANGUAGE_TITLE
    override val languageThai = LANGUAGE_NAME_TH
    override val languageEnglish = LANGUAGE_NAME_EN
    override val languageHint = LANGUAGE_HINT
}
