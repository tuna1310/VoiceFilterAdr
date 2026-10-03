package com.antigravity.noichuan.data.service.mock

import com.antigravity.noichuan.domain.service.TtsService

class MockTtsService : TtsService {
    var lastSpokenText: String? = null
        private set

    override fun speak(text: String) {
        lastSpokenText = text
    }

    override fun stop() {}

    override fun shutdown() {
        lastSpokenText = null
    }
}
