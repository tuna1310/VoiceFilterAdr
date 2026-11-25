package com.antigravity.noichuan.data.service

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import com.antigravity.noichuan.domain.service.SilenceWatchdog
import com.antigravity.noichuan.domain.service.SpeechEngineEvent
import com.antigravity.noichuan.domain.service.SpeechEngineType
import com.antigravity.noichuan.domain.service.SpeechRecognizerEngine
import com.antigravity.noichuan.domain.service.VoskGrammarBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File
import java.io.FileOutputStream

/**
 * Trạng thái chuẩn bị / giải nén mô hình Vosk từ assets sang filesDir.
 */
sealed interface ModelUnpackState {
    data object Idle : ModelUnpackState
    data class Preparing(val progressPercent: Int, val currentFile: String) : ModelUnpackState
    data object Ready : ModelUnpackState
    data class Error(val message: String) : ModelUnpackState
}

/**
 * Engine nhận diện giọng nói on-device sử dụng thư viện Vosk (Alpha Cephei).
 * 
 * ĐẶC TÍNH KỸ THUẬT:
 * 1. Chạy tiến trình kiểm tra & giải nén model hoàn toàn trên luồng nền (Dispatchers.IO).
 * 2. Kiểm tra phiên bản (.model_version) để chỉ giải nén duy nhất 1 lần khi cài đặt/cập nhật.
 * 3. Báo cáo trạng thái chuẩn bị (ModelUnpackState) và tiến độ phần trăm rõ ràng.
 * 4. Đọc luồng PCM 16kHz Mono từ 1 AudioRecord duy nhất qua hàm feedPcmData().
 * 5. Hỗ trợ Grammar JSON giới hạn kèm token "[unk]" bắt buộc (qua VoskGrammarBuilder).
 * 6. Gọi trực tiếp recognizer.acceptWaveForm(buffer, readSize) với audio PCM thật.
 * 7. Tích hợp SilenceWatchdog: Ngắt nhận diện khi im lặng quá 4.0s, KHÔNG tự restart vô hạn.
 */
