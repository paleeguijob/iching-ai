package com.iching.ai.domain.oracle

import com.iching.ai.domain.model.AppLanguage
import com.iching.ai.domain.model.CastResult
import com.iching.ai.domain.model.Hexagram

private val THAI_LINE_NAMES = listOf("หนึ่ง", "สอง", "สาม", "สี่", "ห้า", "หก")

fun linePositionTh(position: Int): String = "เส้นที่ ${THAI_LINE_NAMES[position - 1]}"

/** Same wording as the English LibraryStrings.linePosition. */
fun linePositionEn(position: Int): String = "Line $position"

/**
 * The English meaning without a leading copy of the name ("Wind, gentle penetration" -> "gentle penetration"; "Waiting" -> "").
 * The name is stripped only when a separator or the end follows, so "Windy plains" stays whole.
 */
fun englishMeaningAfterName(nameEn: String, meaning: String): String {
    if (nameEn.isBlank() || !meaning.startsWith(nameEn)) return meaning
    val rest = meaning.substring(nameEn.length)
    if (rest.isNotEmpty() && rest[0] !in ",:—- ") return meaning
    return rest.trimStart(',', ':', '—', '-', ' ')
}

/** Blank [Hexagram.nameEn] means the content is Thai (ruling R25): the Thai sentences are used then. */
private val Hexagram.hasEnglish: Boolean get() = nameEn.isNotBlank()

/**
 * Composes a readable reading straight from the scripture. Shown when the AI reading is not
 * available (quota used up, offline, server error). The AI prompt itself is built on the server
 * (functions/src/core/prompt.ts) so the persona and API key never ship inside the app.
 */
object ScriptureReadingComposer {
    fun compose(cast: CastResult, lang: AppLanguage = AppLanguage.TH): String =
        if (lang == AppLanguage.EN && cast.primary.hasEnglish) composeEn(cast) else composeTh(cast)

    private fun Hexagram.headerEn(): String =
        englishMeaningAfterName(nameEn, meaningTh).let { rest -> if (rest.isEmpty()) "" else ": $rest" }

    private fun composeEn(cast: CastResult): String = buildString {
        val primary = cast.primary
        appendLine("The Yijing opens hexagram ${primary.id}, \"${primary.nameEn}\" (${primary.nameCn})${primary.headerEn()}")
        appendLine()
        appendLine(primary.judgment)
        appendLine()
        appendLine(primary.image)
        appendLine()
        appendLine(primary.description)
        val changing = cast.changingPositions
        if (changing.isNotEmpty()) {
            appendLine()
            appendLine("Changing lines")
            changing.forEach { position ->
                appendLine("${linePositionEn(position)} — ${primary.lines[position - 1]}")
            }
            if (changing.size == 6) primary.allLines?.let { appendLine(it) }
        }
        cast.relating?.let { relating ->
            appendLine()
            appendLine("The situation is moving toward hexagram ${relating.id}, \"${relating.nameEn}\" (${relating.nameCn})${relating.headerEn()}")
            appendLine(relating.judgment)
        }
    }.trim()

    private fun composeTh(cast: CastResult): String = buildString {
        val primary = cast.primary
        appendLine("คัมภีร์อี้จิงเปิดเผยกวาที่ ${primary.id} \"${primary.shortNameTh}\" (${primary.nameCn}) อันหมายถึง${primary.meaningTh}")
        appendLine()
        appendLine(primary.judgment)
        appendLine()
        appendLine(primary.image)
        appendLine()
        appendLine(primary.description)
        val changing = cast.changingPositions
        if (changing.isNotEmpty()) {
            appendLine()
            appendLine("เส้นแปรที่ปรากฏ")
            changing.forEach { position ->
                appendLine("${linePositionTh(position)} — ${primary.lines[position - 1]}")
            }
            if (changing.size == 6) primary.allLines?.let { appendLine(it) }
        }
        cast.relating?.let { relating ->
            appendLine()
            appendLine("สถานการณ์กำลังเคลื่อนสู่กวาที่ ${relating.id} \"${relating.shortNameTh}\" (${relating.nameCn}) — ${relating.meaningTh}")
            appendLine(relating.judgment)
        }
    }.trim()
}
