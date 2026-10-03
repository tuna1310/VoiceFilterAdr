package com.antigravity.noichuan.domain.game

import com.antigravity.noichuan.data.model.Word

/**
 * Lý do kết thúc trò chơi.
 */
enum class GameOverReason {
    OUT_OF_LIVES,
    TIME_EXPIRED,
    ALL_WORDS_COMPLETED,
    USER_QUIT
}

/**
 * Tổng kết lượt chơi Voice Runner / Challenge.
 */
data class GameSummary(
    val totalScore: Int = 0,
    val wordsCompleted: Int = 0,
    val totalWords: Int = 0,
    val accuracy: Float = 0f,
    val maxCombo: Int = 0,
    val durationSeconds: Float = 0f,
    val stars: Int = 1,
    val reason: GameOverReason = GameOverReason.ALL_WORDS_COMPLETED
)

/**
 * Trạng thái của Game State Machine.
 */
sealed interface GameState {
    /** Trạng thái chờ, chưa bắt đầu thử thách. */
    data object Idle : GameState

    /** Đếm ngược 3 - 2 - 1 trước khi bắt đầu chạy và thu âm. */
    data class Countdown(val secondsRemaining: Int) : GameState

    /** Trạng thái đang chơi chính thức. */
    data class Playing(
        val words: List<Word>,
        val currentWordIndex: Int = 0,
        val livesRemaining: Int = 3,
        val maxLives: Int = 3,
        val score: Int = 0,
        val combo: Int = 0,
        val maxCombo: Int = 0,
        val elapsedSeconds: Float = 0f,
        val maxDurationSeconds: Int = 60,
        val wordElapsedMs: Long = 0L,
        val wordTimeLimitMs: Long = 3000L,
        val gateProgress: Float = 0f,
        /** null: đang chạy tới cổng; true: khớp từ thành công; false: va chạm thất bại */
        val isGateSuccess: Boolean? = null,
        val consecutiveCorrect: Int = 0,
        val wordsMatchedCount: Int = 0,
        val isGateTransitioning: Boolean = false,
        val bannerMessage: String? = null
    ) : GameState {
        val currentWord: Word? get() = words.getOrNull(currentWordIndex)
        val nextWord: Word? get() = words.getOrNull(currentWordIndex + 1)
        val wordTimerProgress: Float get() = (wordElapsedMs.toFloat() / wordTimeLimitMs.toFloat()).coerceIn(0f, 1f)
        val overallProgress: Float get() = (elapsedSeconds / maxDurationSeconds.toFloat()).coerceIn(0f, 1f)
    }

    /** Trạng thái tạm dừng (người dùng bấm Pause hoặc app vào nền). */
    data class Paused(val savedPlayingState: Playing) : GameState

    /** Trạng thái kết thúc màn chơi. */
    data class Finished(
        val summary: GameSummary,
        val reason: GameOverReason
    ) : GameState
}

/**
 * Các hành động / sự kiện gửi vào GameStateMachine.
 */
sealed interface GameAction {
    data class Start(
        val words: List<Word>,
        val durationSeconds: Int = 60,
        val timeLimitPerWordMs: Long = 3000L
    ) : GameAction

    data class CountdownTick(val secondsRemaining: Int) : GameAction
    data object CountdownFinish : GameAction
    data class Tick(val deltaMs: Long) : GameAction
    data class WordMatched(val spokenWord: String, val pointsAwarded: Int = 10) : GameAction
    data class WordWrong(val reason: String = "Collision") : GameAction
    data object WordTimeout : GameAction
    data object Pause : GameAction
    data object Resume : GameAction
    data object Restart : GameAction
    data class Finish(val reason: GameOverReason) : GameAction
}
