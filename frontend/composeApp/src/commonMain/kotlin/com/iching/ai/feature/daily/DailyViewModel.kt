package com.iching.ai.feature.daily

import com.iching.ai.ui.strings.AppLanguageHolder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iching.ai.domain.model.AccountStatus
import com.iching.ai.domain.model.AppSettings
import com.iching.ai.domain.model.AppTheme
import com.iching.ai.domain.model.Hexagram
import com.iching.ai.domain.model.StreakState
import com.iching.ai.domain.usecase.ClaimPushPromptUseCase
import com.iching.ai.domain.usecase.GetDailyHexagramUseCase
import com.iching.ai.domain.usecase.RecordDailyViewUseCase
import com.iching.ai.domain.oracle.bangkokEpochDay
import com.iching.ai.feature.home.CampaignCard
import com.iching.ai.feature.home.StreakWeek
import com.iching.ai.feature.home.YearCampaignCards
import com.iching.ai.feature.home.streakWeek
import com.iching.ai.feature.poster.PosterCard
import com.iching.ai.feature.poster.posterCard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

data class DailyUiState(
    val hexagram: Hexagram? = null,
    val streak: StreakState = StreakState(),
    /** The seven-day row of the current run, once today's view is recorded. */
    val week: StreakWeek? = null,
    val newlyUnlocked: List<AppTheme> = emptyList(),
    val showPushPrompt: Boolean = false,
)

class DailyViewModel(
    private val getDaily: GetDailyHexagramUseCase,
    private val recordView: RecordDailyViewUseCase,
    private val claimPushPrompt: ClaimPushPromptUseCase,
    private val log: (String, Map<String, String>) -> Unit,
    private val pushSupported: () -> Boolean,
    private val enablePush: suspend () -> Unit,
    /** Saves a newly counted day to the account. */
    private val syncStreak: suspend () -> Unit = {},
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
    status: Flow<AccountStatus> = emptyFlow(),
    settings: Flow<AppSettings> = emptyFlow(),
    /** Remembers a closed poster card so it never shows again. */
    private val dismissPosterCard: suspend (String) -> Unit = {},
    /** Builds the year hexagram card on this view model's scope; null where there is none (tests). */
    yearCampaign: (CoroutineScope) -> YearCampaignCards? = { null },
    private val languageHolder: AppLanguageHolder,
) : ViewModel() {

    private val campaign = yearCampaign(viewModelScope)

    /** The year hexagram's line near the end of the page. */
    val campaignCard: StateFlow<CampaignCard> = campaign?.card ?: MutableStateFlow(CampaignCard.Hidden)

    fun onCampaignCardShown(card: CampaignCard) {
        campaign?.onShown(card)
    }

    fun onCampaignCardTapped(card: CampaignCard) {
        campaign?.onTapped(card)
    }

    private val _state = MutableStateFlow(DailyUiState())
    val state: StateFlow<DailyUiState> = _state.asStateFlow()

    /** Refreshed with every [refresh], so a poster that ends while the page stays open drops off. */
    private val nowMillis = MutableStateFlow(now())

    /** The poster's card at the end of the page, until the seeker closes it. */
    val posterCard: StateFlow<PosterCard?> = combine(
        status,
        // No card until settings load: an empty default would flash a card closed in an earlier launch.
        settings.map { it.dismissedPosterCardIds },
        nowMillis,
        languageHolder.language,
    ) { s, dismissed, millis, _ -> posterCard(s, dismissed, millis, languageHolder.strings.poster) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private var loggedCardId: String? = null

    init {
        refresh()
        // A language switch rewords the week row without waiting for the next resume.
        viewModelScope.launch {
            languageHolder.language.collect {
                _state.update { st ->
                    if (st.week == null) st else st.copy(week = streakWeek(st.streak, bangkokEpochDay(nowMillis.value), languageHolder.strings.home, languageHolder.strings.common))
                }
            }
        }
    }

    fun onPosterCardShown(id: String) {
        if (loggedCardId == id) return
        loggedCardId = id
        log("poster_shown", cardParams(id))
    }

    fun onPosterCardTapped(id: String) = log("poster_tapped", cardParams(id))

    fun onPosterCardClosed(id: String) {
        log("poster_closed", cardParams(id))
        viewModelScope.launch { dismissPosterCard(id) }
    }

    private fun cardParams(id: String) = mapOf("poster_id" to id, "surface" to "card")

    /** Also runs on every resume: the screen may have stayed open past midnight. */
    fun refresh() {
        campaign?.onResumed()
        viewModelScope.launch {
            val millis = now()
            nowMillis.value = millis
            val hexagram = getDaily(millis)
            val update = recordView(millis)
            if (update.counted) {
                log("daily_view", emptyMap())
                launch { syncStreak() }
            }
            update.newlyUnlocked.forEach { log("streak_milestone", mapOf("days" to it.unlockDays.toString())) }
            val offerPush = pushSupported() && claimPushPrompt()
            _state.update {
                it.copy(
                    hexagram = hexagram,
                    streak = update.state,
                    week = streakWeek(update.state, bangkokEpochDay(millis), languageHolder.strings.home, languageHolder.strings.common),
                    newlyUnlocked = it.newlyUnlocked + update.newlyUnlocked,
                    showPushPrompt = it.showPushPrompt || offerPush,
                )
            }
        }
    }

    fun logEvent(name: String, params: Map<String, String>) = log(name, params)

    fun unlockNoticeShown() = _state.update { it.copy(newlyUnlocked = emptyList()) }

    fun dismissPushPrompt() = _state.update { it.copy(showPushPrompt = false) }

    fun acceptPushPrompt() {
        dismissPushPrompt()
        viewModelScope.launch { enablePush() }
    }
}
