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
                feedback = "Invalid target word.",
                bestMatchedCandidate = ""
            )
        }

        val validCandidates = candidates.map { normalize(it) }.filter { it.isNotEmpty() }
        if (validCandidates.isEmpty()) {
            return ChallengeScore(
                score = 0,
                stars = 1,
                feedback = "Couldn't hear your voice clearly, try again!",
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
            score >= 90 -> 5 to "Awesome! Spot-on pronunciation!"
            score >= 75 -> 4 to "Great job! Almost perfect!"
            score >= 60 -> 3 to "Close! Watch the ending sound!"
            score >= 40 -> 2 to "Keep going, listen and try again!"
            else -> 1 to "Not quite right, give it another try!"
        }
    }
}
