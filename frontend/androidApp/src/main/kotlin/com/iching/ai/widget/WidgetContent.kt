package com.iching.ai.widget

import com.iching.ai.domain.model.AppLanguage
import com.iching.ai.domain.model.AppSettings
import com.iching.ai.domain.oracle.dailyHexagramId
import com.iching.ai.domain.repository.HexagramRepository
import com.iching.ai.ui.strings.shortName
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/** What the widget shows: the Unicode hexagram, its short name and the day's message. */
data class WidgetContent(val hexagramId: Int, val symbol: String, val title: String, val message: String)

private const val MILLIS_PER_DAY = 86_400_000L
private const val BANGKOK_OFFSET_MILLIS = 7 * 3_600_000L

/**
 * The hexagram of [epochDay] (Bangkok day, same as the daily screen). The repository already speaks the language in
 * force, so a hexagram with an English name is shown in English and one without it falls back to Thai.
 */
suspend fun widgetContentFor(epochDay: Long, repo: HexagramRepository): WidgetContent {
    val hexagram = repo.byId(dailyHexagramId(epochDay))
    val lang = if (hexagram.nameEn.isNotBlank()) AppLanguage.EN else AppLanguage.TH
    return WidgetContent(
        hexagramId = hexagram.id,
        // U+4DC0 is hexagram 1 in King Wen order.
        symbol = String(Character.toChars(0x4DC0 + hexagram.id - 1)),
        title = hexagram.shortName(lang),
        message = hexagram.dailyMessage,
    )
}

/** Milliseconds from [nowMillis] to the next midnight at UTC+7; exactly at midnight it is a whole day. */
fun millisUntilBangkokMidnight(nowMillis: Long): Long {
    val intoDay = (nowMillis + BANGKOK_OFFSET_MILLIS).mod(MILLIS_PER_DAY)
    return MILLIS_PER_DAY - intoDay
}

/**
 * The widget runs without the UI, so the holder's language starts on its Thai placeholder. Wait until the holder has
 * caught up with the saved settings, then read it. The whole wait, loading the settings included, ends at
 * [timeoutMillis] (also when English was turned off by the server), and the holder's current language is used.
 */
suspend fun awaitLanguage(
    settings: Flow<AppSettings>,
    language: StateFlow<AppLanguage>,
    timeoutMillis: Long = 1_500,
): AppLanguage {
    withTimeoutOrNull(timeoutMillis) {
        val saved = settings.first()
        val wanted = if (saved.language == AppLanguage.EN && saved.lastEnglishEnabled) AppLanguage.EN else AppLanguage.TH
        language.first { it == wanted }
    }
    return language.value
}

/** Like runCatching, but a cancelled coroutine stays cancelled. */
suspend inline fun <T> runCatchingCancellable(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(e)
    }

/**
 * One midnight run: the next one is set up first, so a redraw that fails can't end the chain; the redraw's failure
 * is dropped (the next run tries again) and cancellation still propagates.
 */
suspend fun refreshAndReschedule(update: suspend () -> Unit, hasWidgets: suspend () -> Boolean, scheduleNext: () -> Unit) {
    if (runCatchingCancellable { hasWidgets() }.getOrDefault(true)) scheduleNext()
    runCatchingCancellable { update() }
}
