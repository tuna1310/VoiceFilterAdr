package com.antigravity.noichuan.data.service

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.antigravity.noichuan.domain.service.SpeechEngineEvent
import com.antigravity.noichuan.domain.service.SpeechEngineType
import com.antigravity.noichuan.domain.service.SpeechRecognizerEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * Engine nhận diện giọng nói sử dụng Google Android SpeechRecognizer.
 * 
 * ĐẶC ĐIỂM QUAN TRỌNG:
 * - Engine này TỰ MỞ một phiên thu âm AudioRecord riêng trong tầng hệ thống/Google App.
 * - KHÔNG hỗ trợ nạp dữ liệu PCM từ ngoài qua feedPcmData().
 * - KHI HOẠT ĐỘNG:
 *   + Ở Chế độ Luyện tập (Practice mode): Hoạt động bình thường.
 *   + Ở Chế độ Thử thách ghi video: Video của MediaRecorder BẮT BUỘC phải tắt mic (video câm)
 *     để tránh lỗi xung đột micro (ERROR_AUDIO).
 */
class GoogleSpeechRecognizerEngine(
    private val context: Context,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) : SpeechRecognizerEngine {

    companion object {
        private const val TAG = "GoogleSpeechEngine"
    }

    override val engineType: SpeechEngineType = SpeechEngineType.GOOGLE_SYSTEM

    private val _events = MutableSharedFlow<SpeechEngineEvent>(replay = 1)
    override val events: SharedFlow<SpeechEngineEvent> = _events.asSharedFlow()

    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    private var currentGrammar: List<String> = emptyList()

    override fun initialize(onReady: () -> Unit, onError: (String) -> Unit) {
        mainHandler.post {
            try {
                if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                    val msg = "Google SpeechRecognizer is not available on this device."
                    emitEvent(SpeechEngineEvent.Error(msg, isRecoverable = false))
                    onError(msg)
                    return@post
                }

                speechRecognizer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
                ) {
                    SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
                } else {
                    SpeechRecognizer.createSpeechRecognizer(context)
                }

                speechRecognizer?.setRecognitionListener(createListener())
                emitEvent(SpeechEngineEvent.Ready)
                onReady()
            } catch (e: Exception) {
                val msg = "Failed to initialize Google SpeechRecognizer: ${e.message}"
                emitEvent(SpeechEngineEvent.Error(msg, isRecoverable = false))
                onError(msg)
            }
        }
    }

    override fun setGrammar(targetWords: List<String>) {
        currentGrammar = targetWords
    }

    override fun feedPcmData(buffer: ShortArray, readSize: Int) {
        // Google SpeechRecognizer tự quản lý mic, không hỗ trợ nhận stream PCM ngoại vi.
        // Ghi log mức verbose nếu cần debug.
    }

    override fun startListening() {
        mainHandler.post {
            try {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-US")
                    putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                }
                emitEvent(SpeechEngineEvent.Listening)
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                emitEvent(SpeechEngineEvent.Error("startListening error: ${e.message}"))
            }
        }
    }

    override fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping listener: ${e.message}")
            }
        }
    }

    override fun destroy() {
        mainHandler.post {
            try {
                speechRecognizer?.destroy()
            } catch (e: Exception) {
                Log.w(TAG, "Error destroying recognizer: ${e.message}")
            } finally {
                speechRecognizer = null
            }
        }
    }

    private fun emitEvent(event: SpeechEngineEvent) {
        coroutineScope.launch {
            _events.emit(event)
        }
    }

    private fun createListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                emitEvent(SpeechEngineEvent.Listening)
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                val (msg, recoverable) = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Microphone recording error (conflict with screen recorder)." to true
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected (timeout)." to true
                    SpeechRecognizer.ERROR_NO_MATCH -> "Could not recognize word." to true
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Service busy, retrying." to true
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission not granted." to false
                    else -> "Recognition error code $error." to true
                }
                if (error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                    emitEvent(SpeechEngineEvent.NoSpeechTimeout(msg))
                } else {
                    emitEvent(SpeechEngineEvent.Error(msg, isRecoverable = recoverable))
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val topMatch = matches?.firstOrNull() ?: ""
                emitEvent(SpeechEngineEvent.FinalResult(topMatch))
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val topMatch = matches?.firstOrNull() ?: ""
                if (topMatch.isNotEmpty()) {
                    emitEvent(SpeechEngineEvent.PartialResult(topMatch))
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }
}
