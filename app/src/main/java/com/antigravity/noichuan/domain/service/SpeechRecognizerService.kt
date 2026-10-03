package com.antigravity.noichuan.domain.service

import kotlinx.coroutines.flow.StateFlow

sealed interface SpeechRecognitionState {
    data object Idle : SpeechRecognitionState
    data object Listening : SpeechRecognitionState
    data object Processing : SpeechRecognitionState
    data class Success(val candidates: List<String>) : SpeechRecognitionState
    data class Error(val message: String, val isRecoverable: Boolean = true) : SpeechRecognitionState
}

interface SpeechRecognizerService {
    val state: StateFlow<SpeechRecognitionState>
    fun startListening(onReady: () -> Unit = {})
    fun stopListening()
    fun destroy()
}
