package com.antigravity.noichuan.data.model

import androidx.annotation.StringRes
import com.antigravity.noichuan.R

enum class GameMode(
    val key: String,
    @StringRes val titleRes: Int,
    @StringRes val descRes: Int,
    val durationSeconds: Int,
    val passingScore: Int
) {
    EASY(
        key = "easy",
        titleRes = R.string.mode_easy_name,
        descRes = R.string.mode_easy_desc,
        durationSeconds = 15,
        passingScore = 50
    ),
    MEDIUM(
        key = "medium",
        titleRes = R.string.mode_medium_name,
        descRes = R.string.mode_medium_desc,
        durationSeconds = 10,
        passingScore = 65
    ),
    HARD(
        key = "hard",
        titleRes = R.string.mode_hard_name,
        descRes = R.string.mode_hard_desc,
        durationSeconds = 6,
        passingScore = 80
    );

    companion object {
        fun fromKey(key: String): GameMode {
            return entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: MEDIUM
        }
    }
}
