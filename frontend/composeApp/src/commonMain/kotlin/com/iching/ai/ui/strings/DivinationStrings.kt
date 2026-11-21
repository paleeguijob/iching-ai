package com.iching.ai.ui.strings

import com.iching.ai.domain.model.AppLanguage
import com.iching.ai.domain.model.LineType

/** The casting screens, the notice and follow-up cards under a reading, and the messages the reading view model raises. */
interface DivinationStrings {
    val titleCasting: String
    val titleReveal: String
    val titleSafety: String
    val titleReading: String
    val yearOpening: String
    val blankPrompt: String
    val shareCard: String
    val savedNote: String
    val backToCalm: String
    val unrolling: String
    val leaveTitle: String
    val leaveSaved: String
    val leaveUnsaved: String
    val stay: String
    val leave: String

    val tossHint: String
    val settling: String
    val toss: String
    val tossNext: String
    val tossRest: String
    fun castProgress(cast: Int): String
    fun lineGloss(line: LineType): String
    fun lineLabel(line: LineType): String

    val listenPreparing: String
    val listenStop: String
    val listen: String
    val listenPremium: String
    val noVoiceTitle: String
    val noVoiceBody: String
    val openSettings: String
    val notNow: String

    val howToReadTitle: String
    val termPrimaryMeaning: String
    val termRelatingMeaning: String
    val noChangingLines: String
    val tapToReadFull: String

    val noticeQuotaGranted: String
    val noticeQuotaAdsDone: String
    val noticeQuotaDone: String
    fun noticeRateLimited(retryAfterSeconds: Int?): String
    val noticeAiDisabled: String
    val noticeFollowUpLimit: String
    val noticeNetwork: String
    val noticeBlocked: String
    val noticeUnauthenticated: String
    val noticeOther: String
    val noticeOffline: String
    val getGuidance: String
    fun watchAdForGuidance(left: Int): String
    val askAgain: String

    val followUpTitle: String
    fun followUpTeaser(questions: Int): String
    val followUpPremiumOnly: String
    val seePlans: String
    val hermitThinking: String
    fun followUpPlaceholder(left: Int): String
    val send: String
    val followUpsUsed: String

    fun reportReason(id: String): String
    val reportTitle: String
    val reportSubmit: String

    val helplineLabel: String
    val helplineUri: String
    val emergencyLabel: String

    /** An empty "tel:" opens the dialer without a number, for languages with no number that fits everywhere. */
    val emergencyUri: String
    val backHome: String

    val recordNotFound: String
    val yearHexagramMissing: String
    val hermitUnreachable: String
    val scriptureUnavailable: String
    val followUpLimitReached: String
    val followUpNeedsPremium: String
    val followUpNoNetwork: String
    val followUpFailed: String
    val reportThanks: String
    val reportFailed: String
}

object ThDivinationStrings : DivinationStrings {
    private val lang = AppLanguage.TH

    override val titleCasting = "เสี่ยงทาย"
    override val titleReveal = "ฉักกะปรากฏ"
    override val titleSafety = "หยุดพักก่อน"
    override val titleReading = "คำทำนาย"
    override val yearOpening = "กำลังเปิดกวาประจำปีของท่าน..."
    override val blankPrompt = "ตั้งจิตให้สงบ แล้วโยนเหรียญ"
    override val shareCard = "แชร์การ์ดคำทำนาย"
    override val savedNote = "บันทึกลงในบันทึกชะตาแล้ว"
    override val backToCalm = "กลับสู่ความสงบ"
    override val unrolling = "สูดลมหายใจลึก ๆ ... คัมภีร์กำลังเปิดออก"
    override val leaveTitle = "ยังทำนายไม่จบ ออกตอนนี้?"
    override val leaveSaved = "กวานี้บันทึกในบันทึกชะตาแล้ว ท่านกลับมารับคำชี้แนะจากนักพรตต่อได้ที่บันทึกชะตา โดยไม่เสียสิทธิ์ซ้ำ"
    override val leaveUnsaved = "หากออกตอนนี้ การเสี่ยงทายครั้งนี้จะไม่ถูกบันทึก"
    override val stay = "อยู่ต่อ"
    override val leave = "ออก"

