package com.iching.ai.ui.strings

import com.iching.ai.domain.model.AppLanguage
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

private val THAI_MONTHS = listOf(
    "มกราคม", "กุมภาพันธ์", "มีนาคม", "เมษายน", "พฤษภาคม", "มิถุนายน",
    "กรกฎาคม", "สิงหาคม", "กันยายน", "ตุลาคม", "พฤศจิกายน", "ธันวาคม",
)

private val THAI_MONTHS_SHORT = listOf(
    "ม.ค.", "ก.พ.", "มี.ค.", "เม.ย.", "พ.ค.", "มิ.ย.",
    "ก.ค.", "ส.ค.", "ก.ย.", "ต.ค.", "พ.ย.", "ธ.ค.",
)

private val EN_MONTHS_SHORT = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

private val THAI_DIGITS = "๐๑๒๓๔๕๖๗๘๙"

private val ZODIAC_TH = listOf("ชวด", "ฉลู", "ขาล", "เถาะ", "มะโรง", "มะเส็ง", "มะเมีย", "มะแม", "วอก", "ระกา", "จอ", "กุน")
private val ZODIAC_EN = listOf("Rat", "Ox", "Tiger", "Rabbit", "Dragon", "Snake", "Horse", "Goat", "Monkey", "Rooster", "Dog", "Pig")

private fun local(epochMillis: Long, tz: TimeZone) =
    Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(tz)

/** Thai uses Thai numerals (47 → "๔๗"), English plain digits. */
fun formatNumber(n: Int, lang: AppLanguage): String = when (lang) {
    AppLanguage.TH -> n.toString().map { if (it in '0'..'9') THAI_DIGITS[it - '0'] else it }.joinToString("")
    AppLanguage.EN -> n.toString()
}

/** TH "30 ตุลาคม 2569" (Buddhist era), EN "30 Oct 2026". */
fun formatDate(epochMillis: Long, lang: AppLanguage, tz: TimeZone = TimeZone.currentSystemDefault()): String {
    val dt = local(epochMillis, tz)
    return when (lang) {
        AppLanguage.TH -> "${dt.day} ${THAI_MONTHS[dt.month.ordinal]} ${dt.year + 543}"
        AppLanguage.EN -> "${dt.day} ${EN_MONTHS_SHORT[dt.month.ordinal]} ${dt.year}"
    }
}

/** TH "๖ พ.ย." (compact journal rows), EN "6 Nov 2026". */
fun formatShortDate(epochMillis: Long, lang: AppLanguage, tz: TimeZone = TimeZone.currentSystemDefault()): String {
    val dt = local(epochMillis, tz)
    return when (lang) {
        AppLanguage.TH -> "${formatNumber(dt.day, lang)} ${THAI_MONTHS_SHORT[dt.month.ordinal]}"
        AppLanguage.EN -> "${dt.day} ${EN_MONTHS_SHORT[dt.month.ordinal]} ${dt.year}"
    }
}

/** TH "14:05 น.", EN "14:05". */
fun formatTime(epochMillis: Long, lang: AppLanguage, tz: TimeZone = TimeZone.currentSystemDefault()): String {
    val dt = local(epochMillis, tz)
    val hm = "${dt.hour.toString().padStart(2, '0')}:${dt.minute.toString().padStart(2, '0')}"
    return if (lang == AppLanguage.TH) "$hm น." else hm
}

/** TH "มะแม ๒๕๗๐" (callers add the "กวาประจำปี" prefix), EN "Year of the Goat 2027". */
fun zodiacYearLabel(year: Int, lang: AppLanguage): String {
    val i = (year - 4).mod(12)
    return when (lang) {
        AppLanguage.TH -> "${ZODIAC_TH[i]} ${formatNumber(year + 543, lang)}"
        AppLanguage.EN -> "Year of the ${ZODIAC_EN[i]} $year"
    }
}
