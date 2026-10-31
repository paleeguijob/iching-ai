package com.iching.ai.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iching.ai.domain.model.AccountStatus
import com.iching.ai.domain.model.Hexagram
import com.iching.ai.domain.usecase.GetHexagramsUseCase
import com.iching.ai.session.AppSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

class HomeViewModel(
    getHexagrams: GetHexagramsUseCase,
    session: AppSession,
) : ViewModel() {

    private val _dailyHexagram = MutableStateFlow<Hexagram?>(null)

    /** A contemplation for today, the same for everyone on a given date. */
    val dailyHexagram: StateFlow<Hexagram?> = _dailyHexagram.asStateFlow()

    val status: StateFlow<AccountStatus> = session.status

    init {
        viewModelScope.launch { session.refreshStatus() }
    }

    init {
        viewModelScope.launch {
            val all = getHexagrams()
            val day = Clock.System.todayIn(TimeZone.currentSystemDefault()).toEpochDays()
            _dailyHexagram.value = all[((day * 37 + 11) % 64).toInt().let { if (it < 0) it + 64 else it }]
        }
    }
}