    override val tossHint = "แตะเหรียญหรือปุ่มด้านล่างทีละครั้ง เส้นจะต่อขึ้นจากล่างจนครบหกเส้น"
    override val settling = "กำลังตั้งจิต..."
    override val toss = "โยนเหรียญ"
    override val tossNext = "โยนครั้งต่อไป"
    override val tossRest = "โยนต่อเนื่องจนครบ"
    override fun castProgress(cast: Int) = "${formatNumber(cast, lang)} / ๖"
    override fun lineGloss(line: LineType) = when (line) {
        LineType.YOUNG_YANG -> "เส้นเต็ม เป็นเส้นนิ่ง ไม่เปลี่ยน"
        LineType.YOUNG_YIN -> "เส้นขาด เป็นเส้นนิ่ง ไม่เปลี่ยน"
        LineType.OLD_YANG -> "เส้นเต็มที่กำลังกลายเป็นเส้นขาด จะพาไปสู่ “กวาแปร”"
        LineType.OLD_YIN -> "เส้นขาดที่กำลังกลายเป็นเส้นเต็ม จะพาไปสู่ “กวาแปร”"
    }
    override fun lineLabel(line: LineType) = when (line) {
        LineType.OLD_YIN -> "หยินแก่ (๖) — เส้นแปร"
        LineType.YOUNG_YANG -> "หยางอ่อน (๗)"
        LineType.YOUNG_YIN -> "หยินอ่อน (๘)"
        LineType.OLD_YANG -> "หยางแก่ (๙) — เส้นแปร"
    }

    override val listenPreparing = "กำลังเตรียมเสียง…"
    override val listenStop = "หยุดอ่าน"
    override val listen = "ฟังคำทำนาย"
    override val listenPremium = "ฟังคำทำนาย · Premium"
    override val noVoiceTitle = "ยังไม่มีเสียงภาษาไทยในเครื่อง"
    override val noVoiceBody = "ติดตั้งเสียงอ่านภาษาไทยในการตั้งค่าของเครื่องก่อน แล้วกลับมาฟังอีกครั้ง"
    override val openSettings = "เปิดการตั้งค่า"
    override val notNow = "ไว้ก่อน"

    override val howToReadTitle = "อ่านกวาอย่างไร"
    override val termPrimaryMeaning = "ภาพของเรื่องนี้ในตอนนี้"
    override val termRelatingMeaning = "ทิศที่เรื่องกำลังเปลี่ยนไป เกิดจากเส้นแปรที่ท่านโยนได้"
    override val noChangingLines = "ครั้งนี้ไม่มีเส้นแปร จึงมีเพียงกวาเดียว"
    override val tapToReadFull = "แตะที่กวาเพื่ออ่านตัวบทเต็มในคัมภีร์"

    override val noticeQuotaGranted = "ท่านได้รับสิทธิ์เพิ่มแล้ว แตะด้านล่างเพื่อรับคำชี้แนะจากนักพรตสำหรับกวานี้"
    override val noticeQuotaAdsDone = "คำชี้แนะจากโฆษณาของวันนี้ครบแล้ว ท่านยังรับคำชี้แนะสำหรับกวานี้ได้"
    override val noticeQuotaDone = "คำชี้แนะจากนักพรตสำหรับวันนี้ครบแล้ว ท่านยังอ่านตัวบทคัมภีร์ด้านล่างได้เสมอ"
    override fun noticeRateLimited(retryAfterSeconds: Int?) =
        "โปรดเว้นจังหวะหายใจสักครู่${retryAfterSeconds?.let { " (${formatNumber(it, lang)} วินาที)" } ?: ""} แล้วจึงถามนักพรตอีกครั้ง"
    override val noticeAiDisabled = "นักพรตปลีกวิเวกชั่วคราว จึงแสดงตัวบทจากคัมภีร์แทน โปรดกลับมาใหม่ภายหลัง"
    override val noticeFollowUpLimit = "ถามต่อครบจำนวนครั้งแล้ว"
    override val noticeNetwork = "การเชื่อมต่อขาดหาย จึงแสดงตัวบทจากคัมภีร์แทน แตะถามนักพรตอีกครั้งเพื่อรับคำทำนายฉบับเต็ม โดยไม่เสียสิทธิ์ซ้ำ"
    override val noticeBlocked = "คำถามนี้ไม่อาจตีความได้ จึงแสดงตัวบทจากคัมภีร์แทน สิทธิ์ของท่านยังไม่ถูกใช้"
    override val noticeUnauthenticated = "ยืนยันตัวตนกับระบบไม่สำเร็จ จึงแสดงตัวบทจากคัมภีร์แทน"
    override val noticeOther = "การติดต่อนักพรตขัดข้องชั่วคราว จึงแสดงตัวบทจากคัมภีร์แทน สิทธิ์ของท่านยังไม่ถูกใช้"
    override val noticeOffline = "ไม่สามารถเชื่อมต่ออินเทอร์เน็ตได้ จึงเสี่ยงทายในเครื่องและแสดงตัวบทจากคัมภีร์"
    override val getGuidance = "รับคำชี้แนะจากนักพรต"
    override fun watchAdForGuidance(left: Int) = "ชมโฆษณาสั้น รับคำชี้แนะ (เหลือ ${formatNumber(left, lang)})"
    override val askAgain = "ถามนักพรตอีกครั้ง"

