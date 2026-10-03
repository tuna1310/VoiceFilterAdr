package com.antigravity.noichuan.domain.service

data class ChallengeScore(
    val score: Int,
    val stars: Int,
    val feedback: String,
    val bestMatchedCandidate: String
)

interface ScoreCalculator {
    fun calculateScore(targetWord: String, candidates: List<String>): ChallengeScore
}
