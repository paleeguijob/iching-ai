package com.iching.ai.domain.siamsi

import com.iching.ai.domain.model.QuestionCategory
import kotlin.random.Random

/** How good a Siam Si slip is. [wire] is the value in the set files. */
enum class SiamSiLevel(val wire: String, val labelTh: String) {
    EXCELLENT("excellent", "ดีมาก"),
    GOOD("good", "ดี"),
    FAIR("fair", "ปานกลาง"),
    CAUTION("caution", "ควรระวัง"),
}

/** The seven matters every slip speaks to, in display order. */
enum class SiamSiTopic(val wire: String, val labelTh: String) {
    WORK("work", "การงาน"),
    MONEY("money", "การเงิน"),
    LOVE("love", "ความรัก"),
    HEALTH("health", "สุขภาพ"),
    TRAVEL("travel", "การเดินทาง"),
    LOST("lost", "ของหาย"),
    DEALINGS("dealings", "คดีความ/การติดต่อ"),
}

data class SiamSiStick(
    val number: Int,
    /** The grade printed in the source text, e.g. 中吉; null for sets without one. */
    val gradeOrig: String?,
    val level: SiamSiLevel,
    /** Extra label from the source, e.g. the stem-branch pair 甲子 of the sixty-stick set. */
    val labelOrig: String?,
    val poemOrig: List<String>,
    val poemTh: List<String>,
    val story: String,
    val summary: String,
    val topics: Map<SiamSiTopic, String>,
    val keywords: List<String>,
)

data class SiamSiSet(
    val id: String,
    val name: String,
    val count: Int,
    val sourceTitle: String,
    val sourceNote: String,
    val sticks: List<SiamSiStick>,
    /** The file's version; the server lists the current one, and a newer one replaces a bundled copy. */
    val version: Int = 1,
) {
    fun stick(number: Int): SiamSiStick = sticks.first { it.number == number }
}

/** One stick drawn from one set. */
data class SiamSiPick(val setId: String, val number: Int)

/** A set the server has switched on, with the version of its file. */
data class SiamSiSetRef(val id: String, val version: Int)

/** The two crescent blocks: ศักดิ์ (one up, one down) confirms; หัวเราะ and ไม่ ask for a new stick. */
enum class PuaResult { SAK, LAUGH, NO }

/** Draws a stick number 1..[count]; decided before any animation plays. */
fun drawStick(count: Int, random: Random): Int = random.nextInt(1, count + 1)

/** ศักดิ์ 1/2, หัวเราะ 1/4, ไม่ 1/4 — like two real blocks landing. */
fun tossPua(random: Random): PuaResult = when (random.nextInt(4)) {
    0, 1 -> PuaResult.SAK
    2 -> PuaResult.LAUGH
    else -> PuaResult.NO
}

private val GRADE_LEVELS = mapOf(
    "上上" to SiamSiLevel.EXCELLENT,
    "上" to SiamSiLevel.GOOD, "上吉" to SiamSiLevel.GOOD, "大吉" to SiamSiLevel.GOOD,
    "中" to SiamSiLevel.FAIR, "中吉" to SiamSiLevel.FAIR, "中平" to SiamSiLevel.FAIR,
    "下" to SiamSiLevel.CAUTION, "下下" to SiamSiLevel.CAUTION,
)

fun gradeToLevel(grade: String): SiamSiLevel? = GRADE_LEVELS[grade.trim()]

/** One stick and how its pua toss came out (null = not tossed yet). */
data class PuaRound(val stick: Int, val result: PuaResult?)

const val MAX_PUA_ROUNDS = 3

/** The pua rounds for one draw. The third round confirms its stick whatever the blocks say. */
data class PuaProgress(val rounds: List<PuaRound>) {
    private val last get() = rounds.lastOrNull()

    val confirmedStick: Int?
        get() {
            val l = last ?: return null
            return when {
                l.result == PuaResult.SAK -> l.stick
                l.result != null && rounds.size >= MAX_PUA_ROUNDS -> l.stick
                else -> null
            }
        }

    val autoConfirmed: Boolean
        get() = confirmedStick != null && last?.result != PuaResult.SAK

    val needsReshake: Boolean
        get() = last?.result != null && confirmedStick == null
}

/** The slip's topics, with the one matching the seeker's category first. */
fun topicOrder(category: QuestionCategory?): List<SiamSiTopic> {
    val first = when (category) {
        QuestionCategory.WORK -> SiamSiTopic.WORK
        QuestionCategory.MONEY -> SiamSiTopic.MONEY
        QuestionCategory.LOVE -> SiamSiTopic.LOVE
        QuestionCategory.MIND -> SiamSiTopic.HEALTH
        else -> null
    } ?: return SiamSiTopic.entries
    return listOf(first) + SiamSiTopic.entries.filter { it != first }
}
