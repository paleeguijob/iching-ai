package com.iching.ai.widget

import com.iching.ai.domain.model.AppLanguage
import com.iching.ai.domain.model.AppSettings
import com.iching.ai.domain.model.Hexagram
import com.iching.ai.domain.oracle.dailyHexagramId
import com.iching.ai.domain.repository.HexagramRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WidgetContentTest {

    private fun hexagram(id: Int, english: Boolean) = Hexagram(
        id = id, nameCn = "乾", pinyin = "Qián",
        nameTh = if (english) "Heaven — Creative force" else "เฉียน — ฟ้า, ความเข้มแข็ง",
        pattern = "111111", keywords = emptyList(), judgment = "", image = "", description = "",
        lines = List(6) { "" },
        dailyMessage = if (english) "Begin with steady effort." else "เริ่มด้วยความมั่นคง",
        nameEn = if (english) "Heaven" else "",
    )

    private class FakeRepo(private val english: Boolean, private val make: (Int, Boolean) -> Hexagram) : HexagramRepository {
        override suspend fun all() = (1..64).map { make(it, english) }
        override suspend fun byId(id: Int) = make(id, english)
        override suspend fun byPattern(pattern: String) = error("unused")
    }

    @Test
    fun sameDayGivesTheDailyScreensHexagram() = runTest {
        val repo = FakeRepo(false, ::hexagram)
        for (day in listOf(0L, 20_413L, 20_500L)) {
            assertEquals(dailyHexagramId(day), widgetContentFor(day, repo).hexagramId)
        }
    }

    @Test
    fun symbolIsTheUnicodeHexagramOfTheKingWenNumber() = runTest {
        val c = widgetContentFor(20_413L, FakeRepo(false, ::hexagram))
        assertEquals(String(Character.toChars(0x4DC0 + c.hexagramId - 1)), c.symbol)
    }

    @Test
    fun englishRepoGivesAnEnglishTitleAndMessage() = runTest {
        val c = widgetContentFor(20_413L, FakeRepo(true, ::hexagram))
        assertEquals("Heaven", c.title)
        assertEquals("Begin with steady effort.", c.message)
        assertFalse(c.title.any { it in '฀'..'๿' })
    }

    @Test
    fun thaiRepoGivesTheThaiShortName() = runTest {
        val c = widgetContentFor(20_413L, FakeRepo(false, ::hexagram))
        assertEquals("เฉียน", c.title)
        assertEquals("เริ่มด้วยความมั่นคง", c.message)
    }

    @Test
    fun blankEnglishNameFallsBackToThai() = runTest {
        val repo = FakeRepo(true) { id, _ -> hexagram(id, true).copy(nameEn = "", nameTh = "เฉียน — ฟ้า") }
        assertEquals("เฉียน", widgetContentFor(20_413L, repo).title)
    }

    @Test
    fun millisUntilBangkokMidnight() {
        val bangkok2359_30 = 20_413L * 86_400_000L + 23 * 3_600_000L + 59 * 60_000L + 30_000L - 7 * 3_600_000L
        assertEquals(30_000L, millisUntilBangkokMidnight(bangkok2359_30))
        // exactly midnight: a whole day until the next one
        assertEquals(86_400_000L, millisUntilBangkokMidnight(20_413L * 86_400_000L - 7 * 3_600_000L))
        assertTrue(millisUntilBangkokMidnight(1_700_000_000_000L) in 1..86_400_000L)
    }
}

class AwaitLanguageTest {
    @Test
    fun waitsForTheHolderToLeaveItsThaiPlaceholder() = runTest {
        val holder = MutableStateFlow(AppLanguage.TH)
        val settings = flowOf(AppSettings(language = AppLanguage.EN))
        val job = launch { delay(300); holder.value = AppLanguage.EN }
        assertEquals(AppLanguage.EN, awaitLanguage(settings, holder))
        job.join()
    }

    @Test
    fun returnsWithinTheTimeoutWhenTheHolderNeverChanges() = runTest {
        val holder = MutableStateFlow(AppLanguage.TH)
        val settings = flowOf(AppSettings(language = AppLanguage.EN))
        val start = testScheduler.currentTime
        assertEquals(AppLanguage.TH, awaitLanguage(settings, holder, timeoutMillis = 1_500))
        assertEquals(1_500L, testScheduler.currentTime - start)
    }

    @Test
    fun returnsWithinTheTimeoutWhenSettingsNeverLoad() = runTest {
        val holder = MutableStateFlow(AppLanguage.TH)
        val never = kotlinx.coroutines.flow.flow<AppSettings> { kotlinx.coroutines.awaitCancellation() }
        assertEquals(AppLanguage.TH, awaitLanguage(never, holder, timeoutMillis = 1_500))
        assertEquals(1_500L, testScheduler.currentTime)
    }

    @Test
    fun staysThaiWhenTheServerSwitchedEnglishOff() = runTest {
        val holder = MutableStateFlow(AppLanguage.TH)
        val settings = flowOf(AppSettings(language = AppLanguage.EN, lastEnglishEnabled = false))
        assertEquals(AppLanguage.TH, awaitLanguage(settings, holder))
    }
}

class RefreshAndRescheduleTest {
    @Test
    fun aFailingRedrawStillSchedulesTheNextRun() = runTest {
        var scheduled = 0
        refreshAndReschedule(update = { error("boom") }, hasWidgets = { true }, scheduleNext = { scheduled++ })
        assertEquals(1, scheduled)
    }

    @Test
    fun noWidgetsMeansNoNextRun() = runTest {
        var scheduled = 0
        var updated = 0
        refreshAndReschedule(update = { updated++ }, hasWidgets = { false }, scheduleNext = { scheduled++ })
        assertEquals(0, scheduled)
        assertEquals(1, updated)
    }

    @Test
    fun cancellationPropagates() = runTest {
        val result = runCatching {
            refreshAndReschedule(update = { throw kotlinx.coroutines.CancellationException("c") }, hasWidgets = { true }, scheduleNext = {})
        }
        assertTrue(result.exceptionOrNull() is kotlinx.coroutines.CancellationException)
    }
}