class VoskSpeechRecognizerEngine(
    private val context: Context,
    private val modelPath: File? = null,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) : SpeechRecognizerEngine {

    companion object {
        private const val TAG = "VoskSpeechEngine"
        const val ASSET_MODEL_DIR = "vosk-model-small-en-us"
        const val MODEL_VERSION = "0.15_small_en_v1"
        const val VERSION_FILE_NAME = ".model_version"
        const val SAMPLE_RATE = 16000.0f

        fun parseResultText(json: String): String {
            return try {
                JSONObject(json).optString("text", "").trim()
            } catch (e: Exception) {
                ""
            }
        }

        fun parsePartialText(json: String): String {
            return try {
                JSONObject(json).optString("partial", "").trim()
            } catch (e: Exception) {
                ""
            }
        }

        fun isModelVersionValid(targetDir: File, expectedVersion: String = MODEL_VERSION): Boolean {
            val verFile = File(targetDir, VERSION_FILE_NAME)
            val confFile = File(targetDir, "conf/model.conf")
            return targetDir.exists() && targetDir.isDirectory &&
                    confFile.exists() &&
                    verFile.exists() && verFile.readText().trim() == expectedVersion
        }
    }

    override val engineType: SpeechEngineType = SpeechEngineType.VOSK

    private val _events = MutableSharedFlow<SpeechEngineEvent>(replay = 1)
    override val events: SharedFlow<SpeechEngineEvent> = _events.asSharedFlow()

    private val _unpackState = MutableStateFlow<ModelUnpackState>(ModelUnpackState.Idle)
    val unpackState: StateFlow<ModelUnpackState> = _unpackState.asStateFlow()

    private var isListening: Boolean = false
    private var currentGrammarJson: String = "[\"[unk]\"]"

    private var voskModel: Model? = null
    private var recognizer: Recognizer? = null

    // Silence Watchdog: Timeout sau 4s nếu không có giọng nói
    private val silenceWatchdog = SilenceWatchdog(
        silenceTimeoutMs = 4000L,
        minSpeechRmsThreshold = 200f,
        onTimeout = { msg ->
            stopListening()
            emitEvent(SpeechEngineEvent.NoSpeechTimeout(msg))
        }
    )

    override fun initialize(onReady: () -> Unit, onError: (String) -> Unit) {
        coroutineScope.launch {
            try {
                val effectiveModelDir = withContext(Dispatchers.IO) {
                    modelPath ?: prepareModelDirectoryInBackground()
                }

                if (!effectiveModelDir.exists() || !effectiveModelDir.isDirectory) {
                    val msg = "Vosk language model is not ready at: ${effectiveModelDir.absolutePath}."
                    Log.w(TAG, msg)
                    _unpackState.value = ModelUnpackState.Error(msg)
                    emitEvent(SpeechEngineEvent.Error(msg, isRecoverable = true))
                    onError(msg)
                    return@launch
                }

                withContext(Dispatchers.IO) {
                    Log.d(TAG, "Khởi tạo Vosk Model từ ${effectiveModelDir.absolutePath}")
                    voskModel = Model(effectiveModelDir.absolutePath)
                    createRecognizerLocked()
                }

                _unpackState.value = ModelUnpackState.Ready
                emitEvent(SpeechEngineEvent.Ready)
                onReady()
            } catch (e: Throwable) {
                val msg = "Failed to initialize Vosk: ${e.message}"
                Log.e(TAG, msg, e)
                _unpackState.value = ModelUnpackState.Error(msg)
                emitEvent(SpeechEngineEvent.Error(msg, isRecoverable = true))
                onError(msg)
            }
        }
    }

    /**
     * Chạy hoàn toàn trên luồng I/O ngầm:
     * - Kiểm tra phiên bản model đã giải nén.
     * - Nếu phiên bản trùng khớp, bỏ qua việc sao chép.
     * - Nếu chưa có hoặc phiên bản cũ, giải nén từng file kèm báo cáo tiến độ.
     */
    private suspend fun prepareModelDirectoryInBackground(): File = withContext(Dispatchers.IO) {
        val destDir = File(context.filesDir, ASSET_MODEL_DIR)

        if (isModelVersionValid(destDir, MODEL_VERSION)) {
            Log.d(TAG, "Model Vosk đã sẵn sàng (version $MODEL_VERSION). Bỏ qua giải nén.")
            _unpackState.value = ModelUnpackState.Ready
            return@withContext destDir
        }

        Log.d(TAG, "Bắt đầu giải nén Model Vosk từ assets sang: ${destDir.absolutePath}")
        _unpackState.value = ModelUnpackState.Preparing(0, "Scanning files...")

        val allAssetFiles = mutableListOf<String>()
        collectAssetFiles(context.assets, ASSET_MODEL_DIR, allAssetFiles)
        val totalFiles = allAssetFiles.size.coerceAtLeast(1)

        destDir.mkdirs()
        var copiedCount = 0

        for (relPath in allAssetFiles) {
            val destFile = File(destDir, relPath.removePrefix("$ASSET_MODEL_DIR/"))
            destFile.parentFile?.mkdirs()

            context.assets.open(relPath).use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            copiedCount++
            val percent = (copiedCount * 100) / totalFiles
            _unpackState.value = ModelUnpackState.Preparing(percent, destFile.name)
        }

        // Ghi nhận phiên bản model
        File(destDir, VERSION_FILE_NAME).writeText(MODEL_VERSION)
        Log.d(TAG, "Giải nén model Vosk hoàn tất ($copiedCount files, version $MODEL_VERSION).")
        _unpackState.value = ModelUnpackState.Ready
        return@withContext destDir
    }

    private fun collectAssetFiles(assetManager: AssetManager, dir: String, result: MutableList<String>) {
        val list = assetManager.list(dir) ?: return
        for (item in list) {
            val sub = if (dir.isEmpty()) item else "$dir/$item"
            val subList = assetManager.list(sub)
            if (!subList.isNullOrEmpty()) {
                collectAssetFiles(assetManager, sub, result)
            } else {
                result.add(sub)
            }
        }
    }

    private fun createRecognizerLocked() {
        val model = voskModel ?: return
        try {
            recognizer = Recognizer(model, SAMPLE_RATE, currentGrammarJson)
            Log.d(TAG, "Vosk Recognizer khởi tạo thành công với grammar: $currentGrammarJson")
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi tạo Vosk Recognizer: ${e.message}", e)
        }
    }

    override fun setGrammar(targetWords: List<String>) {
        currentGrammarJson = VoskGrammarBuilder.buildGrammarJson(targetWords)
        Log.d(TAG, "Cập nhật Vosk grammar: $currentGrammarJson")
        
        recognizer?.let { rec ->
            try {
                rec.setGrammar(currentGrammarJson)
            } catch (e: Throwable) {
                Log.w(TAG, "setGrammar không tương thích, tái tạo recognizer: ${e.message}")
                createRecognizerLocked()
            }
        }
    }

    override fun feedPcmData(buffer: ShortArray, readSize: Int) {
        if (!isListening || readSize <= 0) return

        // 1. Giám sát khoảng lặng & tính RMS
        silenceWatchdog.processPcmChunk(buffer, readSize)

        // 2. Chuyển vào Vosk acceptWaveForm
        val rec = recognizer ?: return
        try {
            val isFinal = rec.acceptWaveForm(buffer, readSize)
            if (isFinal) {
                val text = parseResultText(rec.result)
                if (text.isNotBlank()) {
                    silenceWatchdog.onSpeechRecognized()
                    emitEvent(SpeechEngineEvent.FinalResult(text))
                }
            } else {
                val partial = parsePartialText(rec.partialResult)
                if (partial.isNotBlank()) {
                    silenceWatchdog.onSpeechRecognized()
                    emitEvent(SpeechEngineEvent.PartialResult(partial))
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Lỗi trong acceptWaveForm: ${e.message}", e)
        }
    }

    override fun startListening() {
        if (isListening) return
        isListening = true
        silenceWatchdog.start()
        emitEvent(SpeechEngineEvent.Listening)
        Log.d(TAG, "Vosk bắt đầu lắng nghe với grammar: $currentGrammarJson")
    }

    override fun stopListening() {
        if (!isListening) return
        isListening = false
        silenceWatchdog.stop()

        recognizer?.let { rec ->
            try {
                val text = parseResultText(rec.finalResult)
                if (text.isNotBlank()) {
                    emitEvent(SpeechEngineEvent.FinalResult(text))
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Lỗi khi lấy finalResult Vosk: ${e.message}")
            }
        }
        Log.d(TAG, "Vosk đã dừng lắng nghe")
    }

    override fun destroy() {
        stopListening()
        try {
            recognizer?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Lỗi đóng recognizer: ${e.message}")
        } finally {
            recognizer = null
        }
        try {
            voskModel?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Lỗi đóng model: ${e.message}")
        } finally {
            voskModel = null
        }
    }

    fun simulateRecognizedSpeech(text: String, isFinal: Boolean = true) {
        silenceWatchdog.onSpeechRecognized()
        if (isFinal) {
            emitEvent(SpeechEngineEvent.FinalResult(text))
        } else {
            emitEvent(SpeechEngineEvent.PartialResult(text))
        }
    }

    private fun emitEvent(event: SpeechEngineEvent) {
        coroutineScope.launch {
            _events.emit(event)
        }
    }
}
