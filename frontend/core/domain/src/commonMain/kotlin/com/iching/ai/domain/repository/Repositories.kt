package com.iching.ai.domain.repository

import com.iching.ai.domain.model.AccountStatus
import com.iching.ai.domain.model.AppSettings
import com.iching.ai.domain.model.CoinFace
import com.iching.ai.domain.model.Hexagram
import com.iching.ai.domain.model.LineType
import com.iching.ai.domain.model.Outcome
import com.iching.ai.domain.model.QuestionCategory
import com.iching.ai.domain.model.Reading
import com.iching.ai.domain.model.StreakState
import com.iching.ai.domain.model.YearHexagram
import com.iching.ai.domain.siamsi.SiamSiPick
import com.iching.ai.domain.siamsi.SiamSiSet
import com.iching.ai.domain.siamsi.SiamSiSetRef
import kotlinx.coroutines.flow.Flow

interface HexagramRepository {
    suspend fun all(): List<Hexagram>
    suspend fun byId(id: Int): Hexagram
    suspend fun byPattern(pattern: String): Hexagram
}

/** Why the server stopped short of an AI reading. */
enum class LimitReason { QUOTA, RATE_LIMITED, AI_DISABLED, FOLLOW_UP_LIMIT }

/** Failures the app explains in plain words. */
enum class FailureReason { NETWORK, MODEL_BUSY, RATE_LIMITED, BLOCKED, UNAUTHENTICATED, PREMIUM_REQUIRED, NOT_FOUND, CAMPAIGN_INACTIVE, NO_YEAR_HEXAGRAM, UNKNOWN }

/** Events streamed by the server for one divination or follow-up. */
sealed interface DivinationEvent {
    /** The server cast the hexagram; the app animates exactly these coins. */
    data class Cast(
        val readingId: String,
        val lines: List<LineType>,
        val coins: List<List<CoinFace>>,
    ) : DivinationEvent

    /** A Siam Si reading was stored on the server under [readingId]. */
    data class Created(val readingId: String) : DivinationEvent

    data class Delta(val text: String) : DivinationEvent
    /** [shareSummary] is the one line for the share card; null when the model didn't write a usable one. */
    data class Done(val status: AccountStatus?, val shareSummary: String? = null) : DivinationEvent
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
    /** The hermit's interpretation of a Siam Si slip; [readingId] retries an earlier one without a new reading. */
    fun interpretSiamSi(pick: SiamSiPick, question: String, category: QuestionCategory?, readingId: String? = null): Flow<DivinationEvent>
    suspend fun status(): AccountStatus
    /** This account's hexagram for the year, or null when none was cast yet. */
    suspend fun yearHexagram(): YearHexagram?
    /** Casts the year hexagram (once per account; an existing one is returned again). */
    suspend fun castYearHexagram(): YearHexagram
    /** Interprets the year hexagram for [question]; fails with NO_YEAR_HEXAGRAM when none was cast. */
    fun divineYear(question: String): Flow<DivinationEvent>
    /** Returns true when the server granted the ad credit. */
    suspend fun claimAdReward(): Pair<Boolean, AccountStatus>
    suspend fun report(readingId: String, reason: String, note: String)
    suspend fun saveOutcome(readingId: String, outcome: Outcome?, note: String?)
    /** The server copy of the journal, newest first: the free journal window, or everything for Premium. */
    suspend fun readings(): List<Reading>
    suspend fun deleteReading(readingId: String)
    suspend fun syncPurchases(): AccountStatus
    /** Moves coins and readings of the anonymous account behind [fromIdToken] into the signed-in one. */
    suspend fun mergeAccount(fromIdToken: String): AccountStatus
    suspend fun deleteAccount()
    /** Sends this device's streak; returns the account's copy merged with it (see StreakState.mergedWith). */
    suspend fun syncStreak(streak: StreakState): StreakState
}

interface JournalRepository {
    fun observeAll(): Flow<List<Reading>>
    suspend fun get(id: Long): Reading?
    suspend fun save(reading: Reading): Long
    suspend fun delete(id: Long)
    /** Remembers a server reading the seeker deleted, until the server has deleted it too. */
    suspend fun markDeleted(serverId: String)
    suspend fun pendingDeletions(): List<String>
    suspend fun deletionSynced(serverId: String)
    /** Remembers a reading whose outcome the server has not confirmed saving yet. */
    suspend fun markOutcomeUnsynced(serverId: String)
    suspend fun unsyncedOutcomes(): List<Reading>
    suspend fun outcomeSynced(serverId: String)
    /** Inserts server readings that are not on this device yet (matched by serverId), skipping deleted ones. */
    suspend fun mergeFromServer(readings: List<Reading>): Int
    suspend fun clear()
}

/** Siam Si sets: bundled ones, plus (later) ones downloaded when the server switches them on. */
interface SiamSiRepository {
    /** The sets for [refs], in that order, skipping any that cannot be loaded. */
    suspend fun available(refs: List<SiamSiSetRef>): List<SiamSiSet>

    /** The sets for [refs] already on the device (bundled or downloaded before), with no network call. */
    suspend fun local(refs: List<SiamSiSetRef>): List<SiamSiSet> = available(refs)
    suspend fun set(id: String): SiamSiSet?
}

interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun update(transform: (AppSettings) -> AppSettings)
    suspend fun clear()
}
