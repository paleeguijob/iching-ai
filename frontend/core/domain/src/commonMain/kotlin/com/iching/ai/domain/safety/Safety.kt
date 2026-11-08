package com.iching.ai.domain.safety

// A copy of backend/functions/src/core/safety.ts (crisis lists and replies only), so a question that
// never reaches the server (a Siam Si draw before any interpretation) still gets the hotlines.
// backend/functions/test/core.test.ts checks that both copies stay the same.

private val SELF_HARM = listOf(
    "ฆ่าตัวตาย", "อยากตาย", "อยากหายไปจากโลก", "ไม่อยากมีชีวิต", "ไม่อยากอยู่แล้ว", "ไม่อยากตื่น", "จบชีวิต",
    "ปลิดชีพ", "ทำร้ายตัวเอง", "ทำร้ายตนเอง", "กรีดข้อมือ", "กรีดแขน", "กินยาตาย", "กินยาฆ่าตัว", "ผูกคอ", "โดดตึก",
    "กระโดดตึก", "โดดสะพาน", "ตายไปซะ", "อยู่ไปก็ไร้ค่า", "suicide", "killmyself", "endmylife", "selfharm",
    "wanttodie", "cutmyself",
)

private val MEDICAL_EMERGENCY = listOf(
    "เจ็บหน้าอก", "แน่นหน้าอก", "หายใจไม่ออก", "หมดสติ", "ชักเกร็ง", "เลือดออกไม่หยุด", "กินยาเกินขนาด", "ยาเกินขนาด",
    "แพ้ยารุนแรง", "ปากเบี้ยว", "แขนขาอ่อนแรง", "โดนงูกัด", "overdose", "heartattack", "stroke", "cantbreathe",
)

const val SELF_HARM_MESSAGE = """ผู้แสวงหาเอ๋ย ถ้อยคำของท่านบอกผู้เฒ่าว่าใจของท่านกำลังแบกสิ่งที่หนักเหลือเกิน ในยามนี้คัมภีร์ขอวางไว้ก่อน เพราะสิ่งสำคัญที่สุดคือตัวท่านเอง

ท่านไม่จำเป็นต้องผ่านคืนนี้ตามลำพัง โปรดพูดคุยกับใครสักคนที่ไว้ใจได้ หรือโทรหาสายด่วนสุขภาพจิต 1323 (ตลอด 24 ชั่วโมง ไม่เสียค่าใช้จ่าย) หากรู้สึกว่าตนเองอาจไม่ปลอดภัยในตอนนี้ โปรดโทร 1669 หรือไปโรงพยาบาลที่ใกล้ที่สุดทันที

ดุจสายน้ำที่ไหลผ่านช่วงแคบก่อนจะถึงที่ราบกว้าง ความรู้สึกในยามนี้ไม่ใช่ทั้งหมดของชีวิตท่าน"""

const val MEDICAL_MESSAGE = """ผู้แสวงหาเอ๋ย สิ่งที่ท่านเล่ามาอาจเป็นเรื่องเร่งด่วนของร่างกาย ซึ่งไม่ควรรอคำตอบจากคัมภีร์

โปรดโทรแจ้งเหตุฉุกเฉินทางการแพทย์ 1669 หรือไปโรงพยาบาลที่ใกล้ที่สุดโดยเร็ว เมื่อกายปลอดภัยแล้ว ค่อยกลับมาพินิจคัมภีร์ด้วยใจที่สงบ"""

/** Lower-case and drop spaces / zero-width marks so "อยาก ตาย" and "Kill  myself" still match. */
private val IGNORED = Regex("[\\s\u200B\u200C\u200D\u2060'’.,!?-]")

fun normalizeForSafety(text: String): String = text.lowercase().replace(IGNORED, "")

/** The caring reply with Thai hotlines when [text] signals a crisis, otherwise null. */
fun crisisMessage(text: String): String? {
    val t = normalizeForSafety(text)
    return when {
        SELF_HARM.any { it in t } -> SELF_HARM_MESSAGE
        MEDICAL_EMERGENCY.any { it in t } -> MEDICAL_MESSAGE
        else -> null
    }
}
