package com.antigravity.noichuan.domain.repository

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val isSfxEnabled: Flow<Boolean>
    val isBgmEnabled: Flow<Boolean>
    val speechEngineType: Flow<String> // "VOSK" | "GOOGLE"
    val languageCode: Flow<String> // "vi" | "en"

    suspend fun setSfxEnabled(enabled: Boolean)
    suspend fun setBgmEnabled(enabled: Boolean)
    suspend fun setSpeechEngineType(engineType: String)
    suspend fun setLanguageCode(code: String)
}
