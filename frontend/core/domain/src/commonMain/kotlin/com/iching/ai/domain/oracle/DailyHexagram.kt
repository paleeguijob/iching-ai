package com.iching.ai.domain.oracle

private const val MILLIS_PER_DAY = 86_400_000L
private const val BANGKOK_OFFSET_MILLIS = 7 * 3_600_000L

/** Days since 1970-01-01 at UTC+7. The daily hexagram and the streak use this, not the device's zone. */
fun bangkokEpochDay(nowMillis: Long): Long = (nowMillis + BANGKOK_OFFSET_MILLIS).floorDiv(MILLIS_PER_DAY)

/** The hexagram of the day, the same for everyone. Keep in step with backend/functions/src/core/daily.ts. */
fun dailyHexagramId(epochDay: Long): Int = ((epochDay * 37 + 11).mod(64L)).toInt() + 1
