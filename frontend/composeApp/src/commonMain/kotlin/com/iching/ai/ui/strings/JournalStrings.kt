package com.iching.ai.ui.strings

import com.iching.ai.domain.model.AppLanguage

/** The destiny journal: timeline, entry detail and the outcome note. */
interface JournalStrings {
    val title: String

    /** [days] is the span the plan keeps open (null for unlimited), [count] the readings shown. */
    fun subtitle(days: Int?, count: Int): String
    val filterAll: String
    val filterIching: String
    val filterSiamsi: String
    fun hiddenTitle(count: Int): String
    val hiddenBody: String
    val seePlans: String
    val yearReading: String
    val siamsiTag: String
    val scriptureTag: String
    val prayedInHeart: String
    val emptyTitle: String
    val emptyBody: String
    val emptyAction: String
    val deleteDescription: String
    val deleteTitle: String
    val deleteBody: String
    val delete: String
    val shareSlip: String
    val setMissing: String
    val outcomeQuestion: String
    val outcomeYes: String
    val outcomePartly: String
    val outcomeNo: String
    val notePlaceholder: String
    val save: String
    val askHermitBody: String
    val askHermitAction: String

    /** The row of a Siam Si reading, e.g. "Guanyin slip 17 · Good · Interpreted"; [levelLabel] is null when the slip is gone. */
    fun siamsiRow(setName: String, number: Int, labelOrig: String?, levelLabel: String?, hasInterpretation: Boolean): String
}

object ThJournalStrings : JournalStrings {
    private val lang = AppLanguage.TH

    override val title = "บันทึกชะตา"
    override fun subtitle(days: Int?, count: Int) =
        if (days == null) "ย้อนมองเส้นทางที่ผ่านมา $count ครั้งแห่งการถามไถ่"
        else "$days วันล่าสุด · $count ครั้งแห่งการถามไถ่"
    override val filterAll = "ทั้งหมด"
    override val filterIching = "อี้จิง"
    override val filterSiamsi = "เซียมซี"
    override fun hiddenTitle(count: Int) = "บันทึกเก่าอีก $count รายการ"
    override val hiddenBody = "ยังเก็บไว้ในเครื่องของท่าน · เปิดดูทั้งหมดและกู้คืนข้ามเครื่องได้ด้วย Premium"
    override val seePlans = "ดูแพ็กเกจ"
    override val yearReading = "กวาประจำปี"
    override val siamsiTag = "เซียมซี"
    override val scriptureTag = "ตัวบทคัมภีร์"
    override val prayedInHeart = "อธิษฐานในใจ"
    override val emptyTitle = "บันทึกชะตายังว่างเปล่า"
    override val emptyBody = "ทุกครั้งที่ท่านเสี่ยงทาย คำถามและคำชี้แนะจะถูกจารึกไว้ที่นี่\nให้ย้อนกลับมาทบทวนตามกาลเวลา"
    override val emptyAction = "เสี่ยงทายครั้งแรก"
    override val deleteDescription = "ลบบันทึก"
    override val deleteTitle = "ลบบันทึกนี้?"
    override val deleteBody = "บันทึกที่ลบแล้วจะไม่สามารถนำกลับคืนมาได้"
    override val delete = "ลบ"
    override val shareSlip = "แชร์ใบเซียมซี"
    override val setMissing = "ตำราชุดนี้ไม่มีในเครื่องแล้ว"
    override val outcomeQuestion = "เกิดขึ้นจริงไหม"
    override val outcomeYes = "เกิดขึ้นจริง"
    override val outcomePartly = "เกิดขึ้นบางส่วน"
    override val outcomeNo = "ไม่เกิดขึ้น"
    override val notePlaceholder = "จดบันทึกส่วนตัว: สิ่งที่เกิดขึ้น สิ่งที่ได้เรียนรู้..."
    override val save = "บันทึก"
    override val askHermitBody = "กวานี้ยังไม่ได้รับคำชี้แนะจากนักพรต หากคำทำนายถูกตัดกลางทาง ท่านรับต่อได้โดยไม่เสียสิทธิ์ซ้ำ"
    override val askHermitAction = "รับคำชี้แนะจากนักพรต"

    override fun siamsiRow(setName: String, number: Int, labelOrig: String?, levelLabel: String?, hasInterpretation: Boolean) =
        buildString {
            append("${setName.removePrefix("เซียมซี").removePrefix("เจ้าแม่").trim()} ใบที่ ${formatNumber(number, lang)}")
            labelOrig?.let { append(" $it") }
            levelLabel?.let { append(" · $it") }
            if (hasInterpretation) append(" · มีคำแปล")
        }
}

object EnJournalStrings : JournalStrings {
    override val title = "Journal"
    override fun subtitle(days: Int?, count: Int) =
        if (days == null) "Looking back on $count ${if (count == 1) "question" else "questions"} you have asked"
        else "Last $days ${if (days == 1) "day" else "days"} · $count ${if (count == 1) "question" else "questions"} asked"
    override val filterAll = "All"
    override val filterIching = "I Ching"
    override val filterSiamsi = "Siam Si"
    override fun hiddenTitle(count: Int) = "$count older ${if (count == 1) "entry" else "entries"}"
    override val hiddenBody = "Still kept on your device · see them all and restore across devices with Premium"
    override val seePlans = "See plans"
    override val yearReading = "Year hexagram"
    override val siamsiTag = "Siam Si"
    override val scriptureTag = "Original text"
    override val prayedInHeart = "A silent wish"
    override val emptyTitle = "Your journal is empty"
    override val emptyBody = "Each time you cast, your question and its guidance are kept here.\nCome back to them as time passes."
    override val emptyAction = "Cast for the first time"
    override val deleteDescription = "Delete entry"
    override val deleteTitle = "Delete this entry?"
    override val deleteBody = "A deleted entry cannot be brought back."
    override val delete = "Delete"
    override val shareSlip = "Share slip"
    override val setMissing = "This set is no longer on your device."
    override val outcomeQuestion = "Did it come true?"
    override val outcomeYes = "It happened"
    override val outcomePartly = "Partly"
    override val outcomeNo = "It did not happen"
    override val notePlaceholder = "A private note: what happened, what you learned..."
    override val save = "Save"
    override val askHermitBody = "The hermit has not yet answered this hexagram. If the reading was cut short, you can pick it up again without using another reading."
    override val askHermitAction = "Get the hermit's guidance"

    override fun siamsiRow(setName: String, number: Int, labelOrig: String?, levelLabel: String?, hasInterpretation: Boolean) =
        buildString {
            append("$setName · Slip $number")
            labelOrig?.let { append(" $it") }
            levelLabel?.let { append(" · $it") }
            if (hasInterpretation) append(" · Interpreted")
        }
}
