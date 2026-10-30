package com.iching.ai.domain.repository

import com.iching.ai.domain.model.AppSettings
import com.iching.ai.domain.model.Hexagram
import com.iching.ai.domain.model.Reading
import com.iching.ai.domain.oracle.OraclePrompt
import kotlinx.coroutines.flow.Flow

interface HexagramRepository {
    suspend fun all(): List<Hexagram>
    suspend fun byId(id: Int): Hexagram
    suspend fun byPattern(pattern: String): Hexagram
}

/** Sends a prepared prompt to the language model and streams the answer text in chunks. */
interface OracleRepository {
    fun interpret(prompt: OraclePrompt, apiKey: String, model: String): Flow<String>
}

interface JournalRepository {
    fun observeAll(): Flow<List<Reading>>
    suspend fun get(id: Long): Reading?
    suspend fun save(reading: Reading): Long
    suspend fun delete(id: Long)
}

interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun update(transform: (AppSettings) -> AppSettings)
}

/** Failures the oracle can report, so the UI can explain them in plain words. */
sealed class OracleException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class InvalidApiKey(cause: Throwable? = null) : OracleException("Invalid API key", cause)
    class RateLimited(cause: Throwable? = null) : OracleException("Rate limited", cause)
    class Network(cause: Throwable? = null) : OracleException("Network unavailable", cause)
    class Blocked : OracleException("Response blocked by safety filter")
    class Unknown(message: String, cause: Throwable? = null) : OracleException(message, cause)
}
