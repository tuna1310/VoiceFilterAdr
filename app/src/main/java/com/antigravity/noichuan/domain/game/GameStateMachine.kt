package com.antigravity.noichuan.domain.game

import com.antigravity.noichuan.data.model.Word
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * State Machine thuần Kotlin quản lý toàn bộ vòng đời trận đấu Voice Runner / Challenge.
 * Hoàn toàn tách biệt khỏi Android Framework và Jetpack Compose UI.
 */
class GameStateMachine(
    private val timeline: GameEventTimeline = GameEventTimeline()
) {

    private val _state = MutableStateFlow<GameState>(GameState.Idle)
    val state: StateFlow<GameState> = _state.asStateFlow()

    val eventTimeline: GameEventTimeline get() = timeline

    // Cấu hình thời gian hiệu ứng chuyển cổng (pass hoặc crash) trước khi mở từ tiếp theo
    private val gateTransitionDurationMs = 300L

    fun processAction(action: GameAction) {
        val current = _state.value
        when (action) {
            is GameAction.Start -> handleStart(action)
            is GameAction.CountdownTick -> handleCountdownTick(current, action.secondsRemaining)
            is GameAction.CountdownFinish -> handleCountdownFinish(current)
            is GameAction.Tick -> handleTick(current, action.deltaMs)
            is GameAction.WordMatched -> handleWordMatched(current, action.spokenWord, action.pointsAwarded)
            is GameAction.WordWrong -> handleWordWrong(current, action.reason)
            is GameAction.WordTimeout -> handleWordTimeout(current)
            is GameAction.Pause -> handlePause(current)
            is GameAction.Resume -> handleResume(current)
            is GameAction.Restart -> handleRestart()
            is GameAction.Finish -> handleFinish(current, action.reason)
        }
    }

    private fun handleStart(action: GameAction.Start) {
        if (action.words.isEmpty()) {
            return
        }
        timeline.clear()
        // Bắt đầu với đếm ngược 3 giây
        _state.value = GameState.Countdown(secondsRemaining = 3)
        // Lưu trữ cấu hình để khởi tạo Playing sau khi countdown kết thúc
        savedStartAction = action
    }

    private var savedStartAction: GameAction.Start? = null

    private fun handleCountdownTick(current: GameState, secondsRemaining: Int) {
        if (current is GameState.Countdown) {
            if (secondsRemaining > 0) {
                _state.value = GameState.Countdown(secondsRemaining = secondsRemaining)
            } else {
                handleCountdownFinish(current)
            }
        }
    }

    private fun handleCountdownFinish(current: GameState) {
        if (current is GameState.Countdown) {
            val startCfg = savedStartAction ?: return
            val initialPlaying = GameState.Playing(
                words = startCfg.words,
                currentWordIndex = 0,
                livesRemaining = 3,
                maxLives = 3,
                score = 0,
                combo = 0,
                maxCombo = 0,
                elapsedSeconds = 0f,
                maxDurationSeconds = startCfg.durationSeconds,
                wordElapsedMs = 0L,
                wordTimeLimitMs = startCfg.timeLimitPerWordMs,
                gateProgress = 0f,
                isGateSuccess = null,
                consecutiveCorrect = 0,
                isGateTransitioning = false
            )
            _state.value = initialPlaying
            timeline.record(
                TimelineEventType.GATE_SPAWN,
                0L,
                initialPlaying.currentWord?.word ?: ""
            )
        }
    }

    private fun handleTick(current: GameState, deltaMs: Long) {
        if (current !is GameState.Playing) return

        // 1. Cập nhật thời gian tổng trận đấu
        val newElapsedSeconds = current.elapsedSeconds + (deltaMs / 1000f)
        val currentTotalMs = (newElapsedSeconds * 1000).toLong()

        // Kiểm tra hết giờ tổng
        if (newElapsedSeconds >= current.maxDurationSeconds) {
            transitionToFinished(current, GameOverReason.TIME_EXPIRED, newElapsedSeconds)
            return
        }

        // 2. Nếu đang trong giai đoạn chuyển tiếp cổng (sau khi đúng/sai)
        if (current.isGateTransitioning) {
            val newWordElapsed = current.wordElapsedMs + deltaMs
            if (newWordElapsed >= gateTransitionDurationMs) {
                advanceToNextWord(current, newElapsedSeconds)
            } else {
                _state.value = current.copy(
                    elapsedSeconds = newElapsedSeconds,
                    wordElapsedMs = newWordElapsed,
                    gateProgress = 1.3f // Ở vị trí đã vượt qua hoặc va chạm
                )
            }
            return
        }

        // 3. Trong trạng thái chơi bình thường (cổng đang tiến tới nhân vật)
        val newWordElapsed = current.wordElapsedMs + deltaMs
        val gateAnimDurationMs = 2000L // 2 giây cổng chạy từ xa (0.0) tới nhân vật (1.0)
        val newGateProgress = (newWordElapsed.toFloat() / gateAnimDurationMs.toFloat()).coerceAtMost(1.0f)

        // Kiểm tra hết giờ từ vựng (Word Timeout)
        if (newWordElapsed >= current.wordTimeLimitMs) {
            handleWordTimeout(current)
            return
        }

        _state.value = current.copy(
            elapsedSeconds = newElapsedSeconds,
            wordElapsedMs = newWordElapsed,
            gateProgress = newGateProgress
        )
    }

    private fun handleWordMatched(current: GameState, spokenWord: String, pointsAwarded: Int) {
        if (current !is GameState.Playing) return
        if (current.isGateTransitioning) return // Chống race condition: bỏ qua kết quả rác khi đang chuyển cổng

        val totalMs = (current.elapsedSeconds * 1000).toLong()
        val newCombo = current.combo + 1
        val newMaxCombo = maxOf(current.maxCombo, newCombo)
        val bonus = if (newCombo > 1) (newCombo * 2) else 0
        val totalPoints = current.score + pointsAwarded + bonus
        val currentWordText = current.currentWord?.word ?: ""

        timeline.record(
            TimelineEventType.MATCH_SUCCESS,
            totalMs,
            "word=$currentWordText;spoken=$spokenWord;pts=${pointsAwarded + bonus}"
        )
        if (newCombo % 3 == 0) {
            timeline.record(TimelineEventType.COMBO_MILESTONE, totalMs, "combo=$newCombo")
        }

        _state.value = current.copy(
            score = totalPoints,
            combo = newCombo,
            maxCombo = newMaxCombo,
            consecutiveCorrect = current.consecutiveCorrect + 1,
            wordsMatchedCount = current.wordsMatchedCount + 1,
            isGateSuccess = true,
            isGateTransitioning = true,
            wordElapsedMs = 0L, // reset đồng hồ cho thời gian chuyển tiếp gateTransitionDurationMs
            gateProgress = 1.3f,
            bannerMessage = "🎉 Correct! +${pointsAwarded + bonus}"
        )
    }

    private fun handleWordWrong(current: GameState, reason: String) {
        if (current !is GameState.Playing) return
        if (current.isGateTransitioning) return // Chống race condition

        val totalMs = (current.elapsedSeconds * 1000).toLong()
        val newLives = current.livesRemaining - 1
        timeline.record(TimelineEventType.WORD_WRONG, totalMs, "reason=$reason")
        timeline.record(TimelineEventType.LIFE_LOST, totalMs, "livesRemaining=$newLives")

        if (newLives <= 0) {
            timeline.record(TimelineEventType.GAME_OVER, totalMs, "reason=OUT_OF_LIVES")
            transitionToFinished(current.copy(livesRemaining = 0), GameOverReason.OUT_OF_LIVES, current.elapsedSeconds)
            return
        }

        _state.value = current.copy(
            livesRemaining = newLives,
            combo = 0,
            isGateSuccess = false,
            isGateTransitioning = true,
            wordElapsedMs = 0L,
            gateProgress = 1.3f,
            bannerMessage = "💥 Crash! -1 Life"
        )
    }

    private fun handleWordTimeout(current: GameState) {
        if (current !is GameState.Playing) return
        if (current.isGateTransitioning) return // Chống race condition: bỏ qua timeout trùng lúc vừa match

        val totalMs = (current.elapsedSeconds * 1000).toLong()
        timeline.record(TimelineEventType.TIMEOUT, totalMs, "word=${current.currentWord?.word ?: ""}")
        handleWordWrong(current, "Timeout")
    }

    private fun advanceToNextWord(current: GameState.Playing, elapsedSeconds: Float) {
        val nextIndex = current.currentWordIndex + 1
        if (nextIndex < current.words.size) {
            val nextWord = current.words[nextIndex]
            val totalMs = (elapsedSeconds * 1000).toLong()
            timeline.record(TimelineEventType.GATE_SPAWN, totalMs, nextWord.word)

            _state.value = current.copy(
                currentWordIndex = nextIndex,
                wordElapsedMs = 0L,
                gateProgress = 0f,
                isGateSuccess = null,
                isGateTransitioning = false,
                bannerMessage = null,
                elapsedSeconds = elapsedSeconds
            )
        } else {
            // Đã hoàn thành toàn bộ danh sách từ
            transitionToFinished(current, GameOverReason.ALL_WORDS_COMPLETED, elapsedSeconds)
        }
    }

    private fun handlePause(current: GameState) {
        if (current is GameState.Playing) {
            val totalMs = (current.elapsedSeconds * 1000).toLong()
            timeline.record(TimelineEventType.GAME_PAUSED, totalMs)
            _state.value = GameState.Paused(savedPlayingState = current)
        }
    }

    private fun handleResume(current: GameState) {
        if (current is GameState.Paused) {
            val totalMs = (current.savedPlayingState.elapsedSeconds * 1000).toLong()
            timeline.record(TimelineEventType.GAME_RESUMED, totalMs)
            _state.value = current.savedPlayingState
        }
    }

    private fun handleRestart() {
        val startCfg = savedStartAction
        if (startCfg != null) {
            handleStart(startCfg)
        } else {
            _state.value = GameState.Idle
        }
    }

    private fun handleFinish(current: GameState, reason: GameOverReason) {
        when (current) {
            is GameState.Playing -> transitionToFinished(current, reason, current.elapsedSeconds)
            is GameState.Paused -> transitionToFinished(current.savedPlayingState, reason, current.savedPlayingState.elapsedSeconds)
            else -> _state.value = GameState.Idle
        }
    }

    private fun transitionToFinished(
        playing: GameState.Playing,
        reason: GameOverReason,
        durationSeconds: Float
    ) {
        val completedWords = if (reason == GameOverReason.ALL_WORDS_COMPLETED) {
            playing.words.size
        } else {
            playing.wordsMatchedCount
        }
        val totalWords = playing.words.size
        val accuracy = if (totalWords > 0) (completedWords.toFloat() / totalWords.toFloat()) else 0f

        val stars = when {
            accuracy >= 0.85f -> 3
            accuracy >= 0.50f -> 2
            else -> 1
        }

        val summary = GameSummary(
            totalScore = playing.score,
            wordsCompleted = completedWords,
            totalWords = totalWords,
            accuracy = accuracy,
            maxCombo = playing.maxCombo,
            durationSeconds = durationSeconds,
            stars = stars,
            reason = reason
        )

        val totalMs = (durationSeconds * 1000).toLong()
        timeline.record(TimelineEventType.GAME_OVER, totalMs, "reason=$reason;score=${playing.score}")

        _state.value = GameState.Finished(summary = summary, reason = reason)
    }
}
