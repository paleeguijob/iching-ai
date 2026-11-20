package com.iching.ai.ui.strings

import com.iching.ai.domain.model.AppLanguage
import com.iching.ai.domain.siamsi.PuaResult

/** The Siam Si draw: the quota line on Home, the set sheet, the shake and the moon blocks, the slip, the hermit's interpretation. */
interface SiamSiStrings {
    // The quota line and the leaving dialog
    fun gateLeft(left: Int): String
    val gateLimit: String
    val gateAdWatched: String
    val gateNoAd: String
    val gateAdDismissed: String
    fun gateWatchAd(left: Int): String

    // The set sheet and row
    fun setRow(name: String, count: Int): String
    val setSheetTitle: String
    val setSheetSubtitle: String
    fun setSource(source: String, count: Int): String

    // The screen
    val titleSiamsi: String
    val titleSlip: String
    val titlePua: String
    val shareSlipDescription: String
    fun stickLabel(number: Int): String
    val confirmByPua: String
    val tossPua: String
    val shakeAgain: String
    val openSlip: String
    val leaveDrawTitle: String
    val leaveDrawBody: String
    val leaveInterpretTitle: String
    val leaveInterpretBody: String
    val leaveResumableBody: String
    val stay: String
    val leave: String
    val prayInHeart: String

    // Shaking
    val holdToShake: String
    val shakeHint: String
    val cylinderDescription: String
    val stickDrawn: String
    fun stickDrawnDescription(number: Int): String
    fun stickTitle(number: Int): String

    // The moon blocks
    val tossPrompt: String
    val puaRules: String
    fun puaLabel(result: PuaResult): String
    fun puaHeadline(result: PuaResult, autoConfirmed: Boolean): Pair<String, String>
    val blockUp: String
    val blockDown: String
    val tossLabel: String
    val blocksDescription: String

    // The slip
    val reflection: String
    fun slipHeader(shortName: String, number: Int): String
    val askMore: String
    val shareSlip: String
    fun sourceLine(sourceTitle: String): String
    val storyTitle: String
    val storyHide: String
    val storyShow: String
    val askHermit: String
    val costOne: String
    fun costLeft(left: Int): String
    val interpret: String
    val hermitInterpretation: String
    val hermitConsidering: String
    val aiLabel: String

    // Messages the view model raises
    val offlineInterpret: String
    fun limitRateLimited(retryAfterSeconds: Int?): String
    val limitAiDisabled: String
    val limitDone: String
    val failNetwork: String
    val failBlocked: String
    val failUnauthenticated: String
    val failOther: String
    val followUpLimit: String
}

object ThSiamSiStrings : SiamSiStrings {
    private val lang = AppLanguage.TH

    override fun gateLeft(left: Int) = "วันนี้เหลือ ${formatNumber(left, lang)} ครั้ง"
    override val gateLimit = "วันนี้เสี่ยงครบแล้ว กลับมาใหม่พรุ่งนี้"
    override val gateAdWatched = "ดูโฆษณาไว้แล้ว เสี่ยงได้เลย"
    override val gateNoAd = "ยังไม่มีโฆษณาในตอนนี้ เสี่ยงได้เลย"
    override val gateAdDismissed = "ต้องดูโฆษณาจนจบจึงจะเสี่ยงได้"
    override fun gateWatchAd(left: Int) = "ดูโฆษณาสั้น ๆ ก่อนเสี่ยง · วันนี้เหลือ ${formatNumber(left, lang)} ครั้ง"

    override fun setRow(name: String, count: Int) = "$name · ${formatNumber(count, lang)} ใบ"
    override val setSheetTitle = "เลือกตำราเซียมซี"
    override val setSheetSubtitle = "กลอนจากตำราดั้งเดิม · คำแปลโดยทีมอี้จิง AI"
    override fun setSource(source: String, count: Int) = "$source · ${formatNumber(count, lang)} ใบ"

