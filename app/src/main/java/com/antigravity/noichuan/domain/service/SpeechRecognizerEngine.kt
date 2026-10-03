package com.antigravity.noichuan.domain.service

import kotlinx.coroutines.flow.SharedFlow

sealed interface SpeechEngineEvent {
    data object Ready : SpeechEngineEvent
    data object Listening : SpeechEngineEvent
    data class PartialResult(val text: String) : SpeechEngineEvent
    data class FinalResult(val text: String, val confidence: Float = 1.0f) : SpeechEngineEvent
    data class NoSpeechTimeout(val message: String) : SpeechEngineEvent
    data class Error(val message: String, val isRecoverable: Boolean = true) : SpeechEngineEvent
}

enum class SpeechEngineType {
    VOSK,
    GOOGLE_SYSTEM
}

interface SpeechRecognizerEngine {
    val engineType: SpeechEngineType
    val events: SharedFlow<SpeechEngineEvent>

    fun initialize(onReady: () -> Unit = {}, onError: (String) -> Unit = {})
    fun setGrammar(targetWords: List<String>)
    fun feedPcmData(buffer: ShortArray, readSize: Int)
    fun startListening()
    fun stopListening()
    fun destroy()
}
