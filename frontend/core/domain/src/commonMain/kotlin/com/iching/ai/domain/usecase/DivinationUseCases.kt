package com.iching.ai.domain.usecase

import com.iching.ai.domain.model.CastResult
import com.iching.ai.domain.model.CoinFace
import com.iching.ai.domain.model.CoinToss
import com.iching.ai.domain.model.LineType
import com.iching.ai.domain.repository.HexagramRepository
import kotlin.random.Random

/** Tosses three coins once; each coin is an independent fair 50/50. */
class TossCoinsUseCase(private val random: Random = Random.Default) {
    operator fun invoke(): CoinToss =
        CoinToss(List(3) { if (random.nextBoolean()) CoinFace.HEADS else CoinFace.TAILS })
}

/** Turns six lines (bottom to top) into the primary and relating hexagrams. */
class ResolveCastUseCase(private val hexagrams: HexagramRepository) {
    suspend operator fun invoke(lines: List<LineType>): CastResult {
        require(lines.size == 6) { "A hexagram needs six lines, got ${lines.size}" }
        val primaryPattern = lines.joinToString("") { if (it.isYang) "1" else "0" }
        val relatingPattern = lines.joinToString("") { if (it.isYangAfterChange) "1" else "0" }
        val primary = hexagrams.byPattern(primaryPattern)
        val relating = if (relatingPattern != primaryPattern) hexagrams.byPattern(relatingPattern) else null
        return CastResult(lines = lines, primary = primary, relating = relating)
    }
}
