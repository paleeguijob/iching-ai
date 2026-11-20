package com.iching.ai.ui

import com.iching.ai.domain.model.AppLanguage
import com.iching.ai.ui.strings.formatDate
import com.iching.ai.ui.strings.formatNumber
import com.iching.ai.ui.strings.formatShortDate
import com.iching.ai.ui.strings.formatTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

private val THAI_MONTHS = listOf(
    "มกราคม", "กุมภาพันธ์", "มีนาคม", "เมษายน", "พฤษภาคม", "มิถุนายน",
    "กรกฎาคม", "สิงหาคม", "กันยายน", "ตุลาคม", "พฤศจิกายน", "ธันวาคม",
)

/** e.g. "30 ตุลาคม 2569" (Buddhist era). */
fun formatThaiDate(epochMillis: Long): String = formatDate(epochMillis, AppLanguage.TH)

/** e.g. "๖ พ.ย." — day in Thai numerals and the short month, for compact journal rows. */
fun formatThaiShortDate(epochMillis: Long, timeZone: TimeZone = TimeZone.currentSystemDefault()): String =
    formatShortDate(epochMillis, AppLanguage.TH, timeZone)

/** e.g. "14:05 น." */
fun formatThaiTime(epochMillis: Long): String = formatTime(epochMillis, AppLanguage.TH)

/** Month heading for the journal timeline, e.g. "ตุลาคม 2569". */
fun formatThaiMonth(epochMillis: Long): String {
    val dt = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.currentSystemDefault())
    return "${THAI_MONTHS[dt.month.ordinal]} ${dt.year + 543}"
}

/** Renders a number with Thai numerals for decorative use, e.g. 47 → "๔๗". */
fun Int.toThaiNumerals(): String = formatNumber(this, AppLanguage.TH)
