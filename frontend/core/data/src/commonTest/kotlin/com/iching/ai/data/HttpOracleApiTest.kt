package com.iching.ai.data

import com.iching.ai.data.api.ApiSession
import com.iching.ai.data.api.HttpOracleApi
import com.iching.ai.domain.model.AppTheme
import com.iching.ai.domain.model.CampaignStatus
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
import com.iching.ai.domain.model.SiamSiStatus
import com.iching.ai.domain.siamsi.SiamSiPick
import com.iching.ai.domain.siamsi.SiamSiSetRef
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
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

    @Test
    fun statusParsesTheCampaign() = runTest {
        val api = apiAnswering(
            HttpStatusCode.OK,
            """{"premium":false,"coins":0,"campaign":{"id":"cny-2027","year":2027,"active":true,"endsAt":1801000000000}}""",
        )
        val campaign = api.status().campaign
        assertEquals(CampaignStatus("cny-2027", 2027, true, 1801000000000L), campaign)
    }

    @Test
    fun statusWithoutACampaignHasNone() = runTest {
        assertNull(apiAnswering(HttpStatusCode.OK, """{"premium":false,"campaign":null}""").status().campaign)
    }

    @Test
    fun yearHexagramReturnsNullOn404() = runTest {
        assertNull(apiAnswering(HttpStatusCode.NotFound, """{"error":"year_hexagram_not_found"}""").yearHexagram())
    }

    @Test
    fun yearHexagramParsesTheCast() = runTest {
        var request = ""
        val engine = MockEngine { r ->
            request = "${r.method.value} ${r.url.encodedPath}"
            respond(
                """{"year":2027,"primaryId":11,"lines":[7,7,7,8,8,8],"coins":[[3,3,2],[3,3,2],[3,3,2],[2,2,3],[2,2,3],[2,2,3]],"castAt":1}""",
                HttpStatusCode.OK,
            )
        }
        val year = HttpOracleApi(HttpClient(engine), FakeSession(), "https://x/api").yearHexagram()
        assertEquals("GET /api/year-hexagram", request)
        assertEquals(11, year?.primaryId)
        assertEquals(2027, year?.year)
        assertEquals(listOf(7, 7, 7, 8, 8, 8), year?.lines)
        assertEquals(listOf(3, 3, 2), year?.coins?.get(0))
    }

    @Test
    fun castYearHexagramMapsCampaignInactive() = runTest {
        val api = apiAnswering(HttpStatusCode.Conflict, """{"error":"campaign_inactive"}""")
        val e = assertFailsWith<ApiException> { api.castYearHexagram() }
        assertEquals(FailureReason.CAMPAIGN_INACTIVE, e.reason)
    }

    @Test
    fun castYearHexagramPostsAnEmptyBody() = runTest {
        var request = ""
        var body = ""
        val engine = MockEngine { r ->
            request = "${r.method.value} ${r.url.encodedPath}"
            body = (r.body as TextContent).text
            respond("""{"year":2027,"primaryId":1,"lines":[7,7,7,7,7,7],"coins":[],"castAt":1}""", HttpStatusCode.OK)
        }
        HttpOracleApi(HttpClient(engine), FakeSession(), "https://x/api").castYearHexagram()
        assertEquals("POST /api/year-hexagram", request)
        assertEquals("{}", body)
    }

    @Test
    fun divineYearPostsModeYear() = runTest {
        var sentBody = ""
        val engine = MockEngine { r ->
            sentBody = (r.body as TextContent).text
            respond("event: delta\ndata: {\"text\":\"ok\"}\n\n", HttpStatusCode.OK, sse)
        }
        val events = HttpOracleApi(HttpClient(engine), FakeSession(), "https://x/api").divineYear("ปีนี้เป็นอย่างไร").toList()
        assertTrue(sentBody.contains("\"mode\":\"year\""))
        assertTrue(sentBody.contains("ปีนี้เป็นอย่างไร"))
        assertEquals(listOf<DivinationEvent>(DivinationEvent.Delta("ok")), events)
    }

    @Test
    fun divineYearWithoutAYearHexagramFails() = runTest {
        val engine = MockEngine { respond("""{"error":"no_year_hexagram"}""", HttpStatusCode.Conflict) }
        val events = HttpOracleApi(HttpClient(engine), FakeSession(), "https://x/api").divineYear("x").toList()
        assertEquals(listOf<DivinationEvent>(DivinationEvent.Failed(FailureReason.NO_YEAR_HEXAGRAM)), events)
    }

    @Test
    fun readingsCarryTheYearAndTheNewYearCategory() = runTest {
        val api = apiAnswering(
            HttpStatusCode.OK,
            """{"readings":[{"id":"r1","question":"q","category":"newyear","lines":[7,7,7,7,7,7],"primaryId":1,"createdAt":5,"yearOf":2027},
                {"id":"r2","question":"q","lines":[7,7,7,7,7,7],"primaryId":1,"createdAt":4,"yearOf":null}]}""",
        )
        val readings = api.readings()
        assertEquals(2027, readings[0].yearOf)
        assertEquals(QuestionCategory.NEWYEAR, readings[0].category)
        assertNull(readings[1].yearOf)
    }

    @Test
    fun streakKeepsCinnabarGoldFromTheServer() = runTest {
        val api = apiAnswering(
            HttpStatusCode.OK,
            """{"streak":{"current":1,"best":1,"lastEpochDay":1,"unlocked":["ink_paper","cinnabar_gold"]}}""",
        )
        val merged = api.syncStreak(StreakState())
        assertTrue(AppTheme.CINNABAR_GOLD in merged.unlocked)
    }

    @Test
    fun interpretSiamSiSendsTheSlipAndRelaysTheReadingId() = runTest {
        var sentBody = ""
        var path = ""
        val engine = MockEngine { request ->
            sentBody = (request.body as TextContent).text
            path = request.url.encodedPath
            respond(
                "event: reading\ndata: {\"readingId\":\"r9\"}\n\n" +
                    "event: delta\ndata: {\"text\":\"เมฆเปิด\"}\n\n" +
                    "event: done\ndata: {\"model\":\"m\",\"status\":null}\n\n",
                HttpStatusCode.OK,
                sse,
            )
        }
        val api = HttpOracleApi(HttpClient(engine), FakeSession(), "https://x/api")
        val events = api.interpretSiamSi(SiamSiPick("guanyin100", 17), "งานใหม่", QuestionCategory.WORK).toList()

        assertEquals("/api/siamsi/interpret", path)
        assertTrue(sentBody.contains("\"set\":\"guanyin100\""))
        assertTrue(sentBody.contains("\"stick\":17"))
        assertTrue(sentBody.contains("\"question\":\"งานใหม่\""))
        assertTrue(sentBody.contains("\"category\":\"work\""))
        assertTrue(!sentBody.contains("readingId"))
        assertEquals(DivinationEvent.Created("r9"), events[0])
        assertEquals(DivinationEvent.Delta("เมฆเปิด"), events[1])
        assertIs<DivinationEvent.Done>(events[2])
    }

    @Test
    fun interpretSiamSiRetriesByReadingId() = runTest {
        var sentBody = ""
        val engine = MockEngine { request ->
            sentBody = (request.body as TextContent).text
            respond("event: reading\ndata: {\"readingId\":\"r9\"}\n\n", HttpStatusCode.OK, sse)
        }
        HttpOracleApi(HttpClient(engine), FakeSession(), "https://x/api")
            .interpretSiamSi(SiamSiPick("guanyin100", 17), "", null, readingId = "r9").toList()
        assertTrue(sentBody.contains("\"readingId\":\"r9\""))
    }

    @Test
    fun statusReadsSiamSiAndDefaultsToOff() = runTest {
        val on = apiAnswering(HttpStatusCode.OK, """{"siamsi":{"enabled":true,"sets":[{"id":"guanyin100","version":2}],"drawsPerDay":10}}""").status()
        assertEquals(SiamSiStatus(true, listOf(SiamSiSetRef("guanyin100", 2)), 10), on.siamsi)
        val old = apiAnswering(HttpStatusCode.OK, """{"premium":false}""").status()
        assertEquals(SiamSiStatus(), old.siamsi)
        assertEquals(false, old.siamsi.enabled)
    }

    @Test
    fun readingsCarryTheSiamSiSlip() = runTest {
        val list = apiAnswering(HttpStatusCode.OK, 
            """{"readings":[{"id":"r9","question":"q","lines":[],"primaryId":0,"aiText":"t","createdAt":5,"siamsi":{"set":"jiazi60","stick":3}},
            {"id":"r1","lines":[7,7,7,7,7,7],"primaryId":1,"createdAt":4,"siamsi":null}]}""").readings()
        assertEquals(SiamSiPick("jiazi60", 3), list[0].siamsi)
        assertEquals(emptyList(), list[0].lines)
        assertEquals(null, list[1].siamsi)
    }
}
