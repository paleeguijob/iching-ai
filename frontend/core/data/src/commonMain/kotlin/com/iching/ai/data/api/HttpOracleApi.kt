package com.iching.ai.data.api

import com.iching.ai.domain.model.AccountStatus
import com.iching.ai.domain.model.CoinFace
import com.iching.ai.domain.model.FollowUpTurn
import com.iching.ai.domain.model.InterpretationSource
import com.iching.ai.domain.model.LineType
import com.iching.ai.domain.model.Outcome
import com.iching.ai.domain.model.QuestionCategory
import com.iching.ai.domain.model.Reading
import com.iching.ai.domain.repository.ApiException
import com.iching.ai.domain.repository.DivinationEvent
import com.iching.ai.domain.repository.FailureReason
import com.iching.ai.domain.repository.LimitReason
import com.iching.ai.domain.repository.OracleApi
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.prepareRequest
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.io.IOException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.put

/**
 * Credentials for each request. Implemented by the app on top of the native Firebase SDKs
 * (see composeApp/.../platform/NativeBridges.kt).
 */
interface ApiSession {
    /** Firebase ID token; [forceRefresh] after a 401. */
    suspend fun idToken(forceRefresh: Boolean): String

    /** Firebase App Check token, or null when App Check could not produce one. */
    suspend fun appCheckToken(): String?

    /** Stable per-install id (hashed on the server) for the per-device free limit. */
    val deviceId: String
    val appVersion: String
}

// ---------- wire format (see functions/src/core/api.ts) ----------

@Serializable
internal data class StatusDto(
    val premium: Boolean = false,
    val premiumUntil: Long? = null,
    val coins: Int = 0,
    val includedLeft: Int = 0,
    val adCreditsLeft: Int = 0,
    val adsWatchableLeft: Int = 0,
    val readingsLeft: Int = 0,
    val followUpsPerReading: Int = 0,
    val journalDays: Int? = null,
    val aiEnabled: Boolean = true,
) {
    fun toDomain() = AccountStatus(
        premium = premium, premiumUntilMillis = premiumUntil, coins = coins, includedLeft = includedLeft,
        adCreditsLeft = adCreditsLeft, adsWatchableLeft = adsWatchableLeft, readingsLeft = readingsLeft,
        followUpsPerReading = followUpsPerReading, journalDays = journalDays, aiEnabled = aiEnabled,
    )
}

@Serializable internal data class CastDto(val readingId: String, val lines: List<Int>, val coins: List<List<Int>> = emptyList())
@Serializable internal data class DeltaDto(val text: String)
@Serializable internal data class DoneDto(val status: StatusDto? = null)
@Serializable internal data class LimitedDto(val reason: String, val retryAfterSeconds: Int? = null, val status: StatusDto? = null)
@Serializable internal data class SafetyDto(val message: String)
@Serializable internal data class ErrorDto(val code: String = "internal", val error: String? = null)
@Serializable internal data class AdRewardDto(val granted: Boolean, val status: StatusDto)
@Serializable internal data class TurnDto(val q: String, val a: String)

@Serializable
internal data class ReadingDto(
    val id: String,
    val question: String = "",
    val category: String? = null,
    val lines: List<Int>,
    val primaryId: Int,
    val relatingId: Int? = null,
    val aiText: String? = null,
    val followUps: List<TurnDto> = emptyList(),
    val outcome: String? = null,
    val outcomeNote: String? = null,
    val createdAt: Long,
)

@Serializable internal data class ReadingsDto(val readings: List<ReadingDto>)

internal fun coinFace(value: Int) = if (value == 3) CoinFace.HEADS else CoinFace.TAILS

internal fun limitReason(code: String) = when (code) {
    "rate_limited" -> LimitReason.RATE_LIMITED
    "ai_disabled" -> LimitReason.AI_DISABLED
    "follow_up_limit" -> LimitReason.FOLLOW_UP_LIMIT
    else -> LimitReason.QUOTA
}

internal fun failureReason(code: String) = when (code) {
    "rate_limited", "bad_response", "empty", "auth" -> FailureReason.MODEL_BUSY
    "blocked" -> FailureReason.BLOCKED
    "network" -> FailureReason.NETWORK
    "unauthenticated", "app_check_missing", "app_check_invalid" -> FailureReason.UNAUTHENTICATED
    "premium_required" -> FailureReason.PREMIUM_REQUIRED
    else -> FailureReason.UNKNOWN
}

