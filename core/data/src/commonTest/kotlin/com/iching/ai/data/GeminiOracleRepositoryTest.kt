package com.iching.ai.data

import com.iching.ai.data.oracle.GeminiOracleRepository
import com.iching.ai.domain.oracle.OraclePrompt
import com.iching.ai.domain.repository.OracleException
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class GeminiOracleRepositoryTest {

    private val prompt = OraclePrompt("system", "user")

    private fun client(status: HttpStatusCode, body: String, onRequest: (String) -> Unit = {}) =
        HttpClient(MockEngine { request ->
            onRequest(request.url.toString() + " key=" + request.headers["x-goog-api-key"])
            respond(body, status, headersOf("Content-Type", "text/event-stream"))
        })

    @Test
    fun streamsTextChunksAndSkipsThoughts() = runTest {
        val sse = """
            data: {"candidates":[{"content":{"parts":[{"text":"thinking","thought":true},{"text":"สายน้ำ"}]}}]}

            data: {"candidates":[{"content":{"parts":[{"text":"ย่อมไหล"}]},"finishReason":"STOP"}]}

        """.trimIndent()
        var seen = ""
        val repo = GeminiOracleRepository(client(HttpStatusCode.OK, sse) { seen = it })
        val chunks = repo.interpret(prompt, " key ", "gemini-flash-latest").toList()
        assertEquals(listOf("สายน้ำ", "ย่อมไหล"), chunks)
        assertTrue(seen.contains("models/gemini-flash-latest:streamGenerateContent?alt=sse"))
        assertTrue(seen.endsWith("key=key"))
    }

    @Test
    fun mapsInvalidKey() = runTest {
        val repo = GeminiOracleRepository(client(HttpStatusCode.BadRequest, """{"error":{"message":"API key not valid"}}"""))
        assertFailsWith<OracleException.InvalidApiKey> { repo.interpret(prompt, "k", "m").toList() }
    }

    @Test
    fun mapsRateLimit() = runTest {
        val repo = GeminiOracleRepository(client(HttpStatusCode.TooManyRequests, "{}"))
        assertFailsWith<OracleException.RateLimited> { repo.interpret(prompt, "k", "m").toList() }
    }

    @Test
    fun mapsSafetyBlock() = runTest {
        val sse = "data: {\"candidates\":[{\"finishReason\":\"SAFETY\"}]}\n\n"
        val repo = GeminiOracleRepository(client(HttpStatusCode.OK, sse))
        assertFailsWith<OracleException.Blocked> { repo.interpret(prompt, "k", "m").toList() }
    }
}
