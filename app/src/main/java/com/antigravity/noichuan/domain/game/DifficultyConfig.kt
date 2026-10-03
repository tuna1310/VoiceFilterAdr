package com.antigravity.noichuan.domain.game

import com.antigravity.noichuan.data.model.DifficultyLevel

/**
 * Gom toàn bộ cấu hình 3 độ khó (Beginner / Intermediate / Expert) vào một cấu trúc dữ liệu duy nhất theo spec A4.
 * Ảnh hưởng đến:
 * - Thời gian phản xạ cho mỗi từ (3.0s / 2.5s / 2.0s)
 * - Độ dài ký tự từ vựng được chọn
 * - Ngưỡng đối sánh âm vị học (Pronunciation Matcher Threshold)
 * - Tốc độ chạy của nhân vật và cổng (Gate Speed Multiplier)
 */
data class DifficultyConfig(
    val level: DifficultyLevel,
    val timeLimitPerWordMs: Long,
    val minWordLength: Int,
    val maxWordLength: Int,
    val phoneticMatchThreshold: Double,
    val gateSpeedMultiplier: Float
) {
    companion object {
        fun forLevel(level: DifficultyLevel): DifficultyConfig = when (level) {
            DifficultyLevel.BEGINNER -> DifficultyConfig(
                level = level,
                timeLimitPerWordMs = 3000L,
                minWordLength = 1,
                maxWordLength = 6,
                phoneticMatchThreshold = 0.65,
                gateSpeedMultiplier = 1.0f
            )
            DifficultyLevel.INTERMEDIATE -> DifficultyConfig(
                level = level,
                timeLimitPerWordMs = 2500L,
                minWordLength = 4,
                maxWordLength = 8,
                phoneticMatchThreshold = 0.80,
                gateSpeedMultiplier = 1.25f
            )
            DifficultyLevel.EXPERT -> DifficultyConfig(
                level = level,
                timeLimitPerWordMs = 2000L,
                minWordLength = 6,
                maxWordLength = 15,
                phoneticMatchThreshold = 0.90,
                gateSpeedMultiplier = 1.50f
            )
        }
    }
}
