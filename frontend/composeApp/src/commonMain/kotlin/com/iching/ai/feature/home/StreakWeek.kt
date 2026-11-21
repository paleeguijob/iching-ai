package com.iching.ai.feature.home

import com.iching.ai.domain.model.AppTheme
import com.iching.ai.domain.model.StreakState
import com.iching.ai.domain.model.currentOn
import com.iching.ai.ui.strings.CommonStrings
import com.iching.ai.ui.strings.HomeStrings

/**
 * One circle of the home streak row, labelled with its day number in the run. [done] once that day is counted;
 * [isToday] for the number today is or will be; [milestone] where a theme unlocks.
 */
data class StreakDay(val label: String, val done: Boolean, val isToday: Boolean, val milestone: Boolean)

/** Seven days of the current run, in blocks of seven (1–7, 8–14, …), and one line saying where it stands. */
data class StreakWeek(val days: List<StreakDay>, val caption: String)

// Only the length of the run is stored, not which days were visited, so the row counts days rather than
// naming weekdays: a weekday row would show days visited before a break as missed.
fun streakWeek(state: StreakState, today: Long, strings: HomeStrings, common: CommonStrings): StreakWeek {
    val current = state.currentOn(today)
    val countedToday = current > 0 && state.lastEpochDay == today
    val todayNumber = if (countedToday) current else current + 1
    val start = (todayNumber - 1) / 7 * 7 + 1
    val milestones = AppTheme.entries.map { it.unlockDays }.filter { it > 0 }.toSet()
    val days = (start until start + 7).map { n ->
        StreakDay(label = strings.streakDayLabel(n), done = n <= current, isToday = n == todayNumber, milestone = n in milestones)
    }
    return StreakWeek(days, streakCaption(state, current, countedToday, strings, common))
}

private fun streakCaption(state: StreakState, current: Int, countedToday: Boolean, strings: HomeStrings, common: CommonStrings): String {
    if (current == 0) return strings.streakStart
    val run = strings.streakRun(current)
    if (!countedToday) return strings.streakKeep(run)
    val next = AppTheme.entries.filter { it.unlockDays > current }.minByOrNull { it.unlockDays }
        ?: return strings.streakBest(run, maxOf(state.best, current))
    return strings.streakNextTheme(run, next.unlockDays - current, common.theme(next))
}
