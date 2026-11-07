package com.iching.ai.feature.share

import com.iching.ai.domain.model.Trigram
import kotlin.test.Test
import kotlin.test.assertEquals

class TrigramSceneTest {
    @Test
    fun everyTrigramPaintsItsOwnSkyAndGround() {
        assertEquals(Trigram.entries.size, Trigram.entries.map(::skyMotif).toSet().size)
        assertEquals(Trigram.entries.size, Trigram.entries.map(::groundMotif).toSet().size)
    }

    @Test
    fun all64HexagramsGetADifferentScene() {
        val scenes = Trigram.entries.flatMap { upper -> Trigram.entries.map { lower -> skyMotif(upper) to groundMotif(lower) } }
        assertEquals(64, scenes.toSet().size)
    }
}
