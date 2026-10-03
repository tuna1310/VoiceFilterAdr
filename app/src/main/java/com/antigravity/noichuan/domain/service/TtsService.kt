package com.antigravity.noichuan.domain.service

interface TtsService {
    fun speak(text: String)
    fun stop()
    fun shutdown()
}