    override val titleSiamsi = "เซียมซี"
    override val titleSlip = "ใบเซียมซี"
    override val titlePua = "เสี่ยงไม้ปัว"
    override val shareSlipDescription = "แชร์ใบเซียมซี"
    override fun stickLabel(number: Int) = "ไม้ที่ ${formatNumber(number, lang)}"
    override val confirmByPua = "เสี่ยงไม้ปัวยืนยัน"
    override val tossPua = "โยนไม้ปัว"
    override val shakeAgain = "เขย่าใหม่"
    override val openSlip = "เปิดใบเซียมซี"
    override val leaveDrawTitle = "ยังเสี่ยงไม่จบ ออกตอนนี้?"
    override val leaveDrawBody = "ไม้ที่เสี่ยงได้และผลไม้ปัวจะไม่ถูกเก็บไว้ สิทธิ์เสี่ยงครั้งนี้ยังอยู่ แต่เมื่อกลับมาจะต้องเขย่าใหม่และได้ไม้ใหม่"
    override val leaveInterpretTitle = "นักพรตยังแปลไม่จบ ออกตอนนี้?"
    override val leaveInterpretBody = "ใบเซียมซีนี้บันทึกในบันทึกชะตาแล้ว แต่คำแปลครั้งนี้จะไม่ถูกเก็บไว้"
    override val leaveResumableBody = "ใบเซียมซีนี้บันทึกในบันทึกชะตาแล้ว ท่านกลับมารับคำแปลจากนักพรตต่อได้ที่บันทึกชะตา โดยไม่เสียสิทธิ์ซ้ำ"
    override val stay = "อยู่ต่อ"
    override val leave = "ออก"
    override val prayInHeart = "อธิษฐานในใจ"

    override val holdToShake = "กดค้างเพื่อเขย่า"
    override val shakeHint = "เขย่าโทรศัพท์ หรือ กดค้างที่กระบอก"
    override val cylinderDescription = "กระบอกเซียมซี กดค้างเพื่อเขย่า"
    override val stickDrawn = "ไม้ที่ได้"
    override fun stickDrawnDescription(number: Int) = "ได้ไม้หมายเลข $number"
    override fun stickTitle(number: Int) = "ไม้เซียมซีที่ ${formatNumber(number, lang)}"

    override val tossPrompt = "แตะเพื่อโยนไม้ปัว ถามว่าใบนี้ใช่หรือไม่"
    override val puaRules = "ศักดิ์ = ยืนยัน · หัวเราะ / ไม่ = เขย่าใหม่ (ไม่นับโควตา)\nรอบที่ ๓ ยืนยันใบสุดท้ายให้อัตโนมัติ"
    override fun puaLabel(result: PuaResult) = when (result) {
        PuaResult.SAK -> "ศักดิ์ — คว่ำหนึ่ง หงายหนึ่ง ฟ้ายืนยันใบนี้"
        PuaResult.LAUGH -> "หัวเราะ — หงายทั้งคู่ เขย่าใหม่อีกครั้ง"
        PuaResult.NO -> "ไม่ — คว่ำทั้งคู่ เขย่าใหม่อีกครั้ง"
    }
    override fun puaHeadline(result: PuaResult, autoConfirmed: Boolean): Pair<String, String> = when {
        autoConfirmed -> "ครบสามรอบ · รับใบนี้" to "ไม้ปัวยังไม่ได้ศักดิ์ แต่ครบสามรอบแล้ว\nฟ้าให้ใบสุดท้ายนี้แก่คุณ"
        result == PuaResult.SAK -> "ศักดิ์ · ใช่ใบนี้" to "ไม้ปัวคว่ำหนึ่ง หงายหนึ่ง\nฟ้ารับคำอธิษฐานของคุณแล้ว"
        result == PuaResult.LAUGH -> "หัวเราะ · ยังไม่ใช่" to "ไม้ปัวหงายทั้งคู่\nเขย่าใหม่อีกครั้ง ไม่เสียสิทธิ์"
        else -> "ไม่ · ไม่ใช่ใบนี้" to "ไม้ปัวคว่ำทั้งคู่\nเขย่าใหม่อีกครั้ง ไม่เสียสิทธิ์"
    }
    override val blockUp = "หงาย"
    override val blockDown = "คว่ำ"
    override val tossLabel = "โยนไม้ปัว"
    override val blocksDescription = "ไม้ปัวสองอัน แตะเพื่อโยน"

