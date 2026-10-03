package com.antigravity.noichuan.data.service

import com.antigravity.noichuan.domain.service.ChallengeScore
import com.antigravity.noichuan.domain.service.ScoreCalculator
import kotlin.math.max
import kotlin.math.roundToInt

class ScoreCalculatorImpl : ScoreCalculator {

    override fun calculateScore(targetWord: String, candidates: List<String>): ChallengeScore {
        val normalizedTarget = normalize(targetWord)
        if (normalizedTarget.isEmpty()) {
            return ChallengeScore(
                score = 0,
                stars = 1,
                feedback = "Từ mục tiêu không hợp lệ.",
                bestMatchedCandidate = ""
            )
        }

        val validCandidates = candidates.map { normalize(it) }.filter { it.isNotEmpty() }
        if (validCandidates.isEmpty()) {
            return ChallengeScore(
                score = 0,
                stars = 1,
                feedback = "Chưa nghe rõ giọng của bạn, thử lại nào!",
                bestMatchedCandidate = ""
            )
        }

        var bestScore = -1
        var bestCandidateRaw = ""

        for (candidate in validCandidates) {
            val dist = levenshteinDistance(normalizedTarget, candidate)
            val maxLen = max(normalizedTarget.length, candidate.length)
            val similarity = if (maxLen == 0) 1.0 else 1.0 - (dist.toDouble() / maxLen)
            val score = (similarity * 100.0).roundToInt().coerceIn(0, 100)

            if (score > bestScore) {
                bestScore = score
                bestCandidateRaw = candidate
            }
        }

        val finalScore = bestScore.coerceAtLeast(0)
        val (stars, feedback) = mapScoreToStarsAndFeedback(finalScore)

        return ChallengeScore(
            score = finalScore,
            stars = stars,
            feedback = feedback,
            bestMatchedCandidate = bestCandidateRaw
        )
    }

    fun normalize(text: String): String {
        return text
            .lowercase()
            .replace(Regex("[^a-z0-9\\s]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }

        for (i in 0..s1.length) {
            dp[i][0] = i
        }
        for (j in 0..s2.length) {
            dp[0][j] = j
        }

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,       // deletion
                    dp[i][j - 1] + 1,       // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }
        return dp[s1.length][s2.length]
    }

    private fun mapScoreToStarsAndFeedback(score: Int): Pair<Int, String> {
        return when {
            score >= 90 -> 5 to "Tuyệt vời! Phát âm chuẩn xác!"
            score >= 75 -> 4 to "Rất tốt! Gần như hoàn hảo!"
            score >= 60 -> 3 to "Gần đúng rồi! Chú ý âm đuôi nhé!"
            score >= 40 -> 2 to "Cố lên, nghe lại mẫu và thử lại nhé!"
            else -> 1 to "Chưa chuẩn lắm, hãy thử lại lần nữa!"
        }
    }
}
