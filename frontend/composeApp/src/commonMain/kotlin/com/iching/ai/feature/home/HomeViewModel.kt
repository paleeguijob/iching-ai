package com.iching.ai.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iching.ai.domain.model.AccountStatus
import com.iching.ai.domain.model.Hexagram
import com.iching.ai.domain.model.currentOn
import com.iching.ai.domain.oracle.bangkokEpochDay
import com.iching.ai.domain.usecase.GetDailyHexagramUseCase
import com.iching.ai.domain.usecase.ObserveSettingsUseCase
import com.iching.ai.session.AppSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Clock

class HomeViewModel(
    private val getDaily: GetDailyHexagramUseCase,
    observeSettings: ObserveSettingsUseCase,
    session: AppSession,
) : ViewModel() {

    private val _dailyHexagram = MutableStateFlow<Hexagram?>(null)
    private val today = MutableStateFlow(bangkokEpochDay(nowMillis()))

    /** A contemplation for today, the same for everyone on a given date. */
    val dailyHexagram: StateFlow<Hexagram?> = _dailyHexagram.asStateFlow()

    val streakDays: StateFlow<Int> = combine(observeSettings(), today) { settings, day -> settings.streak.currentOn(day) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val status: StateFlow<AccountStatus> = session.status

    init {
        viewModelScope.launch { session.refreshStatus() }
    }

    /** Runs on every resume too: the app may have stayed open past midnight. */
    fun refreshDay() {
        viewModelScope.launch {
            val now = nowMillis()
            today.value = bangkokEpochDay(now)
            _dailyHexagram.value = getDaily(now)
        }
    }

    private fun nowMillis() = Clock.System.now().toEpochMilliseconds()
}
