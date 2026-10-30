package com.iching.ai.data.oracle

import com.iching.ai.domain.oracle.OraclePrompt
import com.iching.ai.domain.repository.OracleException
import com.iching.ai.domain.repository.OracleRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.io.IOException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
internal data class GeminiRequest(
    val systemInstruction: GeminiContent,
    val contents: List<GeminiContent>,
    val generationConfig: GenerationConfig,
)

@Serializable
internal data class GeminiContent(val parts: List<GeminiPart>, val role: String? = null)

@Serializable
internal data class GeminiPart(val text: String? = null, val thought: Boolean? = null)

@Serializable
internal data class GenerationConfig(
    val temperature: Double,
    val topP: Double,
    val maxOutputTokens: Int,
    val thinkingConfig: ThinkingConfig,
)

@Serializable
internal data class ThinkingConfig(val thinkingLevel: String)

@Serializable
internal data class GeminiStreamChunk(
    val candidates: List<GeminiCandidate> = emptyList(),
    val promptFeedback: PromptFeedback? = null,
)

@Serializable
internal data class GeminiCandidate(val content: GeminiContent? = null, val finishReason: String? = null)

@Serializable
internal data class PromptFeedback(val blockReason: String? = null)

/**
 * Google Gemini over REST with server-sent events, so the reading can appear while it is written.
 * Temperature 0.75 per the spec: literary language without drifting from the scripture.
 */
class GeminiOracleRepository(
    private val client: HttpClient,
    private val baseUrl: String = "https://generativelanguage.googleapis.com/v1beta",
) : OracleRepository {

    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    // channelFlow, not flow: Ktor may run the execute block on another dispatcher (Darwin does),
    // and emitting from there violates flow's context invariant.
    override fun interpret(prompt: OraclePrompt, apiKey: String, model: String): Flow<String> = channelFlow {
        val request = GeminiRequest(
            systemInstruction = GeminiContent(listOf(GeminiPart(prompt.systemInstruction))),
            contents = listOf(GeminiContent(listOf(GeminiPart(prompt.userMessage)), role = "user")),
            generationConfig = GenerationConfig(
                temperature = 0.75,
                topP = 0.95,
                maxOutputTokens = 4096,
                thinkingConfig = ThinkingConfig(thinkingLevel = "low"),
            ),
        )
        try {
            client.preparePost("$baseUrl/models/${model.trim()}:streamGenerateContent?alt=sse") {
                header("x-goog-api-key", apiKey.trim())
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(GeminiRequest.serializer(), request))
            }.execute { response ->
                if (!response.status.isSuccess()) {
                    throw mapHttpError(response.status.value, response.bodyAsText())
                }
                val channel = response.bodyAsChannel()
                while (true) {
                    val line = channel.readUTF8Line() ?: break
                    if (!line.startsWith("data:")) continue
                    val chunk = json.decodeFromString(GeminiStreamChunk.serializer(), line.removePrefix("data:").trim())
                    if (chunk.promptFeedback?.blockReason != null) throw OracleException.Blocked()
                    val candidate = chunk.candidates.firstOrNull() ?: continue
                    val text = candidate.content?.parts.orEmpty()
                        .filter { it.thought != true }
                        .mapNotNull { it.text }
                        .joinToString("")
                    if (text.isNotEmpty()) send(text)
                    if (candidate.finishReason in BLOCKING_FINISH_REASONS) throw OracleException.Blocked()
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: OracleException) {
            throw e
        } catch (e: IOException) {
            throw OracleException.Network(e)
        } catch (e: Exception) {
            throw OracleException.Unknown(e.message ?: "Unexpected oracle failure", e)
        }
    }

    internal fun mapHttpError(status: Int, body: String): OracleException = when {
        status == 429 -> OracleException.RateLimited()
        status == 401 || status == 403 -> OracleException.InvalidApiKey()
        status == 400 && ("API_KEY_INVALID" in body || "API key not valid" in body) -> OracleException.InvalidApiKey()
        else -> OracleException.Unknown("Gemini HTTP $status: ${body.take(300)}")
    }

    private companion object {
        val BLOCKING_FINISH_REASONS = setOf("SAFETY", "PROHIBITED_CONTENT", "BLOCKLIST", "SPII")
    }
}
