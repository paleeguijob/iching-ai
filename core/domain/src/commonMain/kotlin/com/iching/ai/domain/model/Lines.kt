package com.iching.ai.domain.model

/** One face of a coin. Traditional three-coin method: heads (yang side) = 3, tails (yin side) = 2. */
enum class CoinFace(val value: Int) {
    HEADS(3),
    TAILS(2),
}

/**
 * A single line produced by one toss of three coins.
 * 6 = old yin (changing), 7 = young yang, 8 = young yin, 9 = old yang (changing).
 */
enum class LineType(val value: Int, val isYang: Boolean, val isChanging: Boolean) {
    OLD_YIN(6, isYang = false, isChanging = true),
    YOUNG_YANG(7, isYang = true, isChanging = false),
    YOUNG_YIN(8, isYang = false, isChanging = false),
    OLD_YANG(9, isYang = true, isChanging = true);

    /** Whether this line is yang after transformation into the relating hexagram. */
    val isYangAfterChange: Boolean get() = if (isChanging) !isYang else isYang

    companion object {
        fun fromValue(value: Int): LineType =
            entries.firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Line value must be 6..9, was $value")
    }
}

data class CoinToss(val faces: List<CoinFace>) {
    init {
        require(faces.size == 3) { "A toss uses exactly three coins" }
    }

    val line: LineType get() = LineType.fromValue(faces.sumOf { it.value })
}

/** The eight trigrams. [pattern] is read bottom to top, '1' = yang. */
enum class Trigram(val pattern: String, val symbol: String, val nameCn: String, val natureTh: String) {
    QIAN("111", "☰", "乾", "ฟ้า"),
    DUI("110", "☱", "兌", "ทะเลสาบ"),
    LI("101", "☲", "離", "ไฟ"),
    ZHEN("100", "☳", "震", "ฟ้าร้อง"),
    XUN("011", "☴", "巽", "ลม"),
    KAN("010", "☵", "坎", "น้ำ"),
    GEN("001", "☶", "艮", "ภูเขา"),
    KUN("000", "☷", "坤", "ดิน");

    companion object {
        fun fromPattern(pattern: String): Trigram =
            entries.first { it.pattern == pattern }
    }
}
