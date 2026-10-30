package com.iching.ai.domain.usecase

import com.iching.ai.domain.model.CastResult
import com.iching.ai.domain.model.InterpretationSource
import com.iching.ai.domain.oracle.OraclePromptBuilder
import com.iching.ai.domain.oracle.ScriptureReadingComposer
import com.iching.ai.domain.repository.OracleException
import com.iching.ai.domain.repository.OracleRepository
import com.iching.ai.domain.repository.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

/** Why the reading came from the scripture instead of the hermit's voice. */
enum class FallbackReason { NO_API_KEY, INVALID_API_KEY, RATE_LIMITED, NETWORK, BLOCKED, UNKNOWN }

sealed interface InterpretationUpdate {
    /** Accumulated text so far while the model is still answering. */
    data class Streaming(val text: String) : InterpretationUpdate

    data class Completed(
        val text: String,
        val source: InterpretationSource,
        val fallbackReason: FallbackReason? = null,
    ) : InterpretationUpdate
}

/**
 * The RAG step: question + scripture context + persona → language model.
 * Never fails: when the model is unavailable it completes with the scripture text instead.
 */
class InterpretReadingUseCase(
    private val oracle: OracleRepository,
    private val settings: SettingsRepository,
) {
    operator fun invoke(question: String, cast: CastResult): Flow<InterpretationUpdate> = flow {
        val current = settings.settings.first()
        if (!current.hasApiKey) {
            emit(fallback(cast, FallbackReason.NO_API_KEY))
            return@flow
        }
        val prompt = OraclePromptBuilder.build(question, cast)
        val answer = StringBuilder()
        try {
            oracle.interpret(prompt, current.apiKey, current.model).collect { chunk ->
                answer.append(chunk)
                emit(InterpretationUpdate.Streaming(answer.toString()))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // A partial answer is still the hermit's voice; only fall back when nothing arrived.
            if (answer.isBlank()) {
                emit(fallback(cast, e.toReason()))
                return@flow
            }
        }
        if (answer.isBlank()) {
            emit(fallback(cast, FallbackReason.UNKNOWN))
        } else {
            emit(InterpretationUpdate.Completed(answer.toString().trim(), InterpretationSource.AI))
        }
    }

    private fun fallback(cast: CastResult, reason: FallbackReason) = InterpretationUpdate.Completed(
        text = ScriptureReadingComposer.compose(cast),
        source = InterpretationSource.SCRIPTURE,
        fallbackReason = reason,
    )

    private fun Exception.toReason(): FallbackReason = when (this) {
        is OracleException.InvalidApiKey -> FallbackReason.INVALID_API_KEY
        is OracleException.RateLimited -> FallbackReason.RATE_LIMITED
        is OracleException.Network -> FallbackReason.NETWORK
        is OracleException.Blocked -> FallbackReason.BLOCKED
        else -> FallbackReason.UNKNOWN
    }
}
