package com.antigravity.noichuan.domain.service

import com.antigravity.noichuan.domain.game.GameEventTimeline
import com.antigravity.noichuan.domain.game.TimelineEventType
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Trình trộn âm thanh SFX (ding, buzz, crash) vào luồng microphone PCM thu âm từ trận đấu
 * dựa trên GameEventTimeline.
 * 
 * Toàn bộ logic là Pure Kotlin (PCM 16-bit mono 16kHz) không phụ thuộc Android framework,
 * có thể unit test độc lập 100% trên JVM.
 * 
 * Nguyên tắc an toàn: Nếu xảy ra bất kỳ ngoại lệ nào, phương thức luôn trả về file PCM gốc ban đầu,
 * đảm bảo video không bao giờ bị mất tiếng mic.
 */
class TimelineAudioMixer(
    private val sampleRate: Int = 16000
) {

    /**
     * Trích xuất các mẫu 16-bit PCM từ file WAV (bỏ qua RIFF header 44 bytes).
     */
    fun extractPcmFromWav(wavBytes: ByteArray): ShortArray {
        if (wavBytes.size <= 44) return ShortArray(0)
        val dataOffset = findDataChunkOffset(wavBytes)
        val numSamples = (wavBytes.size - dataOffset) / 2
        if (numSamples <= 0) return ShortArray(0)

        val buffer = ByteBuffer.wrap(wavBytes, dataOffset, numSamples * 2).order(ByteOrder.LITTLE_ENDIAN)
        val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            samples[i] = buffer.short
        }
        return samples
    }

    /**
     * Tìm vị trí bắt đầu của chunk "data" trong file WAV chuẩn.
     */
    private fun findDataChunkOffset(wavBytes: ByteArray): Int {
        for (i in 0 until wavBytes.size - 8) {
            if (wavBytes[i] == 'd'.code.toByte() &&
                wavBytes[i + 1] == 'a'.code.toByte() &&
                wavBytes[i + 2] == 't'.code.toByte() &&
                wavBytes[i + 3] == 'a'.code.toByte()
            ) {
                return i + 8 // 4 bytes 'data' + 4 bytes chunk length
            }
        }
        return 44 // Standard default header size
    }

    /**
     * Đọc toàn bộ file PCM thô (16-bit little endian) thành mảng Short.
     */
    fun readRawPcm(file: File): ShortArray {
        if (!file.exists() || file.length() <= 0) return ShortArray(0)
        val bytes = file.readBytes()
        val numSamples = bytes.size / 2
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            samples[i] = buffer.short
        }
        return samples
    }

    /**
     * Ghi mảng Short 16-bit PCM thô ra file.
     */
    fun writeRawPcm(samples: ShortArray, file: File) {
        file.parentFile?.mkdirs()
        val buffer = ByteBuffer.allocate(samples.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (s in samples) {
            buffer.putShort(s)
        }
        file.writeBytes(buffer.array())
    }

    /**
     * Trộn SFX vào file PCM mic theo timeline trận đấu.
     * 
     * @param micPcmFile File PCM thu âm từ mic (16kHz, 16-bit mono)
     * @param timeline Danh sách sự kiện kèm mốc thời gian timestampMs
     * @param dingWav Mảng byte của file ding.wav
     * @param buzzWav Mảng byte của file buzz.wav
     * @param crashWav Mảng byte của file crash.wav
     * @param outputFile File PCM kết quả sau khi đã trộn SFX
     * @param sfxGain Hệ số khuếch đại SFX (mặc định 0.8)
     * @return File PCM đã trộn hoặc file gốc nếu lỗi
     */
    fun mixTimeline(
        micPcmFile: File,
        timeline: GameEventTimeline,
        dingWav: ByteArray?,
        buzzWav: ByteArray?,
        crashWav: ByteArray?,
        outputFile: File,
        sfxGain: Float = 0.8f
    ): File {
        if (!micPcmFile.exists() || micPcmFile.length() <= 0) {
            return micPcmFile
        }

        val events = timeline.events
        if (events.isEmpty() || (dingWav == null && buzzWav == null && crashWav == null)) {
            return micPcmFile
        }

        return try {
            val micSamples = readRawPcm(micPcmFile)
            val dingSamples = dingWav?.let { extractPcmFromWav(it) } ?: ShortArray(0)
            val buzzSamples = buzzWav?.let { extractPcmFromWav(it) } ?: ShortArray(0)
            val crashSamples = crashWav?.let { extractPcmFromWav(it) } ?: ShortArray(0)

            val samplesPerMs = sampleRate / 1000 // 16 samples/ms at 16kHz

            // Tính tổng độ dài cần thiết nếu timeline có sự kiện vượt qua độ dài mic
            var maxRequiredSamples = micSamples.size
            for (ev in events) {
                val startIdx = (ev.timestampMs * samplesPerMs).toInt()
                val sfxLength = when (ev.eventType) {
                    TimelineEventType.MATCH_SUCCESS -> dingSamples.size
                    TimelineEventType.WORD_WRONG, TimelineEventType.TIMEOUT -> buzzSamples.size
                    TimelineEventType.GAME_OVER, TimelineEventType.LIFE_LOST -> crashSamples.size
                    else -> 0
                }
                if (startIdx + sfxLength > maxRequiredSamples) {
                    maxRequiredSamples = startIdx + sfxLength
                }
            }

            // Tạo mảng trộn và chép mic samples vào
            val mixed = IntArray(maxRequiredSamples)
            for (i in micSamples.indices) {
                mixed[i] = micSamples[i].toInt()
            }

            // Trộn từng sự kiện SFX vào mảng
            for (ev in events) {
                val sfx = when (ev.eventType) {
                    TimelineEventType.MATCH_SUCCESS -> dingSamples
                    TimelineEventType.WORD_WRONG, TimelineEventType.TIMEOUT -> buzzSamples
                    TimelineEventType.GAME_OVER, TimelineEventType.LIFE_LOST -> crashSamples
                    else -> null
                } ?: continue

                val startIdx = (ev.timestampMs * samplesPerMs).toInt()
                if (startIdx < 0) continue

                for (s in sfx.indices) {
                    val targetIdx = startIdx + s
                    if (targetIdx < mixed.size) {
                        val sfxVal = (sfx[s] * sfxGain).toInt()
                        mixed[targetIdx] += sfxVal
                    }
                }
            }

            // Chuyển đổi về Short với cơ chế clamp chống clipping vỡ âm
            val finalSamples = ShortArray(mixed.size)
            for (i in mixed.indices) {
                finalSamples[i] = mixed[i].coerceIn(-32768, 32767).toShort()
            }

            writeRawPcm(finalSamples, outputFile)
            outputFile
        } catch (e: Exception) {
            micPcmFile
        }
    }
}
