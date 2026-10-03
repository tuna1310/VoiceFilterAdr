package com.antigravity.noichuan.data.service.mock

import com.antigravity.noichuan.domain.service.SpeechRecognitionState
import com.antigravity.noichuan.domain.service.SpeechRecognizerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MockSpeechRecognizerService(
    private val mockedCandidates: List<String> = listOf("Worcestershire", "wooster", "worcester")
) : SpeechRecognizerService {

    private val _state = MutableStateFlow<SpeechRecognitionState>(SpeechRecognitionState.Idle)
    override val state: StateFlow<SpeechRecognitionState> = _state.asStateFlow()

    override fun startListening(onReady: () -> Unit) {
        _state.value = SpeechRecognitionState.Listening
        onReady()
    }

    override fun stopListening() {
        _state.value = SpeechRecognitionState.Success(mockedCandidates)
    }

    override fun destroy() {
        _state.value = SpeechRecognitionState.Idle
    }

    fun emitMockResults(candidates: List<String>) {
        _state.value = SpeechRecognitionState.Success(candidates)
    }

    fun emitMockError(message: String) {
        _state.value = SpeechRecognitionState.Error(message)
    }
}
