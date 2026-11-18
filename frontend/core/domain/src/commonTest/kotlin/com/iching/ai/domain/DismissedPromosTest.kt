package com.iching.ai.domain

import com.iching.ai.domain.model.AppSettings
import com.iching.ai.domain.model.withPromoDismissed
import kotlin.test.Test
import kotlin.test.assertEquals

class DismissedPromosTest {
    private fun dismissedAll(ids: List<String>) = ids.fold(AppSettings()) { s, id -> s.withPromoDismissed(id) }

    @Test
    fun addsId() = assertEquals(listOf("a"), AppSettings().withPromoDismissed("a").dismissedPromoIds)

    @Test
    fun keepsTheNewestTwenty() = assertEquals(
        (1..20).map { "p$it" },
        dismissedAll((0..20).map { "p$it" }).dismissedPromoIds,
    )

    @Test
    fun dismissingTwiceKeepsOneEntry() {
        val twenty = dismissedAll((0..19).map { "p$it" })
        assertEquals(twenty.dismissedPromoIds, twenty.withPromoDismissed("p5").dismissedPromoIds)
    }
}
