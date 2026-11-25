package com.antigravity.noichuan.domain.service

import kotlin.math.sqrt

/**
 * Giám sát khoảng lặng (Silence Watchdog) cho luồng nhận diện giọng nói.
 * 
 * Tính năng:
 * 1. Phân tích năng lượng âm thanh RMS từ buffer PCM.
 * 2. Theo dõi thời gian im lặng liên tục (không có tiếng hoặc không nhận diện được từ).
 * 3. Khi vượt ngưỡng timeout: DỪNG lại, kích hoạt callback onTimeout("No speech detected").
 * 4. TUYỆT ĐỐI KHÔNG tự động restart vô hạn; chỉ khởi động lại khi có lệnh start() tường minh.
 */
class SilenceWatchdog(
    private val silenceTimeoutMs: Long = 4000L,
    private val minSpeechRmsThreshold: Float = 250f,
    private val onTimeout: (String) -> Unit
) {

    private var lastSpeechTimestampMs: Long = 0L
    private var isRunning: Boolean = false
    private var hasTimedOut: Boolean = false

    fun start(currentTimeMs: Long = System.currentTimeMillis()) {
        isRunning = true
        hasTimedOut = false
        lastSpeechTimestampMs = currentTimeMs
    }

    fun stop() {
        isRunning = false
    }

    fun isWatching(): Boolean = isRunning && !hasTimedOut

    /**
     * Báo nhận được tín hiệu âm thanh có ích (Partial hoặc Final text từ Recognizer).
     */
    fun onSpeechRecognized(currentTimeMs: Long = System.currentTimeMillis()) {
        if (!isRunning || hasTimedOut) return
        lastSpeechTimestampMs = currentTimeMs
    }

    /**
     * Phân tích buffer PCM gửi về từ AudioRecord.
     * Trả về RMS tính được.
     */
    fun processPcmChunk(
        buffer: ShortArray,
        readSize: Int,
        currentTimeMs: Long = System.currentTimeMillis()
    ): Float {
        if (!isRunning || hasTimedOut) return 0f

        val rms = calculateRms(buffer, readSize)
        if (rms >= minSpeechRmsThreshold) {
            lastSpeechTimestampMs = currentTimeMs
        } else {
            val silenceDuration = currentTimeMs - lastSpeechTimestampMs
            if (silenceDuration >= silenceTimeoutMs) {
                hasTimedOut = true
                isRunning = false
                onTimeout("No speech detected")
            }
        }
        return rms
    }

    /**
     * Kiểm tra cưỡng bức theo đồng hồ thời gian (dành cho timer tick định kỳ).
     */
    fun checkTimeout(currentTimeMs: Long = System.currentTimeMillis()): Boolean {
        if (!isRunning || hasTimedOut) return false

        if (currentTimeMs - lastSpeechTimestampMs >= silenceTimeoutMs) {
            hasTimedOut = true
            isRunning = false
            onTimeout("No speech detected")
            return true
        }
        return false
    }

    companion object {
        fun calculateRms(buffer: ShortArray, readSize: Int): Float {
            if (readSize <= 0) return 0f
            var sumSquare = 0.0
            for (i in 0 until readSize) {
                val sample = buffer[i].toDouble()
                sumSquare += sample * sample
            }
            return sqrt(sumSquare / readSize).toFloat()
        }
    }
}
