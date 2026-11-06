package com.iching.ai.data

import com.iching.ai.data.scripture.ScriptureHexagramRepository
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Validates the bundled scripture file itself: 64 hexagrams, 384 lines, King Wen order. */
class ScriptureDataTest {

    private val repo = ScriptureHexagramRepository {
        File("src/commonMain/composeResources/files/hexagrams.json").readText()
    }

    @Test
    fun containsAllHexagramsWithSixLinesEach() = runTest {
        val all = repo.all()
        assertEquals((1..64).toList(), all.map { it.id })
        assertEquals(384, all.sumOf { it.lines.size })
        assertTrue(all.all { it.judgment.isNotBlank() && it.image.isNotBlank() && it.description.isNotBlank() })
        assertTrue(all.all { h -> h.lines.all { it.isNotBlank() } })
    }

    @Test
    fun patternsFollowKingWenSequence() = runTest {
        assertEquals(1, repo.byPattern("111111").id)
        assertEquals(2, repo.byPattern("000000").id)
        assertEquals(47, repo.byPattern("010110").id)
        assertEquals(63, repo.byPattern("101010").id)
        assertEquals(64, repo.byPattern("010101").id)
        assertEquals("ขุน — ความตีบตัน, ถูกล้อม", repo.byId(47).nameTh)
    }

    @Test
    fun everyHexagramHasAShortUniqueDailyMessage() = runTest {
        val all = repo.all()
        assertTrue(all.all { it.dailyMessage.isNotBlank() && it.dailyMessage.length <= 120 })
        assertEquals(64, all.map { it.dailyMessage }.toSet().size)
    }

    @Test
    fun onlyFirstTwoHexagramsHaveAllLinesStatement() = runTest {
        assertEquals(listOf(1, 2), repo.all().filter { it.allLines != null }.map { it.id })
    }
}
