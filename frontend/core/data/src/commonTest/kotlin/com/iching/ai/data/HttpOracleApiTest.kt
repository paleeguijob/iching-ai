package com.iching.ai.data

import com.iching.ai.data.api.ApiSession
import com.iching.ai.data.api.HttpOracleApi
import com.iching.ai.domain.model.AppTheme
import com.iching.ai.domain.model.CoinFace
import com.iching.ai.domain.model.LineType
import com.iching.ai.domain.model.QuestionCategory
import com.iching.ai.domain.model.StreakState
import com.iching.ai.domain.repository.ApiException
import com.iching.ai.domain.repository.DivinationEvent
import com.iching.ai.domain.repository.FailureReason
import com.iching.ai.domain.repository.LimitReason
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

private class FakeSession : ApiSession {
    val forced = mutableListOf<Boolean>()
    override suspend fun idToken(forceRefresh: Boolean): String {
        forced += forceRefresh
        return if (forceRefresh) "fresh" else "stale"
    }
    override suspend fun appCheckToken(): String = "ac"
    override val deviceId = "device-123456"
    override val appVersion = "1.0.0"
}

class HttpOracleApiTest {

    private val sse = headersOf(HttpHeaders.ContentType, "text/event-stream")

    private fun apiAnswering(status: HttpStatusCode, body: String) =
        HttpOracleApi(HttpClient(MockEngine { respond(body, status) }), FakeSession(), "https://x/api")

    @Test
    fun syncPurchasesReadsTheEntitlementsTheServerReturns() = runTest {
        var request = ""
        val engine = MockEngine { r ->
            request = "${r.method.value} ${r.url.encodedPath}"
            respond("""{"premium":true,"premiumUntil":1893456000000,"coins":10}""", HttpStatusCode.OK)
        }
        val status = HttpOracleApi(HttpClient(engine), FakeSession(), "https://x/api").syncPurchases()
        assertEquals("POST /api/purchases/sync", request)
        assertTrue(status.premium)
        assertEquals(1893456000000L, status.premiumUntilMillis)
        assertEquals(10, status.coins)
    }

    @Test
    fun syncPurchasesFailsLoudlyWhenPurchasesAreNotConfigured() = runTest {
        val api = apiAnswering(HttpStatusCode.ServiceUnavailable, """{"error":"purchases_not_configured"}""")
        assertFailsWith<ApiException> { api.syncPurchases() }
    }

    @Test
    fun aRejectedTokenThatStaysRejectedIsUnauthenticated() = runTest {
        val session = FakeSession()
        val engine = MockEngine { respond("""{"error":"unauthenticated"}""", HttpStatusCode.Unauthorized) }
        val failure = assertFailsWith<ApiException> { HttpOracleApi(HttpClient(engine), session, "https://x/api").syncPurchases() }
        assertEquals(FailureReason.UNAUTHENTICATED, failure.reason)
        assertEquals(listOf(false, true), session.forced)
    }

    @Test
    fun mergeAccountSendsTheLeftBehindAccountsToken() = runTest {
        var sent = ""
        val engine = MockEngine { r ->
            sent = "${r.method.value} ${r.url.encodedPath} ${(r.body as TextContent).text}"
            respond("""{"coins":10}""", HttpStatusCode.OK)
        }
        val status = HttpOracleApi(HttpClient(engine), FakeSession(), "https://x/api").mergeAccount("anon-token")
        assertEquals("""POST /api/account/merge {"fromIdToken":"anon-token"}""", sent)
        assertEquals(10, status.coins)
    }

    @Test
    fun syncStreakSendsTheThemeWireNamesAndReadsTheMergedCopy() = runTest {
        var sent = ""
        val engine = MockEngine { r ->
            sent = "${r.method.value} ${r.url.encodedPath} ${(r.body as TextContent).text}"
            respond("""{"streak":{"current":9,"best":30,"lastEpochDay":101,"unlocked":["ink_paper","bamboo","new_one"]}}""", HttpStatusCode.OK)
        }
        val here = StreakState(current = 2, best = 2, lastEpochDay = 100, unlocked = setOf(AppTheme.INK_PAPER, AppTheme.BAMBOO))
        val merged = HttpOracleApi(HttpClient(engine), FakeSession(), "https://x/api").syncStreak(here)
        assertEquals("""POST /api/streak {"current":2,"best":2,"lastEpochDay":100,"unlocked":["ink_paper","bamboo"]}""", sent)
        assertEquals(StreakState(current = 9, best = 30, lastEpochDay = 101, unlocked = setOf(AppTheme.INK_PAPER, AppTheme.BAMBOO)), merged)
    }

    @Test
    fun deleteAccountCallsTheAccountRouteAndReportsFailure() = runTest {
        var request = ""
        val engine = MockEngine { r ->
            request = "${r.method.value} ${r.url.encodedPath}"
            respond("""{"ok":true}""", HttpStatusCode.OK)
        }
        HttpOracleApi(HttpClient(engine), FakeSession(), "https://x/api").deleteAccount()
        assertEquals("DELETE /api/account", request)

        val api = apiAnswering(HttpStatusCode.InternalServerError, """{"error":"internal"}""")
        assertFailsWith<ApiException> { api.deleteAccount() }
    }