    override val reflection = "เซียมซีเพื่อการไตร่ตรองและความบันเทิง โปรดใช้วิจารณญาณ"
    override fun slipHeader(shortName: String, number: Int) = "$shortName · ใบที่ ${formatNumber(number, lang)}"
    override val askMore = "ถามต่อ"
    override val shareSlip = "แชร์ใบ"
    override fun sourceLine(sourceTitle: String) = "ตำรา $sourceTitle · คำแปลโดยทีมอี้จิง AI"
    override val storyTitle = "เรื่องในตำนานของใบนี้"
    override val storyHide = "ซ่อนเรื่องในตำนาน"
    override val storyShow = "อ่านเรื่องในตำนาน"
    override val askHermit = "ให้นักพรตแปลความหมายตามคำถามของคุณ"
    override val costOne = "ใช้ ๑ สิทธิ์"
    override fun costLeft(left: Int) = "ใช้ ๑ สิทธิ์ · เหลือวันนี้ ${formatNumber(left, lang)}"
    override val interpret = "แปลความหมาย"
    override val hermitInterpretation = "คำแปลจากนักพรต"
    override val hermitConsidering = "นักพรตกำลังพิจารณาใบของท่าน..."
    override val aiLabel = "คำแปลสร้างโดย AI จากใบเซียมซี · เซียมซีเพื่อการไตร่ตรองและความบันเทิง โปรดใช้วิจารณญาณ ไม่ใช่คำแนะนำทางการแพทย์ กฎหมาย หรือการเงิน"

    override val offlineInterpret = "ต้องเชื่อมต่ออินเทอร์เน็ตเพื่อแปลความหมาย"
    override fun limitRateLimited(retryAfterSeconds: Int?) =
        "โปรดเว้นจังหวะหายใจสักครู่${retryAfterSeconds?.let { " (${it} วินาที)" } ?: ""} แล้วจึงขอให้นักพรตแปลอีกครั้ง"
    override val limitAiDisabled = "นักพรตปลีกวิเวกชั่วคราว ท่านยังอ่านใบเซียมซีด้านบนได้ โปรดกลับมาใหม่ภายหลัง"
    override val limitDone = "คำแปลจากนักพรตสำหรับวันนี้ครบแล้ว ท่านยังอ่านใบเซียมซีด้านบนได้เสมอ"
    override val failNetwork = "การเชื่อมต่อขาดหาย แตะแปลความหมายอีกครั้งได้ โดยไม่เสียสิทธิ์ซ้ำ"
    override val failBlocked = "คำถามนี้ไม่อาจแปลความได้ สิทธิ์ของท่านยังไม่ถูกใช้"
    override val failUnauthenticated = "ยืนยันตัวตนกับระบบไม่สำเร็จ โปรดลองใหม่อีกครั้ง"
    override val failOther = "การติดต่อนักพรตขัดข้องชั่วคราว สิทธิ์ของท่านยังไม่ถูกใช้ โปรดลองใหม่อีกครั้ง"
    override val followUpLimit = "ถามต่อครบจำนวนครั้งของคำแปลนี้แล้ว"
}

object EnSiamSiStrings : SiamSiStrings {
    override fun gateLeft(left: Int) = "$left left today"
    override val gateLimit = "You have used all of today's draws. Come back tomorrow."
    override val gateAdWatched = "Ad watched. You can draw now."
    override val gateNoAd = "No ad is ready right now. You can draw anyway."
    override val gateAdDismissed = "Watch the ad to the end to draw."
    override fun gateWatchAd(left: Int) = "Watch a short ad before drawing · $left left today"

    override fun setRow(name: String, count: Int) = "$name · $count slips"
    override val setSheetTitle = "Choose a slip book"
    override val setSheetSubtitle = "Verses from the original book · translated by the I Ching AI team"
    override fun setSource(source: String, count: Int) = "$source · $count slips"

