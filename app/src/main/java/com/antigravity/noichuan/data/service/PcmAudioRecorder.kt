package com.antigravity.noichuan.data.service

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.SystemClock
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Quản lý DUY NHẤT một phiên AudioRecord trên thiết bị theo kiến trúc Phương án (b).
 * 
 * - Đọc luồng âm thanh PCM 16kHz, 16-bit mono.
 * - Phát dữ liệu PCM tới PcmAudioDispatcher (để gửi cho Vosk và SilenceWatchdog).
 * - Lưu song song ra file PCM phục vụ cho tiến trình Mux hậu kỳ ghép vào MP4.
 * - Ghi nhận chính xác audioStartNanos theo Master Clock (SystemClock.elapsedRealtimeNanos).
 */
class PcmAudioRecorder(
    private val dispatcher: PcmAudioDispatcher = PcmAudioDispatcher()
) {

    companion object {
        private const val TAG = "PcmAudioRecorder"
        const val SAMPLE_RATE = 16000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    private val isRecording = AtomicBoolean(false)

    var audioStartNanos: Long = 0L
        private set

    var currentAudioOutputFile: File? = null
        private set

    @SuppressLint("MissingPermission")
    fun startRecording(outputFile: File? = null, onStarted: () -> Unit = {}, onError: (String) -> Unit = {}) {
        if (isRecording.get()) return

        try {
            val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
            val bufferSize = (minBufferSize * 2).coerceAtLeast(4096)

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                onError("Không thể khởi tạo AudioRecord (Microphone có thể đang bị ứng dụng khác chiếm giữ).")
                return
            }

            currentAudioOutputFile = outputFile
            audioRecord?.startRecording()
            isRecording.set(true)
            audioStartNanos = SystemClock.elapsedRealtimeNanos()

            recordingThread = Thread({
                readAudioLoop(bufferSize, outputFile)
            }, "PcmAudioRecordThread").apply {
                priority = Thread.MAX_PRIORITY
                start()
            }

            Log.d(TAG, "AudioRecord started at nano: $audioStartNanos")
            onStarted()

        } catch (e: Exception) {
            isRecording.set(false)
            val msg = "Lỗi khởi động PcmAudioRecorder: ${e.message}"
            Log.e(TAG, msg, e)
            onError(msg)
        }
    }

    private fun readAudioLoop(bufferSize: Int, outputFile: File?) {
        val shortBuffer = ShortArray(bufferSize / 2)
        val byteBuffer = ByteArray(bufferSize)
        val fileOutputStream = outputFile?.let {
            it.parentFile?.mkdirs()
            FileOutputStream(it)
        }

        try {
            while (isRecording.get()) {
                val record = audioRecord ?: break
                val readShorts = record.read(shortBuffer, 0, shortBuffer.size)

                if (readShorts > 0) {
                    // 1. Phân phối PCM tới các subscriber (Vosk, Watchdog)
                    dispatcher.dispatchChunk(shortBuffer, readShorts)

                    // 2. Ghi ra file nhị phân nếu có cấu hình
                    fileOutputStream?.let { fos ->
                        for (i in 0 until readShorts) {
                            val v = shortBuffer[i].toInt()
                            byteBuffer[i * 2] = (v and 0xFF).toByte()
                            byteBuffer[i * 2 + 1] = ((v shr 8) and 0xFF).toByte()
                        }
                        fos.write(byteBuffer, 0, readShorts * 2)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in AudioRecord loop", e)
        } finally {
            try {
                fileOutputStream?.flush()
                fileOutputStream?.close()
            } catch (e: Exception) {
                Log.w(TAG, "Error closing audio output stream: ${e.message}")
            }
        }
    }

    fun stopRecording() {
        if (!isRecording.compareAndSet(true, false)) return

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping audioRecord: ${e.message}")
        } finally {
            audioRecord = null
            recordingThread = null
            Log.d(TAG, "AudioRecord stopped")
        }
    }

    fun getDispatcher(): PcmAudioDispatcher = dispatcher
}
