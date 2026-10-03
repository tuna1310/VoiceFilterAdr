package com.antigravity.noichuan.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.antigravity.noichuan.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DataStoreSettingsRepository(
    private val context: Context
) : SettingsRepository {

    companion object {
        val KEY_SFX_ENABLED = booleanPreferencesKey("settings_sfx_enabled")
        val KEY_BGM_ENABLED = booleanPreferencesKey("settings_bgm_enabled")
        val KEY_SPEECH_ENGINE = stringPreferencesKey("settings_speech_engine")
        val KEY_LANGUAGE = stringPreferencesKey("settings_language_code")
    }

    override val isSfxEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_SFX_ENABLED] ?: true
    }

    override val isBgmEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_BGM_ENABLED] ?: true
    }

    override val speechEngineType: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_SPEECH_ENGINE] ?: "VOSK"
    }

    override val languageCode: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_LANGUAGE] ?: "vi"
    }

    override suspend fun setSfxEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SFX_ENABLED] = enabled
        }
    }

    override suspend fun setBgmEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_BGM_ENABLED] = enabled
        }
    }

    override suspend fun setSpeechEngineType(engineType: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SPEECH_ENGINE] = engineType
        }
    }

    override suspend fun setLanguageCode(code: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_LANGUAGE] = code
        }
    }
}
