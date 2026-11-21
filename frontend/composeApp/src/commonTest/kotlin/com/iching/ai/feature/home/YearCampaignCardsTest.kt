package com.iching.ai.feature.home

import com.iching.ai.domain.model.AccountStatus
import com.iching.ai.domain.model.CampaignStatus
import com.iching.ai.domain.model.Hexagram
import com.iching.ai.domain.model.YearHexagram
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

private val HEXAGRAM = Hexagram(
    id = 11, nameCn = "", pinyin = "", nameTh = "", pattern = "111111",
    keywords = emptyList(), judgment = "", image = "", description = "", lines = List(6) { "" },
)
private val ACTIVE = AccountStatus.UNKNOWN_FREE.copy(campaign = CampaignStatus("cny-2027", 2027, active = true, endsAtMillis = 0))
private val STORED = YearHexagram(2027, 11, listOf(7, 8, 7, 8, 7, 8), emptyList())

class YearCampaignCardsTest {
    private val scheduler = TestCoroutineScheduler()
    private val scope = CoroutineScope(StandardTestDispatcher(scheduler))
    private val events = mutableListOf<Pair<String, Map<String, String>>>()
    private val seen = mutableListOf<CampaignCard>()
    private var lookups = 0

    @AfterTest
    fun tearDown() = scope.cancel()

    /** Observes the card the way the daily page does, logging each card as it is shown. */
    private fun cards(status: MutableStateFlow<AccountStatus>, lookup: suspend () -> YearHexagram?): YearCampaignCards {
        val cards = YearCampaignCards(
            scope,
            status,
            yearHexagram = {
                lookups++
                lookup()
            },
            getHexagram = { HEXAGRAM },
            log = { name, params -> events += name to params },
        )
        cards.card.onEach {
            seen += it
            cards.onShown(it)
        }.launchIn(scope)
        scheduler.advanceUntilIdle()
        return cards
    }

    private fun bannerStates() = events.filter { it.first == "campaign_banner_shown" }.map { it.second["state"] }

    @Test
    fun aUserWithAStoredResultNeverSeesNew() {
        val cards = cards(MutableStateFlow(ACTIVE)) { STORED }
        assertIs<CampaignCard.Cast>(cards.card.value)
        assertTrue(seen.none { it is CampaignCard.New }, "the card must not flash 'new' before the lookup answers")
        assertEquals(listOf<String?>("cast"), bannerStates())
    }

    @Test
    fun aUserWithoutOneSeesNewAfterTheLookupAnswersNull() {
        val cards = cards(MutableStateFlow(ACTIVE)) { null }
        assertEquals(CampaignCard.New(2027), cards.card.value)
        assertEquals(listOf<String?>("new"), bannerStates())
    }

    @Test
    fun theCardStaysHiddenWhileTheLookupFails() {
        val cards = cards(MutableStateFlow(ACTIVE)) { error("offline") }
        assertEquals(CampaignCard.Hidden, cards.card.value)
        assertEquals(emptyList(), bannerStates())
    }

    @Test
    fun noYearLookupWhileTheCampaignIsInactiveAndOneOnceItTurnsActive() {
        val status = MutableStateFlow(ACTIVE.copy(campaign = ACTIVE.campaign!!.copy(active = false)))
        val cards = cards(status) { STORED }
        assertEquals(0, lookups)
        assertEquals(CampaignCard.Hidden, cards.card.value)

        status.value = ACTIVE
        scheduler.advanceUntilIdle()
        assertEquals(1, lookups)
        assertIs<CampaignCard.Cast>(cards.card.value)
    }

    @Test
    fun resumingLooksTheYearUpAgainSoACastElsewhereShows() {
        var stored: YearHexagram? = null
        val cards = cards(MutableStateFlow(ACTIVE)) { stored }
        assertEquals(CampaignCard.New(2027), cards.card.value)

        stored = STORED
        cards.onResumed()
        scheduler.advanceUntilIdle()
        assertIs<CampaignCard.Cast>(cards.card.value)
    }
}
