package com.antigravity.noichuan.data.model

data class ChallengeResult(
    val word: Word,
    val spokenText: String,
    val score: Int,
    val stars: Int,
    val feedback: String,
    val passed: Boolean
)