    override val titleSiamsi = "Siam Si"
    override val titleSlip = "Siam Si slip"
    override val titlePua = "Moon blocks"
    override val shareSlipDescription = "Share the slip"
    override fun stickLabel(number: Int) = "Stick $number"
    override val confirmByPua = "Toss the blocks to confirm"
    override val tossPua = "Toss the blocks"
    override val shakeAgain = "Shake again"
    override val openSlip = "Open the slip"
    override val leaveDrawTitle = "The draw is not finished. Leave now?"
    override val leaveDrawBody = "Your stick and the block results will not be kept. Your draw is not lost, but you will shake again and get a new stick when you return."
    override val leaveInterpretTitle = "The hermit is not finished. Leave now?"
    override val leaveInterpretBody = "This slip is saved in your journal, but this interpretation will not be kept."
    override val leaveResumableBody = "This slip is saved in your journal. You can return there for the hermit's interpretation without using another reading."
    override val stay = "Stay"
    override val leave = "Leave"
    override val prayInHeart = "Hold your question in mind"

    override val holdToShake = "Hold to shake"
    override val shakeHint = "Shake your phone, or press and hold the cylinder"
    override val cylinderDescription = "Siam Si cylinder. Press and hold to shake"
    override val stickDrawn = "Your stick"
    override fun stickDrawnDescription(number: Int) = "You drew stick number $number"
    override fun stickTitle(number: Int) = "Siam Si stick $number"

    override val tossPrompt = "Tap to toss the moon blocks and ask whether this is your slip"
    override val puaRules = "Sacred = confirmed · Laughing / No = shake again (not counted against your quota)\nOn the 3rd round the last slip is confirmed automatically"
    override fun puaLabel(result: PuaResult) = when (result) {
        PuaResult.SAK -> "Sacred: one up, one down. Heaven confirms this slip"
        PuaResult.LAUGH -> "Laughing: both up. Shake again"
        PuaResult.NO -> "No: both down. Shake again"
    }
    override fun puaHeadline(result: PuaResult, autoConfirmed: Boolean): Pair<String, String> = when {
        autoConfirmed -> "Three rounds · take this slip" to "The blocks were not sacred, but three rounds are done.\nHeaven gives you this last slip."
        result == PuaResult.SAK -> "Sacred · this is your slip" to "One block up, one down.\nHeaven has received your prayer."
        result == PuaResult.LAUGH -> "Laughing · not yet" to "Both blocks face up.\nShake again. It does not use up your draw."
        else -> "No · not this slip" to "Both blocks face down.\nShake again. It does not use up your draw."
    }
    override val blockUp = "Up"
    override val blockDown = "Down"
    override val tossLabel = "Toss the blocks"
    override val blocksDescription = "Two moon blocks. Tap to toss"

    override val reflection = "Siam Si slips are for reflection and entertainment. Please use your own judgment."
    override fun slipHeader(shortName: String, number: Int) = "$shortName · Slip $number"
    override val askMore = "Ask more"
    override val shareSlip = "Share slip"
    override fun sourceLine(sourceTitle: String) = "Source: $sourceTitle · translated by the I Ching AI team"
    override val storyTitle = "The legend behind this slip"
    override val storyHide = "Hide the legend"
    override val storyShow = "Read the legend"
    override val askHermit = "Ask the hermit to interpret this slip for your question"
    override val costOne = "Uses 1 reading"
    override fun costLeft(left: Int) = "Uses 1 reading · $left left today"
    override val interpret = "Interpret"
    override val hermitInterpretation = "The hermit's interpretation"
    override val hermitConsidering = "The hermit is considering your slip..."
    override val aiLabel = "Interpretation generated by AI from the slip · Siam Si slips are for reflection and entertainment. Please use your own judgment. Not medical, legal or financial advice."

    override val offlineInterpret = "An internet connection is needed to interpret the slip."
    override fun limitRateLimited(retryAfterSeconds: Int?) =
        "Please take a breath${retryAfterSeconds?.let { " ($it seconds)" } ?: ""} and then ask the hermit to interpret again."
    override val limitAiDisabled = "The hermit is away for now. You can still read the slip above. Please come back later."
    override val limitDone = "Today's hermit interpretations are used up. You can always read the slip above."
    override val failNetwork = "The connection dropped. Tap Interpret to try again. It will not use another reading."
    override val failBlocked = "This question cannot be interpreted. Your reading has not been used."
    override val failUnauthenticated = "Signing in to the service failed. Please try again."
    override val failOther = "The hermit could not be reached for now. Your reading has not been used. Please try again."
    override val followUpLimit = "You have used all the follow-up questions for this interpretation."
}
