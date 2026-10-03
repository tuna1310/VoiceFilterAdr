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
import com.antigravity.noichuan.domain.service.SpeechRecognitionState
import com.antigravity.noichuan.domain.service.SpeechRecognizerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AndroidSpeechRecognizerService(
    private val context: Context
) : SpeechRecognizerService {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val _state = MutableStateFlow<SpeechRecognitionState>(SpeechRecognitionState.Idle)
    override val state: StateFlow<SpeechRecognitionState> = _state.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null

    private fun ensureRecognizer(): Boolean {
        if (speechRecognizer != null) return true

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _state.value = SpeechRecognitionState.Error(
                "Dịch vụ nhận diện giọng nói không khả dụng trên thiết bị này.",
                isRecoverable = false
            )
            return false
        }

        speechRecognizer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
        ) {
            SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        } else {
            SpeechRecognizer.createSpeechRecognizer(context)
        }

        speechRecognizer?.setRecognitionListener(createListener())
        return speechRecognizer != null
    }

    override fun startListening(onReady: () -> Unit) {
        mainHandler.post {
            try {
                if (!ensureRecognizer()) return@post

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    )
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-US")
                    putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                }

                _state.value = SpeechRecognitionState.Listening
                speechRecognizer?.startListening(intent)
                onReady()
            } catch (e: Exception) {
                _state.value = SpeechRecognitionState.Error("Không thể kích hoạt ghi âm: ${e.message}")
            }
        }
    }

    override fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun destroy() {
        mainHandler.post {
            try {
                speechRecognizer?.destroy()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                speechRecognizer = null
                _state.value = SpeechRecognitionState.Idle
            }
        }
    }

    private fun createListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _state.value = SpeechRecognitionState.Listening
            }

            override fun onBeginningOfSpeech() {
                _state.value = SpeechRecognitionState.Listening
            }

            override fun onRmsChanged(rmsdB: Float) {
                // RMS amplitude
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                _state.value = SpeechRecognitionState.Processing
            }

            override fun onError(error: Int) {
                val errorMsg = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Lỗi thu âm từ microphone."
                    SpeechRecognizer.ERROR_CLIENT -> "Lỗi kết nối từ ứng dụng."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Chưa cấp quyền microphone."
                    SpeechRecognizer.ERROR_NETWORK,
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Lỗi mạng khi nhận diện."
                    SpeechRecognizer.ERROR_NO_MATCH -> "Chưa nghe rõ từ bạn phát âm. Hãy thử lại!"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Dịch vụ đang bận, vui lòng thử lại."
                    SpeechRecognizer.ERROR_SERVER -> "Lỗi máy chủ nhận diện."
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Hết thời gian chờ giọng nói."
                    else -> "Lỗi nhận diện giọng nói (mã: $error)."
                }
                _state.value = SpeechRecognitionState.Error(errorMsg, isRecoverable = true)
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    _state.value = SpeechRecognitionState.Success(matches)
                } else {
                    _state.value = SpeechRecognitionState.Error("Không có kết quả nhận diện.", isRecoverable = true)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }
}
