package com.iching.ai.ui

import com.iching.ai.domain.model.AppLanguage
import com.iching.ai.ui.strings.formatDate
import com.iching.ai.ui.strings.formatNumber
import com.iching.ai.ui.strings.formatShortDate
import com.iching.ai.ui.strings.formatTime
import kotlinx.datetime.TimeZone

/** e.g. "30 ตุลาคม 2569" (Buddhist era). */
fun formatThaiDate(epochMillis: Long): String = formatDate(epochMillis, AppLanguage.TH)

/** e.g. "๖ พ.ย." — day in Thai numerals and the short month, for compact journal rows. */
fun formatThaiShortDate(epochMillis: Long, timeZone: TimeZone = TimeZone.currentSystemDefault()): String =
    formatShortDate(epochMillis, AppLanguage.TH, timeZone)

/** e.g. "14:05 น." */
fun formatThaiTime(epochMillis: Long): String = formatTime(epochMillis, AppLanguage.TH)

/** Renders a number with Thai numerals for decorative use, e.g. 47 → "๔๗". */
fun Int.toThaiNumerals(): String = formatNumber(this, AppLanguage.TH)
