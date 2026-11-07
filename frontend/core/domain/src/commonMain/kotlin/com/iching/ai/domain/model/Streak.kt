package com.iching.ai.domain.model

/**
 * Colour themes, unlocked by days in a row of opening the daily hexagram. [wire] is the stored value.
 * A negative [unlockDays] means no streak unlocks the theme (the server grants it).
 */
enum class AppTheme(val wire: String, val labelTh: String, val unlockDays: Int) {
    INK_PAPER("ink_paper", "กระดาษหมึก", 0),
    BAMBOO("bamboo", "ไผ่เขียว", 7),
    CINNABAR_DAWN("cinnabar_dawn", "อรุณชาด", 30),
    INK_NIGHT("ink_night", "ราตรีหมึก", 100),
    CINNABAR_GOLD("cinnabar_gold", "ชาดทอง", -1);

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
    val newlyUnlocked = AppTheme.entries.filter { it.unlockDays in 0..nextCurrent && it !in unlocked }
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

/**
 * Combines this device's streak with the account's copy on the server: the run that counted on the
 * later day wins, and best and unlocked themes only ever grow. Matches mergeStreaks in the backend.
 */
fun StreakState.mergedWith(other: StreakState): StreakState {
    val mine = lastEpochDay ?: -1
    val theirs = other.lastEpochDay ?: -1
    val run = when {
        mine > theirs -> this
        theirs > mine -> other
        current >= other.current -> this
        else -> other
    }
    return StreakState(
        current = run.current,
        best = maxOf(best, other.best, run.current),
        lastEpochDay = run.lastEpochDay,
        unlocked = unlocked + other.unlocked + AppTheme.INK_PAPER,
    )
}
