package com.iching.ai.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iching.ai.domain.model.AppSettings
import com.iching.ai.domain.usecase.ObserveSettingsUseCase
import com.iching.ai.domain.usecase.UpdateSettingsUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    observeSettings: ObserveSettingsUseCase,
    private val updateSettings: UpdateSettingsUseCase,
) : ViewModel() {

    val settings: StateFlow<AppSettings?> = observeSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun saveApiKey(key: String) = update { it.copy(apiKey = key.trim()) }
    fun setModel(model: String) = update { it.copy(model = model) }
    fun setSound(enabled: Boolean) = update { it.copy(soundEnabled = enabled) }

    private fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { updateSettings(transform) }
    }

    companion object {
        /** Models offered in the picker (label, id). */
        val MODELS = listOf(
            "Gemini Flash (แนะนำ)" to "gemini-flash-latest",
            "Gemini Pro (ลึกซึ้ง ช้ากว่า)" to "gemini-pro-latest",
            "Gemini Flash Lite (เร็วที่สุด)" to "gemini-flash-lite-latest",
        )
    }
}
