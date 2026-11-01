package com.iching.ai.domain.model

/** Colour themes, unlocked by days in a row of opening the daily hexagram. [wire] is the stored value. */
enum class AppTheme(val wire: String, val labelTh: String, val unlockDays: Int) {
    INK_PAPER("ink_paper", "กระดาษหมึก", 0),
    BAMBOO("bamboo", "ไผ่เขียว", 7),
    CINNABAR_DAWN("cinnabar_dawn", "อรุณชาด", 30),
    INK_NIGHT("ink_night", "ราตรีหมึก", 100);

    companion object {
        fun fromWire(value: String?): AppTheme? = entries.firstOrNull { it.wire == value }
    }
}

data class StreakState(
    val current: Int = 0,
    val best: Int = 0,
    val lastEpochDay: Long? = null,
    val unlocked: Set<AppTheme> = setOf(AppTheme.INK_PAPER),
)

data class StreakUpdate(
    val state: StreakState,
    val newlyUnlocked: List<AppTheme>,
    /** False when this day was already counted, or the device clock went backwards. */
    val counted: Boolean,
)

/** The streak as of [today]: zero once a whole day was missed, even though the next view is what resets it. */
fun StreakState.currentOn(today: Long): Int {
    val last = lastEpochDay ?: return 0
    return if (today - last <= 1) current else 0
}

fun StreakState.recordView(today: Long): StreakUpdate {
    val last = lastEpochDay
    if (last != null && today <= last) return StreakUpdate(this, emptyList(), counted = false)
    val nextCurrent = if (last == today - 1) current + 1 else 1
    val newlyUnlocked = AppTheme.entries.filter { it.unlockDays <= nextCurrent && it !in unlocked }
    return StreakUpdate(
        state = copy(
            current = nextCurrent,
            best = maxOf(best, nextCurrent),
            lastEpochDay = today,
            unlocked = unlocked + newlyUnlocked,
        ),
        newlyUnlocked = newlyUnlocked,
        counted = true,
    )
}