    @Test
    fun deleteReadingCallsTheReadingRoute() = runTest {
        var request = ""
        val engine = MockEngine { r ->
            request = "${r.method.value} ${r.url.encodedPath}"
            respond("""{"ok":true}""", HttpStatusCode.OK)
        }
        HttpOracleApi(HttpClient(engine), FakeSession(), "https://x/api").deleteReading("r1")
        assertEquals("DELETE /api/readings/r1", request)
    }

    @Test
    fun relaysCastDeltaAndDone() = runTest {
        var sentBody = ""
        var auth = ""
        val engine = MockEngine { request ->
            sentBody = (request.body as TextContent).text
            auth = request.headers["Authorization"].orEmpty()
            respond(
                """
                event: cast
                data: {"readingId":"r1","question":"q","lines":[9,7,8,7,8,6],"coins":[[3,3,3],[3,2,2],[3,3,2],[2,3,2],[2,2,3],[2,2,2]],"primaryId":5,"relatingId":9}

                event: delta
                data: {"text":"สายน้ำ"}

                event: done
                data: {"model":"m","shareSummary":"ใจนิ่งย่อมเห็นทาง","status":{"premium":false,"coins":0,"includedLeft":0,"adCreditsLeft":0,"adsWatchableLeft":2,"readingsLeft":0,"followUpsPerReading":0,"journalDays":7,"aiEnabled":true}}

                """.trimIndent(),
                HttpStatusCode.OK,
                sse,
            )
        }
        val api = HttpOracleApi(HttpClient(engine), FakeSession(), "https://x/api")
        val events = api.divine("งานใหม่", QuestionCategory.WORK).toList()

        assertTrue(sentBody.contains("\"category\":\"work\""))
        assertEquals("Bearer stale", auth)
        val cast = assertIs<DivinationEvent.Cast>(events[0])
        assertEquals("r1", cast.readingId)
        assertEquals(LineType.OLD_YANG, cast.lines[0])
        assertEquals(listOf(CoinFace.TAILS, CoinFace.TAILS, CoinFace.TAILS), cast.coins[5])
        assertEquals(DivinationEvent.Delta("สายน้ำ"), events[1])
        val done = assertIs<DivinationEvent.Done>(events[2])
        assertEquals(2, done.status?.adsWatchableLeft)
        assertEquals("ใจนิ่งย่อมเห็นทาง", done.shareSummary)
    }

    @Test
    fun mapsLimitedAndSafetyEvents() = runTest {
        val engine = MockEngine {
            respond(
                "event: limited\ndata: {\"reason\":\"quota\",\"retryAfterSeconds\":null,\"status\":null}\n\n" +
                    "event: safety\ndata: {\"topic\":\"self_harm\",\"message\":\"โทร 1323\"}\n\n",
                HttpStatusCode.OK,
                sse,
            )
        }
        val events = HttpOracleApi(HttpClient(engine), FakeSession(), "https://x/api").interpretAgain("r1").toList()
        assertEquals(LimitReason.QUOTA, assertIs<DivinationEvent.Limited>(events[0]).reason)
        assertEquals("โทร 1323", assertIs<DivinationEvent.Safety>(events[1]).message)
    }

    @Test
    fun retriesOnceWithFreshTokenAfter401() = runTest {
        val session = FakeSession()
        var calls = 0
        val engine = MockEngine { request ->
            calls++
            if (request.headers["Authorization"] == "Bearer stale") {
                respond("{\"error\":\"unauthenticated\"}", HttpStatusCode.Unauthorized)
            } else {
                respond("event: delta\ndata: {\"text\":\"ok\"}\n\n", HttpStatusCode.OK, sse)
            }
        }
        val events = HttpOracleApi(HttpClient(engine), session, "https://x/api").followUp("r1", "แล้วไง").toList()
        assertEquals(2, calls)
        assertEquals(listOf(false, true), session.forced)
        assertEquals(listOf<DivinationEvent>(DivinationEvent.Delta("ok")), events)
    }

    @Test
    fun anOutcomeForAMissingReadingFailsAsNotFound() = runTest {
        val api = apiAnswering(HttpStatusCode.NotFound, """{"error":"reading_not_found"}""")
        val e = assertFailsWith<ApiException> { api.saveOutcome("r1", null, null) }
        assertEquals(FailureReason.NOT_FOUND, e.reason)
    }

    @Test
    fun premiumRequiredBecomesFailedEvent() = runTest {
        val engine = MockEngine { respond("{\"error\":\"premium_required\"}", HttpStatusCode.Forbidden) }
        val events = HttpOracleApi(HttpClient(engine), FakeSession(), "https://x/api").followUp("r1", "x").toList()
        assertEquals(listOf<DivinationEvent>(DivinationEvent.Failed(FailureReason.PREMIUM_REQUIRED)), events)
    }
}
