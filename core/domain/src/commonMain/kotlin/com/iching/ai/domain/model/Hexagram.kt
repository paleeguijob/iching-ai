package com.iching.ai.domain.model

/**
 * One of the 64 hexagrams with its classical text.
 * [pattern] has six characters read bottom to top (index 0 = line 1), '1' = yang, '0' = yin.
 * [lines] holds the six line statements, bottom to top.
 */
data class Hexagram(
    val id: Int,
    val nameCn: String,
    val pinyin: String,
    val nameTh: String,
    val pattern: String,
    val keywords: List<String>,
    val judgment: String,
    val image: String,
    val description: String,
    val lines: List<String>,
    /** Extra statement used when all six lines change (only hexagrams 1 and 2). */
    val allLines: String? = null,
) {
    val lowerTrigram: Trigram get() = Trigram.fromPattern(pattern.substring(0, 3))
    val upperTrigram: Trigram get() = Trigram.fromPattern(pattern.substring(3, 6))

    /** Thai name without the meaning part, e.g. "ขุน" from "ขุน — ความตีบตัน, ถูกล้อม". */
    val shortNameTh: String get() = nameTh.substringBefore(" —").trim()

    /** Meaning part of the Thai name, e.g. "ความตีบตัน, ถูกล้อม". */
    val meaningTh: String get() = nameTh.substringAfter("— ", nameTh).trim()
}

/** Result of six tosses resolved against the scripture. */
data class CastResult(
    /** Six line values, bottom to top. */
    val lines: List<LineType>,
    val primary: Hexagram,
    /** The hexagram the situation is changing into; null when no line changes. */
    val relating: Hexagram?,
) {
    /** 1-based positions of changing lines, bottom to top. */
    val changingPositions: List<Int>
        get() = lines.mapIndexedNotNull { index, line -> if (line.isChanging) index + 1 else null }
}

enum class InterpretationSource { AI, SCRIPTURE }

data class Reading(
    val id: Long = 0,
    val question: String,
    val createdAtMillis: Long,
    val lines: List<LineType>,
    val primaryId: Int,
    val relatingId: Int?,
    val interpretation: String,
    val source: InterpretationSource,
) {
    val changingPositions: List<Int>
        get() = lines.mapIndexedNotNull { index, line -> if (line.isChanging) index + 1 else null }
}

data class AppSettings(
    val apiKey: String = "",
    val model: String = DEFAULT_MODEL,
    val soundEnabled: Boolean = true,
) {
    val hasApiKey: Boolean get() = apiKey.isNotBlank()

    companion object {
        const val DEFAULT_MODEL = "gemini-flash-latest"
    }
}
