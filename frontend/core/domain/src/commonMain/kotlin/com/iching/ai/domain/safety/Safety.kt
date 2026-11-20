package com.iching.ai.domain.safety

import com.iching.ai.domain.model.AppLanguage

// A copy of backend/functions/src/core/safety.ts (crisis lists and replies only), so a question that
// never reaches the server (a Siam Si draw before any interpretation) still gets the hotlines.
// backend/functions/test/core.test.ts checks that both copies stay the same.

private val SELF_HARM = listOf(
    "ฆ่าตัวตาย", "อยากตาย", "อยากหายไปจากโลก", "ไม่อยากมีชีวิต", "ไม่อยากอยู่แล้ว", "ไม่อยากตื่น", "จบชีวิต",
    "ปลิดชีพ", "ทำร้ายตัวเอง", "ทำร้ายตนเอง", "กรีดข้อมือ", "กรีดแขน", "กินยาตาย", "กินยาฆ่าตัว", "ผูกคอ", "โดดตึก",
    "กระโดดตึก", "โดดสะพาน", "ตายไปซะ", "อยู่ไปก็ไร้ค่า",
)

private val MEDICAL_EMERGENCY = listOf(
    "เจ็บหน้าอก", "แน่นหน้าอก", "หายใจไม่ออก", "หมดสติ", "ชักเกร็ง", "เลือดออกไม่หยุด", "กินยาเกินขนาด", "ยาเกินขนาด",
    "แพ้ยารุนแรง", "ปากเบี้ยว", "แขนขาอ่อนแรง", "โดนงูกัด",
)

/**
 * English phrases as regular-expression sources, matched whole-word on [normalizeEnglishForSafety]'s text (same lists
 * as the server's SELF_HARM_EN / MEDICAL_EMERGENCY_EN). Every Latin-script phrase lives here; the Thai lists above hold
 * Thai script only. Both languages are always checked, whatever language the app shows.
 */
private val SELF_HARM_EN = listOf(
    "kill(ing)? myself", "suicid(e|es|al)", "end(ing)? my (own )?life", "end(ing)? it all", "take my (own )?life",
    "want(ed)? to die", "wanna die", "wish i (was|were) dead", "better off dead", "(dont|do not) want to (live|be alive)",
    "self harm(ing|ed)?", "(hurt|hurting|harm|harming|cut|cutting) myself",
)

private val MEDICAL_EMERGENCY_EN = listOf(
    "(cant|cannot|can not) breathe?", "not breathing", "chest pains?", "heart attack", "stroke", "unconscious(ness)?",
    "severe bleeding", "bleeding (heavily|badly)", "wont stop bleeding", "seizures?", "overdos(e|ed|ing)",
    "anaphyla(xis|ctic)", "snake bite", "bitten by a snake",
)

const val SELF_HARM_MESSAGE = """ผู้แสวงหาเอ๋ย ถ้อยคำของท่านบอกผู้เฒ่าว่าใจของท่านกำลังแบกสิ่งที่หนักเหลือเกิน ในยามนี้คัมภีร์ขอวางไว้ก่อน เพราะสิ่งสำคัญที่สุดคือตัวท่านเอง

ท่านไม่จำเป็นต้องผ่านคืนนี้ตามลำพัง โปรดพูดคุยกับใครสักคนที่ไว้ใจได้ หรือโทรหาสายด่วนสุขภาพจิต 1323 (ตลอด 24 ชั่วโมง ไม่เสียค่าใช้จ่าย) หากรู้สึกว่าตนเองอาจไม่ปลอดภัยในตอนนี้ โปรดโทร 1669 หรือไปโรงพยาบาลที่ใกล้ที่สุดทันที

ดุจสายน้ำที่ไหลผ่านช่วงแคบก่อนจะถึงที่ราบกว้าง ความรู้สึกในยามนี้ไม่ใช่ทั้งหมดของชีวิตท่าน"""

const val MEDICAL_MESSAGE = """ผู้แสวงหาเอ๋ย สิ่งที่ท่านเล่ามาอาจเป็นเรื่องเร่งด่วนของร่างกาย ซึ่งไม่ควรรอคำตอบจากคัมภีร์

โปรดโทรแจ้งเหตุฉุกเฉินทางการแพทย์ 1669 หรือไปโรงพยาบาลที่ใกล้ที่สุดโดยเร็ว เมื่อกายปลอดภัยแล้ว ค่อยกลับมาพินิจคัมภีร์ด้วยใจที่สงบ"""

const val SELF_HARM_MESSAGE_EN = """Seeker, your words tell me your heart is carrying something very heavy. For now, let us set the scripture aside, because what matters most is you.

You do not have to get through tonight alone. Please talk to someone you trust, or reach out to a helpline; findahelpline.com shows free, confidential helplines in your country. If you feel you may not be safe right now, please call your local emergency number or go to the nearest hospital straight away.

Like a river that passes through a narrow gorge before it reaches the open plain, what you feel now is not the whole of your life."""

const val MEDICAL_MESSAGE_EN = """Seeker, what you describe may be urgent for your body, and it should not wait for an answer from the scripture.

Please call your local emergency number or go to the nearest hospital now. If you are unsure whom to call, findahelpline.com can help you find a local helpline. Once your body is safe, come back and reflect on the scripture with a calm mind."""

/** Lower-case and drop spaces / zero-width marks so "อยาก ตาย" and "Kill  myself" still match. */
private val IGNORED = Regex("[\\s\u200B\u200C\u200D\u2060'’.,!?-]")

fun normalizeForSafety(text: String): String = text.lowercase().replace(IGNORED, "")

private val NOT_ALNUM = Regex("[^a-z0-9]+")

/** Lower-case, drop apostrophes ("can't" → "cant"), turn every other run of non-alphanumerics into one space. */
fun normalizeEnglishForSafety(text: String): String =
    " " + text.lowercase().replace("'", "").replace("’", "").replace(NOT_ALNUM, " ").trim() + " "

/** A space inside a phrase is optional ("killmyself" still matches), but the phrase must start and end on a word break. */
private fun wholeWords(phrases: List<String>) =
    Regex("\\b(?:" + phrases.joinToString("|") { it.replace(" ", " ?") } + ")\\b")

private val SELF_HARM_EN_RE = wholeWords(SELF_HARM_EN)
private val MEDICAL_EMERGENCY_EN_RE = wholeWords(MEDICAL_EMERGENCY_EN)

/**
 * The caring reply when [text] signals a crisis, otherwise null. Thai and English phrases are both checked whatever
 * [language] is; [language] only picks the reply's language (the English one never offers the Thai numbers).
 */
fun crisisMessage(text: String, language: AppLanguage = AppLanguage.TH): String? {
    val t = normalizeForSafety(text)
    val e = normalizeEnglishForSafety(text)
    val en = language == AppLanguage.EN
    return when {
        SELF_HARM.any { it in t } || SELF_HARM_EN_RE.containsMatchIn(e) -> if (en) SELF_HARM_MESSAGE_EN else SELF_HARM_MESSAGE
        MEDICAL_EMERGENCY.any { it in t } || MEDICAL_EMERGENCY_EN_RE.containsMatchIn(e) -> if (en) MEDICAL_MESSAGE_EN else MEDICAL_MESSAGE
        else -> null
    }
}
