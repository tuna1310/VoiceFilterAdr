package com.antigravity.noichuan.data.repository.mock

import com.antigravity.noichuan.domain.repository.ProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MockProgressRepository(
    initialStreak: Int = 3,
    initialTotalScore: Int = 450
) : ProgressRepository {

    private val _streak = MutableStateFlow(initialStreak)
    override val streak: Flow<Int> = _streak.asStateFlow()

    private val _totalScore = MutableStateFlow(initialTotalScore)
    override val totalScore: Flow<Int> = _totalScore.asStateFlow()

    private val _completedWords = MutableStateFlow<Set<String>>(emptySet())
    override val completedWordIds: Flow<Set<String>> = _completedWords.asStateFlow()

    private val _starsBySubject = MutableStateFlow<Map<String, Int>>(
        mapOf(
            "cinema" to 12,
            "music" to 15,
            "food" to 8,
            "travel" to 5,
            "science" to 20,
            "life" to 10
        )
    )
    override val starsBySubject: Flow<Map<String, Int>> = _starsBySubject.asStateFlow()

    override suspend fun recordCompletion(
        wordId: String,
        subject: String,
        score: Int,
        stars: Int
    ) {
        _totalScore.value += score
        val currentStars = _starsBySubject.value.toMutableMap()
        currentStars[subject] = (currentStars[subject] ?: 0) + stars
        _starsBySubject.value = currentStars

        val completed = _completedWords.value.toMutableSet()
        completed.add(wordId)
        _completedWords.value = completed
    }

    override suspend fun checkAndUpdateStreak() {
        _streak.value += 1
    }
}
