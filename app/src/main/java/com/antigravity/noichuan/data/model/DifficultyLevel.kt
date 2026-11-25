package com.antigravity.noichuan.data.model

import androidx.compose.ui.graphics.Color

enum class DifficultyLevel(
    val key: String,
    val label: String,
    val labelVi: String,
    val color: Color
) {
    BEGINNER("basic", "Beginner", "Beginner", Color(0xFF29B6F6)),
    INTERMEDIATE("medium", "Intermediate", "Intermediate", Color(0xFFFFA726)),
    EXPERT("advanced", "Expert", "Expert", Color(0xFFEF5350));

    companion object {
        fun fromKey(key: String): DifficultyLevel {
            return entries.firstOrNull {
                it.key.equals(key, ignoreCase = true) ||
                        it.name.equals(key, ignoreCase = true) ||
                        it.label.equals(key, ignoreCase = true)
            } ?: when (key.lowercase()) {
                "intermediate", "med" -> INTERMEDIATE
                "expert", "hard", "adv" -> EXPERT
                else -> BEGINNER
            }
        }
    }
}