/** Parses one server-sent event into a domain event; unknown events are ignored. */
internal fun parseEvent(json: Json, event: String, data: String): DivinationEvent? = when (event) {
    "cast" -> json.decodeFromString(CastDto.serializer(), data).let { dto ->
        DivinationEvent.Cast(
            readingId = dto.readingId,
            lines = dto.lines.map(LineType::fromValue),
            coins = dto.coins.map { toss -> toss.map(::coinFace) },
        )
    }
    "delta" -> DivinationEvent.Delta(json.decodeFromString(DeltaDto.serializer(), data).text)
    "done" -> DivinationEvent.Done(json.decodeFromString(DoneDto.serializer(), data).status?.toDomain())
    "limited" -> json.decodeFromString(LimitedDto.serializer(), data).let {
        DivinationEvent.Limited(limitReason(it.reason), it.retryAfterSeconds, it.status?.toDomain())
    }
    "safety" -> DivinationEvent.Safety(json.decodeFromString(SafetyDto.serializer(), data).message)
    "error" -> DivinationEvent.Failed(failureReason(json.decodeFromString(ErrorDto.serializer(), data).code))
    else -> null
}

class HttpOracleApi(
    private val client: HttpClient,
    private val session: ApiSession,
    private val baseUrl: String,
) : OracleApi {

    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    override fun divine(question: String, category: QuestionCategory?): Flow<DivinationEvent> =
        stream("/divine", buildJsonObject {
            put("question", question)
            category?.let { put("category", it.wire) }
        })

    override fun interpretAgain(readingId: String): Flow<DivinationEvent> =
        stream("/divine", buildJsonObject { put("readingId", readingId) })

    override fun followUp(readingId: String, message: String): Flow<DivinationEvent> =
        stream("/follow-up", buildJsonObject {
            put("readingId", readingId)
            put("message", message)
        })

    override suspend fun status(): AccountStatus =
        json.decodeFromString(StatusDto.serializer(), call(HttpMethod.Get, "/status")).toDomain()

    override suspend fun claimAdReward(): Pair<Boolean, AccountStatus> =
        json.decodeFromString(AdRewardDto.serializer(), call(HttpMethod.Post, "/ad-reward", JsonObject(emptyMap())))
            .let { it.granted to it.status.toDomain() }

    override suspend fun report(readingId: String, reason: String, note: String) {
        call(HttpMethod.Post, "/report", buildJsonObject {
            put("readingId", readingId)
            put("reason", reason)
            put("note", note)
        })
    }

    override suspend fun saveOutcome(readingId: String, outcome: Outcome?, note: String?) {
        call(HttpMethod.Post, "/readings/$readingId/outcome", buildJsonObject {
            put("outcome", outcome?.name?.lowercase()?.let { JsonPrimitive(it) } ?: JsonNull)
            note?.let { put("note", it) }
        })
    }

    override suspend fun readings(): List<Reading> =
        json.decodeFromString(ReadingsDto.serializer(), call(HttpMethod.Get, "/readings?limit=500")).readings.map { dto ->
            Reading(
                question = dto.question,
                createdAtMillis = dto.createdAt,
                lines = dto.lines.map(LineType::fromValue),
                primaryId = dto.primaryId,
                relatingId = dto.relatingId,
                interpretation = dto.aiText.orEmpty(),
                source = if (dto.aiText.isNullOrBlank()) InterpretationSource.SCRIPTURE else InterpretationSource.AI,
                serverId = dto.id,
                category = QuestionCategory.fromWire(dto.category),
                outcome = dto.outcome?.let { o -> Outcome.entries.firstOrNull { it.name.equals(o, ignoreCase = true) } },
                outcomeNote = dto.outcomeNote,
                followUps = dto.followUps.map { FollowUpTurn(it.q, it.a) },
            )
        }

    override suspend fun deleteReading(readingId: String) {
        call(HttpMethod.Delete, "/readings/$readingId")
    }

    override suspend fun syncPurchases(): AccountStatus =
        json.decodeFromString(StatusDto.serializer(), call(HttpMethod.Post, "/purchases/sync", JsonObject(emptyMap()))).toDomain()

    override suspend fun deleteAccount() {
        call(HttpMethod.Delete, "/account")
    }

    // ---------- plumbing ----------

    private class Credentials(val idToken: String, val appCheck: String?)

    // Fetched before building the request: Ktor's request builder block is not suspending.
    private suspend fun credentials(forceRefresh: Boolean) =
        Credentials(session.idToken(forceRefresh), session.appCheckToken())

    private fun HttpRequestBuilder.authorize(c: Credentials) {
        header("Authorization", "Bearer ${c.idToken}")
        c.appCheck?.let { header("X-Firebase-AppCheck", it) }
        header("X-Device-Id", session.deviceId)
        header("X-App-Version", session.appVersion)
    }

    private suspend fun errorOf(response: HttpResponse): ApiException {
        val code = runCatching { json.decodeFromString(ErrorDto.serializer(), response.bodyAsText()).error }.getOrNull() ?: "http_${response.status.value}"
        return ApiException(failureReason(code), "HTTP ${response.status.value}: $code")
    }

    /** A plain JSON call; retries once with a fresh ID token after a 401. */
    private suspend fun call(method: HttpMethod, path: String, body: JsonElement? = null): String {
        try {
            for (attempt in 0..1) {
                val creds = credentials(forceRefresh = attempt > 0)
                val result = client.prepareRequest("$baseUrl$path") {
                    this.method = method
                    authorize(creds)
                    if (body != null) {
                        contentType(ContentType.Application.Json)
                        setBody(body.toString())
                    }
                }.execute { response ->
                    when {
                        response.status.isSuccess() -> response.bodyAsText()
                        response.status.value == 401 && attempt == 0 -> null
                        else -> throw errorOf(response)
                    }
                }
                if (result != null) return result
            }
            throw ApiException(FailureReason.UNAUTHENTICATED, "Unauthenticated")
        } catch (e: CancellationException) {
            throw e
        } catch (e: ApiException) {
            throw e
        } catch (e: IOException) {
            throw ApiException(FailureReason.NETWORK, e.message ?: "Network error")
        } catch (e: Exception) {
            throw ApiException(FailureReason.UNKNOWN, e.message ?: "Unexpected error")
        }
    }

    /**
     * POSTs and relays the server-sent events. Transport problems become a final
     * [DivinationEvent.Failed] instead of an exception, so the screen can fall back gracefully.
     * channelFlow, not flow: Ktor's Darwin engine runs the execute block on another dispatcher.
     */
    private fun stream(path: String, body: JsonElement): Flow<DivinationEvent> = channelFlow {
        try {
            for (attempt in 0..1) {
                val creds = credentials(forceRefresh = attempt > 0)
                val retry = client.prepareRequest("$baseUrl$path") {
                    method = HttpMethod.Post
                    authorize(creds)
                    header("Accept", "text/event-stream")
                    contentType(ContentType.Application.Json)
                    setBody(body.toString())
                }.execute { response ->
                    when {
                        response.status.isSuccess() -> {
                            relay(response)
                            false
                        }
                        response.status.value == 401 && attempt == 0 -> true
                        else -> {
                            send(DivinationEvent.Failed(errorOf(response).reason))
                            false
                        }
                    }
                }
                if (!retry) return@channelFlow
            }
            send(DivinationEvent.Failed(FailureReason.UNAUTHENTICATED))
        } catch (e: CancellationException) {
            throw e
        } catch (e: ApiException) {
            send(DivinationEvent.Failed(e.reason))
        } catch (e: IOException) {
            send(DivinationEvent.Failed(FailureReason.NETWORK))
        } catch (e: Exception) {
            send(DivinationEvent.Failed(FailureReason.UNKNOWN))
        }
    }

    private suspend fun ProducerScope<DivinationEvent>.relay(response: HttpResponse) {
        val channel = response.bodyAsChannel()
        var event = "message"
        while (true) {
            val line = channel.readUTF8Line() ?: break
            when {
                line.startsWith("event:") -> event = line.removePrefix("event:").trim()
                line.startsWith("data:") -> parseEvent(json, event, line.removePrefix("data:").trim())?.let { send(it) }
                line.isBlank() -> event = "message"
            }
        }
    }
}
