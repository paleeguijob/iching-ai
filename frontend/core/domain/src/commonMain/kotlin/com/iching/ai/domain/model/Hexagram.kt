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
    /** Pre-written reflection for the day this hexagram is the daily one (≤ 120 chars, also the push body). */
    val dailyMessage: String = "",
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

/** The seeker's answer to "did it come true?" in the Destiny Journal. */
enum class Outcome { YES, PARTLY, NO }

data class FollowUpTurn(val question: String, val answer: String)

data class Reading(
    val id: Long = 0,
    val question: String,
    val createdAtMillis: Long,
    val lines: List<LineType>,
    val primaryId: Int,
    val relatingId: Int?,
    val interpretation: String,
    val source: InterpretationSource,
    /** Id of the reading on the server; null for offline casts that never reached it. */
    val serverId: String? = null,
    val category: QuestionCategory? = null,
    val outcome: Outcome? = null,
    val outcomeNote: String? = null,
    val followUps: List<FollowUpTurn> = emptyList(),
) {
    val changingPositions: List<Int>
        get() = lines.mapIndexedNotNull { index, line -> if (line.isChanging) index + 1 else null }
}

/** Question categories (also sent to the server to steer the prompt). [wire] is the API value. */
enum class QuestionCategory(val wire: String, val labelTh: String, val sampleTh: String) {
    WORK("work", "การงาน", "การงานของข้าพเจ้าช่วงนี้ควรดำเนินไปอย่างไร"),
    LOVE("love", "ความรัก", "ความสัมพันธ์ของข้าพเจ้าจะเป็นไปในทิศทางใด"),
    MONEY("money", "การเงิน", "ข้าพเจ้าควรจัดการเรื่องเงินทองอย่างไรในยามนี้"),
    DECISION("decision", "การตัดสินใจ", "ข้าพเจ้าควรตัดสินใจเรื่องที่ค้างคาใจอย่างไร"),
    MIND("mind", "สุขภาพใจ", "ข้าพเจ้าจะหาความสงบในใจได้อย่างไร");

    companion object {
        fun fromWire(value: String?): QuestionCategory? = entries.firstOrNull { it.wire == value }
    }
}

data class AppSettings(
    val soundEnabled: Boolean = true,
    /** The seeker has read and accepted the disclaimer on first launch. */
    val onboardingAccepted: Boolean = false,
    val streak: StreakState = StreakState(),
    val selectedTheme: AppTheme = AppTheme.INK_PAPER,
    val dailyPushEnabled: Boolean = false,
    /** False while the FCM topic subscription still has to catch up with [dailyPushEnabled]. */
    val dailyPushSynced: Boolean = true,
    val pushPromptShown: Boolean = false,
)
