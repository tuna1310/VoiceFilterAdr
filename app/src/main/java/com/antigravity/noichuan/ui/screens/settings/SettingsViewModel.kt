package com.antigravity.noichuan.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antigravity.noichuan.data.service.SoundEffectManager
import com.antigravity.noichuan.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val soundEffectManager: SoundEffectManager
) : ViewModel() {

    val isSfxEnabled: StateFlow<Boolean> = settingsRepository.isSfxEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val isBgmEnabled: StateFlow<Boolean> = settingsRepository.isBgmEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val speechEngineType: StateFlow<String> = settingsRepository.speechEngineType.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "VOSK"
    )

    val languageCode: StateFlow<String> = settingsRepository.languageCode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "vi"
    )

    fun toggleSfx(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setSfxEnabled(enabled)
            soundEffectManager.setSfxEnabled(enabled)
        }
    }

    fun toggleBgm(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setBgmEnabled(enabled)
            soundEffectManager.setBgmEnabled(enabled)
        }
    }

    fun setSpeechEngine(engineType: String) {
        viewModelScope.launch {
            settingsRepository.setSpeechEngineType(engineType)
        }
    }

    fun setLanguage(lang: String) {
        viewModelScope.launch {
            settingsRepository.setLanguageCode(lang)
        }
    }
}
