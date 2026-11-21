package com.iching.ai.domain.siamsi

/** Which way of asking Home starts. */
enum class DivinationMode(val wire: String) {
    ICHING("iching"),
    SIAMSI("siamsi");

    companion object {
        fun fromWire(value: String?): DivinationMode? = entries.firstOrNull { it.wire == value }
    }
}

/**
 * Siam Si draws used today, counted on this device only: slips cost the server nothing.
 * [day] is the UTC+7 epoch day ([com.iching.ai.domain.oracle.bangkokEpochDay]).
 * [pendingDraw] is a draw already paid for (ad watched) whose slip has not been confirmed yet.
 */
data class SiamSiUsage(val day: Long = 0L, val used: Int = 0, val pendingDraw: Boolean = false) {
    /** This usage as seen on [today]: a new day starts from nothing, and an unused draw expires with it. */
    fun on(today: Long): SiamSiUsage = if (day == today) this else SiamSiUsage(today)
}

fun drawsLeft(usage: SiamSiUsage, today: Long, perDay: Int): Int =
    (perDay - usage.on(today).used).coerceAtLeast(0)

/** Spends one draw and keeps it pending until its slip is confirmed. */
fun SiamSiUsage.grant(today: Long): SiamSiUsage = on(today).let { it.copy(used = it.used + 1, pendingDraw = true) }

fun SiamSiUsage.consumePending(): SiamSiUsage = copy(pendingDraw = false)
