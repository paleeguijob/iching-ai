package com.iching.ai.domain.repository

import com.iching.ai.domain.model.AccountStatus
import com.iching.ai.domain.model.AppSettings
import com.iching.ai.domain.model.CoinFace
import com.iching.ai.domain.model.Hexagram
import com.iching.ai.domain.model.LineType
import com.iching.ai.domain.model.Outcome
import com.iching.ai.domain.model.QuestionCategory
import com.iching.ai.domain.model.Reading
import kotlinx.coroutines.flow.Flow

interface HexagramRepository {
    suspend fun all(): List<Hexagram>
    suspend fun byId(id: Int): Hexagram
    suspend fun byPattern(pattern: String): Hexagram
}

/** Why the server stopped short of an AI reading. */
enum class LimitReason { QUOTA, RATE_LIMITED, AI_DISABLED, FOLLOW_UP_LIMIT }

/** Failures the app explains in plain words. */
enum class FailureReason { NETWORK, MODEL_BUSY, BLOCKED, UNAUTHENTICATED, PREMIUM_REQUIRED, UNKNOWN }

/** Events streamed by the server for one divination or follow-up. */
sealed interface DivinationEvent {
    /** The server cast the hexagram; the app animates exactly these coins. */
    data class Cast(
        val readingId: String,
        val lines: List<LineType>,
        val coins: List<List<CoinFace>>,
    ) : DivinationEvent

    data class Delta(val text: String) : DivinationEvent
    data class Done(val status: AccountStatus?) : DivinationEvent
    data class Limited(val reason: LimitReason, val retryAfterSeconds: Int?, val status: AccountStatus?) : DivinationEvent

    /** A crisis question: show this caring message (with hotlines) instead of a reading. */
    data class Safety(val message: String) : DivinationEvent

    /** The model failed after the cast; the server already refunded the quota. */
    data class Failed(val reason: FailureReason) : DivinationEvent
}

class ApiException(val reason: FailureReason, message: String) : Exception(message)

/** The backend (Cloud Functions). All calls are authenticated with Firebase Auth + App Check. */
interface OracleApi {
    fun divine(question: String, category: QuestionCategory?): Flow<DivinationEvent>
    /** Interprets an existing cast again (after an ad reward, a purchase or a failed attempt). */
    fun interpretAgain(readingId: String): Flow<DivinationEvent>
    fun followUp(readingId: String, message: String): Flow<DivinationEvent>
    suspend fun status(): AccountStatus
    /** Returns true when the server granted the ad credit. */
    suspend fun claimAdReward(): Pair<Boolean, AccountStatus>
    suspend fun report(readingId: String, reason: String, note: String)
    suspend fun saveOutcome(readingId: String, outcome: Outcome?, note: String?)
    /** Premium only: the server copy of the journal, newest first. */
    suspend fun readings(): List<Reading>
    suspend fun syncPurchases(): AccountStatus
    suspend fun deleteAccount()
}

interface JournalRepository {
    fun observeAll(): Flow<List<Reading>>
    suspend fun get(id: Long): Reading?
    suspend fun save(reading: Reading): Long
    suspend fun delete(id: Long)
    /** Inserts server readings that are not on this device yet (matched by serverId). */
    suspend fun mergeFromServer(readings: List<Reading>): Int
    suspend fun clear()
}

interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun update(transform: (AppSettings) -> AppSettings)
    suspend fun clear()
}
