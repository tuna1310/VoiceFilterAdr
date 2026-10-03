package com.antigravity.noichuan.domain.repository

import kotlinx.coroutines.flow.Flow

interface ProgressRepository {
    val streak: Flow<Int>
    val totalScore: Flow<Int>
    val completedWordIds: Flow<Set<String>>
    val starsBySubject: Flow<Map<String, Int>>
    suspend fun recordCompletion(wordId: String, subject: String, score: Int, stars: Int)
    suspend fun checkAndUpdateStreak()
}
