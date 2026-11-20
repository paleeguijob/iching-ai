package com.iching.ai.feature.home

import com.iching.ai.domain.model.AccountStatus
import com.iching.ai.domain.model.CampaignStatus
import com.iching.ai.domain.model.Hexagram
import com.iching.ai.domain.model.YearHexagram
import com.iching.ai.domain.model.campaignIdFor
import com.iching.ai.ui.strings.HomeStrings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The year hexagram's card on the daily page. */
sealed interface CampaignCard {
    data object Hidden : CampaignCard
    data class New(val year: Int) : CampaignCard
    data class Cast(val year: Int, val hexagram: Hexagram) : CampaignCard
}

/** Shown only while the server says the campaign is active; [hexagram] is the year hexagram once cast. */
fun campaignCard(campaign: CampaignStatus?, year: YearHexagram?, hexagram: Hexagram?): CampaignCard = when {
    campaign == null || !campaign.active -> CampaignCard.Hidden
    year != null && hexagram != null -> CampaignCard.Cast(campaign.year, hexagram)
    else -> CampaignCard.New(campaign.year)
}

/** The year hexagram as one quiet line; [pending] marks a year not cast yet. */
data class CampaignLine(val text: String, val action: String, val pending: Boolean)

fun campaignLine(card: CampaignCard, strings: HomeStrings): CampaignLine? = when (card) {
    CampaignCard.Hidden -> null
    is CampaignCard.New -> CampaignLine(strings.campaignNew(card.year), action = strings.campaignActionCast, pending = true)
    is CampaignCard.Cast -> CampaignLine(strings.campaignCast(card.hexagram.shortNameTh), action = strings.campaignActionView, pending = false)
}

/**
 * Looks up the seeker's year hexagram while the campaign runs and says which card to show. The analytics
 * keep their banner names from when the card sat on home.
 */
class YearCampaignCards(
    private val scope: CoroutineScope,
    private val status: StateFlow<AccountStatus>,
    private val yearHexagram: suspend () -> YearHexagram?,
    private val getHexagram: suspend (Int) -> Hexagram,
    private val log: (String, Map<String, String>) -> Unit,
) {
    private val yearResult = MutableStateFlow<YearHexagram?>(null)
    private val yearHexagramFound = MutableStateFlow<Hexagram?>(null)

    /**
     * True once a year lookup has answered. Until then the card stays hidden: showing "new" first would
     * flash the wrong card (and log it) for someone who already has a hexagram. A failed lookup never
     * sets it, so offline the card stays hidden rather than guessing.
     */
    private val answered = MutableStateFlow(false)

    val card: StateFlow<CampaignCard> = combine(status, yearResult, yearHexagramFound, answered) { s, year, hexagram, done ->
        if (done) campaignCard(s.campaign, year, hexagram) else CampaignCard.Hidden
    }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), CampaignCard.Hidden)

    private var loggedState: String? = null

    init {
        // The year hexagram is only asked for once the server says there is a campaign.
        scope.launch {
            status.map { it.campaign?.active == true }.distinctUntilChanged().collect { if (it) refresh() }
        }
    }

    /** The page is in front again: the seeker may have cast the year on another screen meanwhile. */
    fun onResumed() {
        if (status.value.campaign?.active == true) refresh()
    }

    /** Keeps the last answer when the server cannot be reached. */
    private fun refresh() {
        scope.launch {
            val result = try {
                yearHexagram()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                return@launch
            }
            val hexagram = result?.let { runCatching { getHexagram(it.primaryId) }.getOrNull() ?: return@launch }
            yearHexagramFound.value = hexagram
            yearResult.value = result
            answered.value = true
        }
    }

    /** The card is on screen; counted once per kind of card, not on every redraw. */
    fun onShown(card: CampaignCard) {
        val state = card.stateName() ?: return
        if (loggedState == state) return
        loggedState = state
        log("campaign_banner_shown", mapOf("campaign_id" to campaignId(card), "state" to state))
    }

    fun onTapped(card: CampaignCard) {
        val state = card.stateName() ?: return
        log("campaign_banner_tap", mapOf("campaign_id" to campaignId(card), "state" to state))
    }

    private fun campaignId(card: CampaignCard): String {
        val year = when (card) {
            is CampaignCard.New -> card.year
            is CampaignCard.Cast -> card.year
            CampaignCard.Hidden -> 0
        }
        return campaignIdFor(status.value.campaign, year)
    }

    private fun CampaignCard.stateName(): String? = when (this) {
        CampaignCard.Hidden -> null
        is CampaignCard.New -> "new"
        is CampaignCard.Cast -> "cast"
    }
}
