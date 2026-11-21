package com.iching.ai.domain

import com.iching.ai.domain.model.AppSettings
import com.iching.ai.domain.model.withFirstOpen
import com.iching.ai.domain.model.withPosterCardDismissed
import com.iching.ai.domain.model.withPosterSeen
import kotlin.test.Test
import kotlin.test.assertEquals

class PosterIdsTest {
    private fun seenAll(ids: List<String>) = ids.fold(AppSettings()) { s, id -> s.withPosterSeen(id) }
    private fun closedAll(ids: List<String>) = ids.fold(AppSettings()) { s, id -> s.withPosterCardDismissed(id) }

    @Test
    fun addsId() {
        assertEquals(listOf("a"), AppSettings().withPosterSeen("a").seenPosterIds)
        assertEquals(listOf("a"), AppSettings().withPosterCardDismissed("a").dismissedPosterCardIds)
    }

    @Test
    fun keepsTheNewestTwenty() {
        val ids = (0..20).map { "p$it" }
        assertEquals((1..20).map { "p$it" }, seenAll(ids).seenPosterIds)
        assertEquals((1..20).map { "p$it" }, closedAll(ids).dismissedPosterCardIds)
    }

    @Test
    fun addingTwiceKeepsOneEntry() {
        val seen = seenAll((0..19).map { "p$it" })
        assertEquals(seen.seenPosterIds, seen.withPosterSeen("p5").seenPosterIds)
        val closed = closedAll((0..19).map { "p$it" })
        assertEquals(closed.dismissedPosterCardIds, closed.withPosterCardDismissed("p5").dismissedPosterCardIds)
    }

    @Test
    fun firstOpenIsKept() = assertEquals(5L, AppSettings().withFirstOpen(5).withFirstOpen(9).firstOpenEpochDay)
}
