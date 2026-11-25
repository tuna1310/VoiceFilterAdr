package com.antigravity.noichuan.domain.service

import java.io.File

/**
 * Kế hoạch đồng bộ âm thanh - hình ảnh (A/V Sync Plan) cho tiến trình Mux hậu kỳ.
 */
data class AvSyncPlan(
    val offsetNanos: Long,
    val offsetMs: Long,
    val trimAudioMs: Long,
    val padSilenceMs: Long,
    val videoDurationMs: Long,
    val audioDurationMs: Long,
    val effectiveAudioDurationMs: Long,
    val finalDriftMs: Long
)

/**
 * Kết quả hoàn tất của tiến trình Muxer video & audio.
 * Cam kết an toàn: Nếu việc ghép audio thất bại, luôn trả về file video gốc không tiếng
 * và kèm thông báo rõ ràng để KHÔNG BAO GIỜ làm mất clip của người dùng.
 */
data class MuxResult(
    val finalVideoFile: File,
    val isAudioMuxed: Boolean,
    val syncPlan: AvSyncPlan?,
    val errorMessage: String? = null
)

/**
 * Bộ tính toán sai lệch thời gian (Offset & Drift Calculator) dựa trên Master Clock duy nhất
 * (SystemClock.elapsedRealtimeNanos hoặc System.nanoTime).
 */
object MuxerOffsetCalculator {

    /**
     * Tính toán kế hoạch bù trừ thời gian cho audio.
     * 
     * @param videoStartNanos Thời điểm frame đầu tiên của Video được đưa vào Surface (Master clock)
     * @param audioStartNanos Thời điểm buffer PCM đầu tiên được đọc từ AudioRecord (Cùng Master clock)
     * @param videoDurationMs Tổng thời lượng video trích xuất từ MediaRecorder (ms)
     * @param audioDurationMs Tổng thời lượng audio thực tế đã thu được (ms)
     */
    fun calculateSyncPlan(
        videoStartNanos: Long,
        audioStartNanos: Long,
        videoDurationMs: Long,
        audioDurationMs: Long
    ): AvSyncPlan {
        val offsetNanos = videoStartNanos - audioStartNanos
        val offsetMs = offsetNanos / 1_000_000L

        // Nếu offsetMs > 0: Audio thu trước khi video chạy -> cần cắt bỏ (trim) phần audio dôi dư ở đầu
        val trimAudioMs = if (offsetMs > 0) offsetMs else 0L

        // Nếu offsetMs < 0: Video chạy trước khi Audio kịp thu -> cần chèn khoảng im lặng (pad silence) vào đầu audio
        val padSilenceMs = if (offsetMs < 0) -offsetMs else 0L

        // Thời lượng audio thực tế sau khi bù trừ
        val effectiveAudioDurationMs = (audioDurationMs - trimAudioMs + padSilenceMs).coerceAtLeast(0L)

        // Độ lệch cuối clip (drift)
        val finalDriftMs = videoDurationMs - effectiveAudioDurationMs

        return AvSyncPlan(
            offsetNanos = offsetNanos,
            offsetMs = offsetMs,
            trimAudioMs = trimAudioMs,
            padSilenceMs = padSilenceMs,
            videoDurationMs = videoDurationMs,
            audioDurationMs = audioDurationMs,
            effectiveAudioDurationMs = effectiveAudioDurationMs,
            finalDriftMs = finalDriftMs
        )
    }

    /**
     * Fallback an toàn: Trả về kết quả bảo lưu video gốc không tiếng khi có lỗi Muxer.
     */
    fun createFallbackResult(
        originalVideoFile: File,
        reason: String,
        syncPlan: AvSyncPlan? = null
    ): MuxResult {
        return MuxResult(
            finalVideoFile = originalVideoFile,
            isAudioMuxed = false,
            syncPlan = syncPlan,
            errorMessage = "Audio mux error: $reason. Your video was safely saved without audio."
        )
    }
}
