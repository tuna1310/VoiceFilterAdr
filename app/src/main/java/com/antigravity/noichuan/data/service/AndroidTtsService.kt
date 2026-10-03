package com.antigravity.noichuan.data.service

import android.content.Context
import android.speech.tts.TextToSpeech
import com.antigravity.noichuan.domain.service.TtsService
import java.util.Locale

class AndroidTtsService(context: Context) : TtsService {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var pendingText: String? = null

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.let { engine ->
                    val result = engine.setLanguage(Locale.US)
                    if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                        isInitialized = true
                        pendingText?.let {
                            speak(it)
                            pendingText = null
                        }
                    }
                }
            }
        }
    }

    override fun speak(text: String) {
        if (!isInitialized) {
            pendingText = text
            return
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "UTTERANCE_${System.currentTimeMillis()}")
    }

    override fun stop() {
        tts?.stop()
    }

    override fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            tts = null
            isInitialized = false
        }
    }
}
