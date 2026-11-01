package com.iching.ai.domain.oracle

import com.iching.ai.domain.model.CastResult

private val THAI_LINE_NAMES = listOf("หนึ่ง", "สอง", "สาม", "สี่", "ห้า", "หก")

fun linePositionTh(position: Int): String = "เส้นที่ ${THAI_LINE_NAMES[position - 1]}"

/**
 * Composes a readable reading straight from the scripture. Shown when the AI reading is not
 * available (quota used up, offline, server error). The AI prompt itself is built on the server
 * (functions/src/core/prompt.ts) so the persona and API key never ship inside the app.
 */
object ScriptureReadingComposer {
    fun compose(cast: CastResult): String = buildString {
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