    override val followUpTitle = "ถามต่อกับนักพรต"
    override fun followUpTeaser(questions: Int) = "สนทนาต่อในกวาเดิมได้ ${formatNumber(questions, lang)} คำถาม"
    override val followUpPremiumOnly = "สิทธิ์ของสมาชิก Premium"
    override val seePlans = "ดูแพ็กเกจ"
    override val hermitThinking = "นักพรตกำลังพินิจ..."
    override fun followUpPlaceholder(left: Int) = "ถามต่อ (เหลือ ${formatNumber(left, lang)} คำถาม)"
    override val send = "ส่ง"
    override val followUpsUsed = "ถามต่อครบ ๓ คำถามแล้ว — หากใจยังมีคำถามใหม่ ให้เสี่ยงทายครั้งใหม่"

    override fun reportReason(id: String) = when (id) {
        "harmful" -> "เป็นอันตราย หรือชี้นำให้ทำสิ่งเสี่ยง"
        "offensive" -> "หยาบคาย ไม่เหมาะสม"
        "wrong" -> "ไม่ตรงกับคัมภีร์ หรือข้อมูลผิด"
        else -> "อื่น ๆ"
    }
    override val reportTitle = "รายงานคำตอบนี้"
    override val reportSubmit = "ส่งรายงาน"

    override val helplineLabel = "โทรสายด่วนสุขภาพจิต 1323"
    override val helplineUri = "tel:1323"
    override val emergencyLabel = "เหตุฉุกเฉินทางการแพทย์ 1669"
    override val emergencyUri = "tel:1669"
    override val backHome = "กลับสู่หน้าแรก"

    override val recordNotFound = "ไม่พบบันทึกนี้"
    override val yearHexagramMissing = "ยังไม่พบกวาประจำปีของท่าน โปรดเสี่ยงทายกวาประจำปีก่อน แล้วจึงถามนักพรตเรื่องปีนี้"
    override val hermitUnreachable = "ติดต่อนักพรตไม่ได้ในตอนนี้ โปรดตรวจสอบอินเทอร์เน็ตแล้วลองใหม่อีกครั้ง"
    override val scriptureUnavailable = "ไม่สามารถเปิดคัมภีร์ได้"
    override val followUpLimitReached = "ถามต่อครบจำนวนครั้งของคำทำนายนี้แล้ว"
    override val followUpNeedsPremium = "การถามต่อเป็นสิทธิ์ของสมาชิก Premium"
    override val followUpNoNetwork = "ไม่สามารถเชื่อมต่ออินเทอร์เน็ตได้"
    override val followUpFailed = "นักพรตยังไม่อาจตอบได้ในตอนนี้ โปรดลองใหม่อีกครั้ง"
    override val reportThanks = "ขอบคุณ เราได้รับรายงานแล้วและจะตรวจสอบ"
    override val reportFailed = "ส่งรายงานไม่สำเร็จ โปรดลองใหม่"
}

object EnDivinationStrings : DivinationStrings {
    private val lang = AppLanguage.EN

    override val titleCasting = "Casting"
    override val titleReveal = "The hexagram appears"
    override val titleSafety = "Pause for a moment"
    override val titleReading = "Your reading"
    override val yearOpening = "Opening your year hexagram..."
    override val blankPrompt = "Settle your mind, then toss the coins"
    override val shareCard = "Share reading card"
    override val savedNote = "Saved to your journal"
    override val backToCalm = "Return to calm"
    override val unrolling = "Take a deep breath... the book is opening"
    override val leaveTitle = "The reading is not finished. Leave now?"
    override val leaveSaved = "This hexagram is saved in your journal. You can return there for the hermit's guidance without using another reading."
    override val leaveUnsaved = "If you leave now, this cast will not be saved."
    override val stay = "Stay"
    override val leave = "Leave"

