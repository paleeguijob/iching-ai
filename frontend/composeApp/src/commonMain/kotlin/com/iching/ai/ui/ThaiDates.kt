package com.iching.ai.ui

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

private val THAI_MONTHS = listOf(
    "มกราคม", "กุมภาพันธ์", "มีนาคม", "เมษายน", "พฤษภาคม", "มิถุนายน",
    "กรกฎาคม", "สิงหาคม", "กันยายน", "ตุลาคม", "พฤศจิกายน", "ธันวาคม",
)

/** e.g. "30 ตุลาคม 2569" (Buddhist era). */
fun formatThaiDate(epochMillis: Long): String {
    val dt = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.currentSystemDefault())
    return "${dt.day} ${THAI_MONTHS[dt.month.ordinal]} ${dt.year + 543}"
}

private val THAI_MONTHS_SHORT = listOf(
    "ม.ค.", "ก.พ.", "มี.ค.", "เม.ย.", "พ.ค.", "มิ.ย.",
    "ก.ค.", "ส.ค.", "ก.ย.", "ต.ค.", "พ.ย.", "ธ.ค.",
)

/** e.g. "๖ พ.ย." — day in Thai numerals and the short month, for compact journal rows. */
fun formatThaiShortDate(epochMillis: Long): String {
    val dt = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.currentSystemDefault())
    return "${dt.day.toThaiNumerals()} ${THAI_MONTHS_SHORT[dt.month.ordinal]}"
}

/** e.g. "14:05 น." */
fun formatThaiTime(epochMillis: Long): String {
    val dt = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.currentSystemDefault())
    return "${dt.hour.toString().padStart(2, '0')}:${dt.minute.toString().padStart(2, '0')} น."
}

/** Month heading for the journal timeline, e.g. "ตุลาคม 2569". */
fun formatThaiMonth(epochMillis: Long): String {
    val dt = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.currentSystemDefault())
    return "${THAI_MONTHS[dt.month.ordinal]} ${dt.year + 543}"
}

private val THAI_DIGITS = "๐๑๒๓๔๕๖๗๘๙"

/** Renders a number with Thai numerals for decorative use, e.g. 47 → "๔๗". */
fun Int.toThaiNumerals(): String = toString().map { THAI_DIGITS[it - '0'] }.joinToString("")
