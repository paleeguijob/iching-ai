package com.iching.ai.domain.oracle

import com.iching.ai.domain.model.CastResult
import com.iching.ai.domain.model.Hexagram

data class OraclePrompt(
    val systemInstruction: String,
    val userMessage: String,
)

private val THAI_LINE_NAMES = listOf("หนึ่ง", "สอง", "สาม", "สี่", "ห้า", "หก")

fun linePositionTh(position: Int): String = "เส้นที่ ${THAI_LINE_NAMES[position - 1]}"

/**
 * Builds the RAG prompt: persona (system) + scripture context + the seeker's question.
 * The scripture block is the only source of meaning the model is allowed to use.
 */
object OraclePromptBuilder {

    val PERSONA: String = """
        คุณคือนักพรตเต๋าผู้เร้นกายบนยอดเขาอู่ตัง มีชีวิตอยู่ด้วยความสมถะและเข้าใจสัจธรรมของการเปลี่ยนแปลง (เต๋า) หน้าที่ของคุณคือการแปลผล 'คัมภีร์อี้จิง' ที่จับยามได้ ให้เข้ากับคำถามของผู้ใช้

        กฎการตอบ (Guidelines):
        1. ใช้สรรพนามแทนตัวเองว่า 'อาตมา' หรือ 'ผู้เฒ่า' และเรียกผู้ใช้ว่า 'ท่าน' หรือ 'ผู้แสวงหา'
        2. น้ำเสียงต้องสงบ เมตตา ลุ่มลึก แฝงปรัชญาธรรมชาติ (น้ำ, ลม, ภูเขา) และไม่ตัดสินถูกผิด
        3. ห้ามใช้คำศัพท์เทคโนโลยี หรือบอกว่าตนเองคือ AI เด็ดขาด
        4. อ้างอิงความหมายดั้งเดิมของ [ฉักกะ/กวา] ที่ระบบส่งให้ (Context Data) เป็นแก่นหลักในการตอบเสมอ ห้ามแต่งความหมายของกวาเอง
        5. โครงสร้าง: เริ่มด้วยคำเปรียบเปรยถึงธรรมชาติ -> แปลความหมายกวาให้เข้ากับคำถาม -> สรุปคำแนะนำให้ก้าวต่อไปด้วยความว่างและสมดุล

        รูปแบบการเขียน:
        - ตอบเป็นภาษาไทยล้วน เป็นร้อยแก้ว 3-5 ย่อหน้า ความยาวรวมประมาณ 180-320 คำ
        - ไม่ใช้หัวข้อ สัญลักษณ์ markdown ตัวหนา หรือรายการแบบบูลเล็ต
        - เอ่ยชื่อกวาเป็นภาษาไทยอย่างน้อยหนึ่งครั้ง และหากมีเส้นแปร ให้เชื่อมความหมายของเส้นแปรและกวาแปรเข้ากับคำถามด้วย
        - หากคำถามเกี่ยวกับสุขภาพ กฎหมาย หรือการเงินที่ร้ายแรง ให้ชี้แนะด้วยเมตตาว่าควรปรึกษาผู้รู้ในทางโลกควบคู่กันไป
    """.trimIndent()

    fun build(question: String, cast: CastResult): OraclePrompt {
        val context = buildString {
            appendLine("[Context Data — คัมภีร์อี้จิงที่จับยามได้]")
            appendHexagram("กวาหลัก", cast.primary)
            val changing = cast.changingPositions
            if (changing.isEmpty()) {
                appendLine("เส้นแปร: ไม่มี — ให้ยึดคำตัดสินและภาพลักษณ์ของกวาหลักเป็นแก่น")
            } else {
                appendLine("เส้นแปร (${changing.size} เส้น) — ความหมายเฉพาะที่ต้องเน้น:")
                changing.forEach { position ->
                    appendLine("- ${linePositionTh(position)}: ${cast.primary.lines[position - 1]}")
                }
                if (changing.size == 6 && cast.primary.allLines != null) {
                    appendLine("- เมื่อเส้นทั้งหกแปรพร้อมกัน: ${cast.primary.allLines}")
                }
            }
            cast.relating?.let {
                appendLine()
                appendHexagram("กวาแปร (ทิศทางที่สถานการณ์กำลังคลี่คลายไป)", it, includeDescription = false)
            }
        }
        val user = buildString {
            append(context.trim())
            appendLine()
            appendLine()
            appendLine("[คำถามของผู้แสวงหา]")
            appendLine(question.trim().ifEmpty { "ผู้แสวงหามิได้เอ่ยคำถาม เพียงตั้งจิตอธิษฐานขอคำชี้แนะในวิถีชีวิตยามนี้" })
            appendLine()
            append("จงแปลผลคัมภีร์ข้างต้นให้เข้ากับคำถามนี้ ตามกฎการตอบอย่างเคร่งครัด")
        }
        return OraclePrompt(systemInstruction = PERSONA, userMessage = user)
    }

    private fun StringBuilder.appendHexagram(label: String, hexagram: Hexagram, includeDescription: Boolean = true) {
        appendLine("$label: กวาที่ ${hexagram.id} ${hexagram.nameCn} (${hexagram.pinyin}) — ${hexagram.nameTh}")
        appendLine("ตรีแกรม: บน ${hexagram.upperTrigram.natureTh} / ล่าง ${hexagram.lowerTrigram.natureTh}")
        appendLine("คำตัดสิน: ${hexagram.judgment}")
        appendLine("ภาพลักษณ์: ${hexagram.image}")
        if (includeDescription) appendLine("ภาพรวม: ${hexagram.description}")
    }
}

/**
 * Composes a readable reading straight from the scripture, used when the
 * language model cannot be reached (no key, offline, quota...).
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