    override val tossHint = "Tap the coins or the button below, one toss at a time. The lines build upward from the bottom until there are six."
    override val settling = "Settling your mind..."
    override val toss = "Toss the coins"
    override val tossNext = "Next toss"
    override val tossRest = "Toss the rest"
    override fun castProgress(cast: Int) = "$cast / 6"
    override fun lineGloss(line: LineType) = when (line) {
        LineType.YOUNG_YANG -> "A solid line. Steady, it does not change"
        LineType.YOUNG_YIN -> "A broken line. Steady, it does not change"
        LineType.OLD_YANG -> "A solid line turning broken. It leads to the “relating hexagram”"
        LineType.OLD_YIN -> "A broken line turning solid. It leads to the “relating hexagram”"
    }
    override fun lineLabel(line: LineType) = when (line) {
        LineType.OLD_YIN -> "Old yin (6) — changing line"
        LineType.YOUNG_YANG -> "Young yang (7)"
        LineType.YOUNG_YIN -> "Young yin (8)"
        LineType.OLD_YANG -> "Old yang (9) — changing line"
    }

    override val listenPreparing = "Preparing the voice…"
    override val listenStop = "Stop"
    override val listen = "Listen to the reading"
    override val listenPremium = "Listen · Premium"
    override val noVoiceTitle = "No voice installed"
    override val noVoiceBody = "Install a reading voice in your device settings, then come back to listen."
    override val openSettings = "Open settings"
    override val notNow = "Not now"

    override val howToReadTitle = "How to read a hexagram"
    override val termPrimaryMeaning = "A picture of your matter as it stands now"
    override val termRelatingMeaning = "Where the matter is heading, drawn from the changing lines you tossed"
    override val noChangingLines = "No changing lines this time, so there is only one hexagram"
    override val tapToReadFull = "Tap a hexagram to read its full text in the book"

    override val noticeQuotaGranted = "You have earned another reading. Tap below to receive the hermit's guidance for this hexagram."
    override val noticeQuotaAdsDone = "Today's ad-supported guidance is used up. You can still get guidance for this hexagram."
    override val noticeQuotaDone = "Today's hermit guidance is used up. You can always read the scripture below."
    override fun noticeRateLimited(retryAfterSeconds: Int?) =
        "Please take a breath${retryAfterSeconds?.let { " ($it seconds)" } ?: ""} and then ask the hermit again."
    override val noticeAiDisabled = "The hermit is away for now, so the scripture is shown instead. Please come back later."
    override val noticeFollowUpLimit = "You have used all your follow-up questions."
    override val noticeNetwork = "The connection dropped, so the scripture is shown instead. Tap to ask the hermit again for the full reading. It will not use another reading."
    override val noticeBlocked = "This question cannot be interpreted, so the scripture is shown instead. Your reading has not been used."
    override val noticeUnauthenticated = "Signing in to the service failed, so the scripture is shown instead."
    override val noticeOther = "The hermit could not be reached for now, so the scripture is shown instead. Your reading has not been used."
    override val noticeOffline = "There is no internet connection, so the cast was made on this device and the scripture is shown."
    override val getGuidance = "Get the hermit's guidance"
    override fun watchAdForGuidance(left: Int) = "Watch a short ad for guidance ($left left)"
    override val askAgain = "Ask the hermit again"

    override val followUpTitle = "Ask the hermit more"
    override fun followUpTeaser(questions: Int) = "Ask up to $questions more questions about this hexagram"
    override val followUpPremiumOnly = "For Premium members"
    override val seePlans = "See plans"
    override val hermitThinking = "The hermit is reflecting..."
    override fun followUpPlaceholder(left: Int) = "Ask more ($left left)"
    override val send = "Send"
    override val followUpsUsed = "You have asked all 3 follow-up questions. For a new question, cast again."

    override fun reportReason(id: String) = when (id) {
        "harmful" -> "Harmful, or encourages risky actions"
        "offensive" -> "Rude or inappropriate"
        "wrong" -> "Does not match the scripture, or is wrong"
        else -> "Other"
    }
    override val reportTitle = "Report this answer"
    override val reportSubmit = "Send report"

    override val helplineLabel = "Find a helpline near you"
    override val helplineUri = "https://findahelpline.com"
    override val emergencyLabel = "Contact your local emergency services"
    override val emergencyUri = "tel:"
    override val backHome = "Back to home"

    override val recordNotFound = "This record was not found."
    override val yearHexagramMissing = "We could not find your year hexagram. Cast your year hexagram first, then ask the hermit about this year."
    override val hermitUnreachable = "The hermit cannot be reached right now. Check your connection and try again."
    override val scriptureUnavailable = "The book could not be opened."
    override val followUpLimitReached = "You have used all follow-up questions for this reading."
    override val followUpNeedsPremium = "Follow-up questions are for Premium members."
    override val followUpNoNetwork = "There is no internet connection."
    override val followUpFailed = "The hermit cannot answer right now. Please try again."
    override val reportThanks = "Thank you. We have your report and will look into it."
    override val reportFailed = "The report could not be sent. Please try again."
}
